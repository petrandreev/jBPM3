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
package org.jbpm.jpdl.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import org.jbpm.JbpmException;
import org.jbpm.graph.action.ActionTypes;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.node.ProcessFactory;
import org.jbpm.graph.node.StartState;
import org.jbpm.jpdl.JpdlException;

/**
 * Class for writing process definitions to character streams.
 * <p>
 * Saving process definitions to XML is in no way complete, but may be useful for process
 * definitions with no custom nodes.
 * </p>
 */
public class JpdlXmlWriter {

  private static final String JPDL_NAMESPACE = "urn:jbpm.org:jpdl-3.2";

  private final Writer writer;
  private final List problems = new ArrayList();
  private boolean useNamespace;

  public JpdlXmlWriter(Writer writer) {
    if (writer == null) throw new JbpmException("writer is null");
    this.writer = writer;
  }

  public void addProblem(String msg) {
    problems.add(new Problem(Problem.LEVEL_ERROR, msg));
  }

  public static String toString(ProcessDefinition processDefinition) {
    StringWriter stringWriter = new StringWriter();
    JpdlXmlWriter jpdlWriter = new JpdlXmlWriter(stringWriter);
    jpdlWriter.write(processDefinition);
    return stringWriter.toString();
  }

  public void setUseNamespace(boolean useNamespace) {
    this.useNamespace = useNamespace;
  }

  public void write(ProcessDefinition processDefinition) {
    if (processDefinition == null) throw new JbpmException("process definition is null");

    problems.clear();
    try {
      // collect the actions of the process definition
      // we will remove each named event action and the remaining ones will be written
      // on the process definition.
      Document document = createDomTree(processDefinition);

      // write the document using the given writer
      XMLWriter xmlWriter = new XMLWriter(writer, OutputFormat.createPrettyPrint());
      xmlWriter.write(document);
      xmlWriter.flush();
    }
    catch (IOException e) {
      Problem problem = new Problem(Problem.LEVEL_ERROR, "could not write process definition", e);
      problems.add(problem);
    }

    if (problems.size() > 0) throw new JpdlException(problems);
  }

  private Document createDomTree(ProcessDefinition processDefinition) {
    Document document = DocumentHelper.createDocument();
    Element root = addElement(document, "process-definition");

    String value = processDefinition.getName();
    if (value != null) root.addAttribute("name", value);

    // write the start-state
    if (processDefinition.getStartState() != null) {
      root.addComment("START-STATE");
      writeStartNode(root, (StartState) processDefinition.getStartState());
    }
    // write the nodeMap
    if (processDefinition.getNodes() != null && processDefinition.getNodes().size() > 0) {
      root.addComment("NODES");
      writeNodes(root, processDefinition.getNodes());
    }
    // write the process level actions
    if (processDefinition.hasEvents()) {
      root.addComment("PROCESS-EVENTS");
      writeEvents(root, processDefinition);
    }
    if (processDefinition.hasActions()) {
      root.addComment("ACTIONS");
      List namedProcessActions = getNamedProcessActions(processDefinition.getActions());
      writeActions(root, namedProcessActions);
    }

    return document;
  }

  private List getNamedProcessActions(Map actions) {
    List namedProcessActions = new ArrayList();
    for (Iterator iter = actions.values().iterator(); iter.hasNext();) {
      Action action = (Action) iter.next();
      if (action.getEvent() == null && action.getName() != null) {
        namedProcessActions.add(action);
      }
    }
    return namedProcessActions;
  }

  private void writeStartNode(Element element, StartState startState) {
    if (startState != null) {
      writeNode(addElement(element, getTypeName(startState)), startState);
    }
  }

  private void writeNodes(Element parentElement, Collection nodes) {
    for (Iterator iter = nodes.iterator(); iter.hasNext();) {
      org.jbpm.graph.def.Node node = (org.jbpm.graph.def.Node) iter.next();
      if (!(node instanceof StartState)) {
        Element nodeElement = addElement(parentElement, ProcessFactory.getTypeName(node));
        node.write(nodeElement);
        writeNode(nodeElement, node);
      }
    }
  }

  private void writeNode(Element element, org.jbpm.graph.def.Node node) {
    String value = node.getName();
    if (value != null) element.addAttribute("name", value);
    writeTransitions(element, node);
    writeEvents(element, node);
  }

  private void writeTransitions(Element element, org.jbpm.graph.def.Node node) {
    if (node.getLeavingTransitionsMap() != null) {
      for (Iterator iter = node.getLeavingTransitionsList().iterator(); iter.hasNext();) {
        Transition transition = (Transition) iter.next();
        writeTransition(element.addElement("transition"), transition);
      }
    }
  }

  private void writeTransition(Element transitionElement, Transition transition) {
    if (transition.getTo() != null) {
      transitionElement.addAttribute("to", transition.getTo().getName());
    }
    if (transition.getName() != null) {
      transitionElement.addAttribute("name", transition.getName());
    }
    Event transitionEvent = transition.getEvent(Event.EVENTTYPE_TRANSITION);
    if (transitionEvent != null && transitionEvent.hasActions()) {
      writeActions(transitionElement, transitionEvent.getActions());
    }
  }

  private void writeEvents(Element element, GraphElement graphElement) {
    if (graphElement.hasEvents()) {
      for (Iterator iter = graphElement.getEvents().values().iterator(); iter.hasNext();) {
        Event event = (Event) iter.next();
        writeEvent(element.addElement("event"), event);
      }
    }
  }

  private void writeEvent(Element eventElement, Event event) {
    eventElement.addAttribute("type", event.getEventType());
    if (event.hasActions()) {
      for (Iterator actionIter = event.getActions().iterator(); actionIter.hasNext();) {
        Action action = (Action) actionIter.next();
        writeAction(eventElement, action);
      }
    }
  }

  private void writeActions(Element parentElement, Collection actions) {
    for (Iterator actionIter = actions.iterator(); actionIter.hasNext();) {
      Action action = (Action) actionIter.next();
      writeAction(parentElement, action);
    }
  }

  private void writeAction(Element parentElement, Action action) {
    String actionName = ActionTypes.getActionName(action.getClass());
    Element actionElement = parentElement.addElement(actionName);

    if (action.getName() != null) {
      actionElement.addAttribute("name", action.getName());
    }

    if (!action.acceptsPropagatedEvents()) {
      actionElement.addAttribute("accept-propagated-events", "false");
    }

    action.write(actionElement);
  }

  private Element addElement(Branch parent, String elementName) {
    return useNamespace ? parent.addElement(elementName, JPDL_NAMESPACE)
      : parent.addElement(elementName);
  }

  private String getTypeName(Object o) {
    return ProcessFactory.getTypeName((org.jbpm.graph.def.Node) o);
  }
}
