/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.graph.node;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.log.ProcessStateLog;
import org.jbpm.job.SignalTokenJob;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.msg.MessageService;
import org.jbpm.util.Clock;

public class ProcessState extends Node {

  private static final long serialVersionUID = 1L;

  private static SubProcessResolver defaultSubProcessResolver;

  /** @deprecated set configuration entry <code>jbpm.sub.process.resolver</code> instead */
  public static void setDefaultSubProcessResolver(SubProcessResolver subProcessResolver) {
    defaultSubProcessResolver = subProcessResolver;
  }

  public static SubProcessResolver getSubProcessResolver() {
    return defaultSubProcessResolver != null ? defaultSubProcessResolver
      : (SubProcessResolver) Configs.getObject("jbpm.sub.process.resolver");
  }

  protected ProcessDefinition subProcessDefinition;
  protected Set variableAccesses;
  protected String subProcessName;

  public ProcessState() {
  }

  public ProcessState(String name) {
    super(name);
  }

  // event types //////////////////////////////////////////////////////////////

  private static final String[] EVENT_TYPES = {
    Event.EVENTTYPE_SUBPROCESS_CREATED,
    Event.EVENTTYPE_SUBPROCESS_END,
    Event.EVENTTYPE_NODE_ENTER,
    Event.EVENTTYPE_NODE_LEAVE,
    Event.EVENTTYPE_BEFORE_SIGNAL,
    Event.EVENTTYPE_AFTER_SIGNAL
  };

  /**
   * @deprecated arrays are mutable and thus vulnerable to external manipulation. use
   * {@link #getSupportedEventTypes()} instead
   */
  public static final String[] supportedEventTypes = (String[]) EVENT_TYPES.clone();

  public String[] getSupportedEventTypes() {
    return (String[]) EVENT_TYPES.clone();
  }

  // xml //////////////////////////////////////////////////////////////////////

  public void read(Element processStateElement, JpdlXmlReader jpdlReader) {
    Element subProcessElement = processStateElement.element("sub-process");
    if (subProcessElement != null) {
      String binding = subProcessElement.attributeValue("binding");
      if ("late".equalsIgnoreCase(binding)) {
        subProcessName = subProcessElement.attributeValue("name");
        if (log.isDebugEnabled()) {
          log.debug(this + " will be late bound to process definition: " + subProcessName);
        }
      }
      else {
        subProcessDefinition = resolveSubProcess(subProcessElement, jpdlReader);
      }
    }

    variableAccesses = new HashSet(jpdlReader.readVariableAccesses(processStateElement));
  }

  private ProcessDefinition resolveSubProcess(Element subProcessElement,
    JpdlXmlReader jpdlReader) {
    SubProcessResolver subProcessResolver = getSubProcessResolver();
    try {
      ProcessDefinition subProcess = subProcessResolver.findSubProcess(subProcessElement);
      if (subProcess != null) {
        if (log.isDebugEnabled()) log.debug("bound " + this + " to " + subProcess);
        return subProcess;
      }
    }
    catch (JpdlException e) {
      jpdlReader.addError(e.getMessage());
    }

    // check whether this is a recursive process invocation
    String subProcessName = subProcessElement.attributeValue("name");
    if (subProcessName != null && subProcessName.equals(processDefinition.getName())) {
      if (log.isDebugEnabled()) {
        log.debug("bound " + this + " to its own " + processDefinition);
      }
      return processDefinition;
    }
    return null;
  }

  public void execute(ExecutionContext executionContext) {
    Token superProcessToken = executionContext.getToken();

    ProcessDefinition usedSubProcessDefinition = subProcessDefinition;
    // if this process has late binding
    if (subProcessDefinition == null && subProcessName != null) {
      Element subProcessElement = new DefaultElement("sub-process");
      subProcessElement.addAttribute("name", (String) JbpmExpressionEvaluator
        .evaluate(subProcessName, executionContext, String.class));

      SubProcessResolver subProcessResolver = getSubProcessResolver();
      usedSubProcessDefinition = subProcessResolver.findSubProcess(subProcessElement);
    }

    // create the subprocess
    ProcessInstance subProcessInstance = superProcessToken
      .createSubProcessInstance(usedSubProcessDefinition);

    // fire the subprocess created event
    fireEvent(Event.EVENTTYPE_SUBPROCESS_CREATED, executionContext);

    // feed the readable variableInstances
    if (variableAccesses != null && !variableAccesses.isEmpty()) {
      ContextInstance superContextInstance = executionContext.getContextInstance();
      ContextInstance subContextInstance = subProcessInstance.getContextInstance();
      subContextInstance.setTransientVariables(superContextInstance.getTransientVariables());

      // loop over all the variable accesses
      for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
        VariableAccess variableAccess = (VariableAccess) iter.next();
        // if this variable access is readable
        if (variableAccess.isReadable()) {
          // the variable is copied from the super process variable name
          // to the sub process mapped name
          String variableName = variableAccess.getVariableName();
          Object value = superContextInstance.getVariable(variableName, superProcessToken);
          if (value != null) {
            String mappedName = variableAccess.getMappedName();
            if (log.isDebugEnabled()) {
              log.debug(superProcessToken + " reads '" + variableName + "' into '" + mappedName
                + '\'');
            }
            subContextInstance.setVariable(mappedName, value);
          }
        }
      }
    }

    // send the signal to start the subprocess
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    MessageService messageService;
    if (jbpmContext != null && Configs.getBoolean("jbpm.sub.process.async")
      && (messageService = jbpmContext.getServices().getMessageService()) != null) {
      // signal sub-process token asynchronously to clearly denote transactional boundaries
      // https://jira.jboss.org/browse/JBPM-2948
      SignalTokenJob job = new SignalTokenJob(subProcessInstance.getRootToken());
      job.setDueDate(new Date());
      messageService.send(job);
    }
    else {
      // message service unavailable, signal sub-process synchronously
      subProcessInstance.signal();
    }
  }

  public void leave(ExecutionContext executionContext, Transition transition) {
    ProcessInstance subProcessInstance = executionContext.getSubProcessInstance();
    Token superProcessToken = subProcessInstance.getSuperProcessToken();

    // feed the readable variableInstances
    if (variableAccesses != null && !variableAccesses.isEmpty()) {
      ContextInstance superContextInstance = executionContext.getContextInstance();
      ContextInstance subContextInstance = subProcessInstance.getContextInstance();

      // loop over all the variable accesses
      for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
        VariableAccess variableAccess = (VariableAccess) iter.next();
        // if this variable access is writable
        if (variableAccess.isWritable()) {
          // the variable is copied from the sub process mapped name
          // to the super process variable name
          String mappedName = variableAccess.getMappedName();
          Object value = subContextInstance.getVariable(mappedName);
          if (value != null) {
            String variableName = variableAccess.getVariableName();
            if (log.isDebugEnabled()) {
              log.debug(superProcessToken + " writes '" + variableName + "' from '"
                + mappedName + '\'');
            }
            superContextInstance.setVariable(variableName, value, superProcessToken);
          }
        }
      }
    }

    // fire the subprocess ended event
    fireEvent(Event.EVENTTYPE_SUBPROCESS_END, executionContext);

    // remove the subprocess reference
    superProcessToken.setSubProcessInstance(null);

    // override the normal log generation in super.leave() by creating the log here
    // and replacing addNodeLog() with an empty version
    superProcessToken.addLog(new ProcessStateLog(this, superProcessToken.getNodeEnter(),
      Clock.getCurrentTime(), subProcessInstance));

    // call the subProcessEndAction
    super.leave(executionContext, getDefaultLeavingTransition());
  }

  protected void addNodeLog(Token token) {
    // override the normal log generation in super.leave() by creating the log in this.leave()
    // and replacing this method with an empty version
  }

  public ProcessDefinition getSubProcessDefinition() {
    return subProcessDefinition;
  }

  public void setSubProcessDefinition(ProcessDefinition subProcessDefinition) {
    this.subProcessDefinition = subProcessDefinition;
  }

  private static final Log log = LogFactory.getLog(ProcessState.class);
}
