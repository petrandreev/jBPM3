package org.jbpm.command;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * abstract base class for "get" commands which also implements default pre-fetching.
 * 
 * Note: pre-fetching logs is not possible here, so you have to load Logs explicitly with
 * GetProcessInstanceLogCommand
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public abstract class AbstractGetObjectBaseCommand extends AbstractBaseCommand {

  static final Log log = LogFactory.getLog(AbstractGetObjectBaseCommand.class);

  private static final long serialVersionUID = 1L;

  /**
   * if true, all process variables in the context (process instance / task) are prefetched too
   */
  private boolean includeAllVariables;

  /**
   * specify the names of the variables to prefetch
   */
  private String[] variablesToInclude = new String[0];

  private transient JbpmContext jbpmContext;

  public AbstractGetObjectBaseCommand() {
  }

  public AbstractGetObjectBaseCommand(boolean includeAllVariables, boolean includeLogs) {
    this.includeAllVariables = includeAllVariables;
  }

  public AbstractGetObjectBaseCommand(String[] variablesToInclude) {
    this.variablesToInclude = variablesToInclude;
  }

  public void retrieveTaskInstanceDetails(TaskInstance ti) {
    try {
      Token token = ti.getToken();
      // process instance
      ProcessInstance processInstance = token.getProcessInstance();
      // in TaskInstances created with jbpm 3.1, this association was not present!
      ti.setProcessInstance(processInstance);
      // process definition
      Hibernate.initialize(processInstance.getProcessDefinition());
      // node
      Hibernate.initialize(token.getNode());
      // definition
      Hibernate.initialize(ti.getTask());
      // available transitions
      // Hibernate.initialize(ti.getAvailableTransitions());

      retrieveVariables(ti);
    }
    catch (RuntimeException ex) {
      log.warn("failure retrieving " + ti, ex);
    }
  }

  public ProcessInstance retrieveProcessInstance(ProcessInstance pi) {
    try {
      // process definition
      Hibernate.initialize(pi.getProcessDefinition());
      // root token
      retrieveToken(pi.getRootToken());
      // super process token
      Token superProcessToken = pi.getSuperProcessToken();
      if (superProcessToken != null) {
        Hibernate.initialize(superProcessToken);
        Hibernate.initialize(superProcessToken.getProcessInstance());
      }

      retrieveVariables(pi);
    }
    catch (RuntimeException ex) {
      log.warn("failure retrieving " + pi, ex);
    }
    return pi;
  }

  public ProcessDefinition retrieveProcessDefinition(ProcessDefinition pd) {
    try {
      // often needed to start a process:
      for (Iterator iter = pd.getStartState().getLeavingTransitions().iterator(); iter.hasNext();) {
        Transition transition = (Transition) iter.next();
        Hibernate.initialize(transition);
      }
    }
    catch (RuntimeException ex) {
      log.warn("failure retrieving " + pd, ex);
    }
    return pd;
  }

  protected void retrieveToken(Token token) {
    retrieveNode(token.getNode());
    // Hibernate.initialize(token.getAvailableTransitions());

    if( token.getChildren() != null ) { 
      for (Iterator iter = token.getChildren().values().iterator(); iter.hasNext();) {
        retrieveToken((Token) iter.next());
      }
    }
  }

  protected void retrieveNode(Node node) {
    if( node != null ) {
      Hibernate.initialize(node);
      // Hibernate.initialize(node.getLeavingTransitions());
      if (node.getSuperState() != null) retrieveNode(node.getSuperState());
    }
  }

  public void retrieveVariables(ProcessInstance pi) {
    if (includeAllVariables) {
      pi.getContextInstance().getVariables();
    }
    else {
      for (int i = 0; i < variablesToInclude.length; i++) {
        pi.getContextInstance().getVariable(variablesToInclude[i]);
      }
    }
  }

  public void retrieveVariables(TaskInstance ti) {
    if (includeAllVariables) {
      ti.getVariables();
    }
    else {
      for (int i = 0; i < variablesToInclude.length; i++) {
        ti.getVariable(variablesToInclude[i]);
      }
    }
  }

  public boolean isIncludeAllVariables() {
    return includeAllVariables;
  }

  public void setIncludeAllVariables(boolean includeAllVariables) {
    this.includeAllVariables = includeAllVariables;
  }

  public String[] getVariablesToInclude() {
    return variablesToInclude;
  }

  public void setVariablesToInclude(String[] variablesToInclude) {
    this.variablesToInclude = variablesToInclude;
  }

  public void setVariablesToInclude(String variableToInclude) {
    this.variablesToInclude = new String[] {
      variableToInclude
    };
  }

  protected JbpmContext getJbpmContext() {
    return jbpmContext;
  }

  protected void setJbpmContext(JbpmContext jbpmContext) {
    this.jbpmContext = jbpmContext;
  }

  // methods for fluent programming

  public AbstractGetObjectBaseCommand variablesToInclude(String[] variablesToInclude) {
    setVariablesToInclude(variablesToInclude);
    return this;
  }

  public AbstractGetObjectBaseCommand variablesToInclude(String variableToInclude) {
    setVariablesToInclude(variableToInclude);
    return this;
  }

  public AbstractGetObjectBaseCommand includeAllVariables(boolean includeAllVariables) {
    setIncludeAllVariables(includeAllVariables);
    return this;
  }

}