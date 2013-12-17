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

import java.io.*;
import java.util.*;

import org.dom4j.*;
import org.dom4j.io.*;
import org.jbpm.JbpmException;
import org.jbpm.graph.action.ActionTypes;
import org.jbpm.graph.def.*;
import org.jbpm.graph.node.*;
import org.jbpm.jpdl.*;

/**
 * @deprecated xml generation was never finished and will be removed in the future.
 */
public class JpdlXmlWriter {

  static final String JPDL_NAMESPACE = "http://jbpm.org/3/jpdl";
  static final Namespace jbpmNamespace = new Namespace(null, JPDL_NAMESPACE);
  
  Writer writer = null;
  List problems = new ArrayList();
  boolean useNamespace = false;

  public JpdlXmlWriter( Writer writer ) {
    if (writer==null) throw new JbpmException("writer is null");
    this.writer = writer;
  }
  
  public void addProblem(String msg) {
    problems.add(msg);
  }
  public static String toString(ProcessDefinition processDefinition) {
    StringWriter stringWriter = new StringWriter();
    JpdlXmlWriter jpdlWriter = new JpdlXmlWriter(stringWriter);
    jpdlWriter.write(processDefinition);
    return stringWriter.toString();
  }
  
  public void setUseNamespace(boolean useNamespace)
  {
	  this.useNamespace = useNamespace;
  }
  
  //newElement.add( jbpmNamespace );

  public void write(ProcessDefinition processDefinition) {
    problems = new ArrayList();
    if (processDefinition==null) throw new JbpmException("processDefinition is null");
    try {
      // collect the actions of the process definition
      // we will remove each named event action and the remaining ones will be written 
      // on the process definition.
      // create a dom4j dom-tree for the process definition
      Document document = createDomTree(processDefinition);
      
      // write the dom-tree to the given writer
      OutputFormat outputFormat = new OutputFormat( "  ", true );
      // OutputFormat outputFormat = OutputFormat.createPrettyPrint();
      XMLWriter xmlWriter = new XMLWriter( writer, outputFormat );
      xmlWriter.write( document );
      xmlWriter.flush();
      writer.flush();
    } catch (IOException e) {
      addProblem("couldn't write process definition xml: "+e.getMessage());
    }
    
    if (problems.size()>0) {
      throw new JpdlException(problems);
    }
  }
  
  private Document createDomTree(ProcessDefinition processDefinition) {
    Document document = DocumentHelper.createDocument();
    Element root = null;
	
	if (useNamespace)
      root = document.addElement( "process-definition", jbpmNamespace.getURI() );
	else
	  root = document.addElement( "process-definition" 
			  );
    addAttribute( root, "name", processDefinition.getName() );

    // write the start-state
    if ( processDefinition.getStartState()!=null ) {
      writeComment(root, "START-STATE");
      writeStartNode( root, (StartState) processDefinition.getStartState() );
    }
    // write the nodeMap
    if ( ( processDefinition.getNodes()!=null )
         && ( processDefinition.getNodes().size() > 0 ) ) {
      writeComment(root, "NODES");
      writeNodes( root, processDefinition.getNodes() );
    }
    // write the process level actions
    if ( processDefinition.hasEvents() ) {
      writeComment(root, "PROCESS-EVENTS");
      writeEvents( root, processDefinition );
    }
    if( processDefinition.hasActions() ) {
      writeComment(root, "ACTIONS");
      List namedProcessActions = getNamedProcessActions(processDefinition.getActions());
      writeActions(root, namedProcessActions);
    }

    root.addText( System.getProperty("line.separator") );

    return document;
  }
  
  private List getNamedProcessActions(Map actions) {
    List namedProcessActions = new ArrayList();
    Iterator iter = actions.values().iterator();
    while (iter.hasNext()) {
      Action action = (Action) iter.next();
      if ( (action.getEvent()==null)
           && (action.getName()!=null) ) {
        namedProcessActions.add(action);
      }
    }
    return namedProcessActions;
  }

  private void writeStartNode(Element element, StartState startState ) {
    if (startState!=null) {
      writeNode( addElement( element, getTypeName(startState) ), startState );
    }
  }
  
  private void writeNodes(Element parentElement, Collection nodes) {
    Iterator iter = nodes.iterator();
    while (iter.hasNext()) {
      org.jbpm.graph.def.Node node = (org.jbpm.graph.def.Node) iter.next();
      if ( ! (node instanceof StartState) ) {
        Element nodeElement = addElement( parentElement, ProcessFactory.getTypeName(node) );
        node.write(nodeElement);
        writeNode( nodeElement, node );
      }
    }
  }
  
  private void writeNode(Element element, org.jbpm.graph.def.Node node ) {
    addAttribute( element, "name", node.getName() );
    writeTransitions(element, node);
    writeEvents(element, node);
  }

  private void writeTransitions(Element element, org.jbpm.graph.def.Node node) {
    if ( node.getLeavingTransitionsMap()!=null ) {
      Iterator iter = node.getLeavingTransitionsList().iterator();
      while (iter.hasNext()) {
        Transition transition = (Transition) iter.next();
        writeTransition( element.addElement("transition"), transition );
      }
    }
  }

  private void writeTransition(Element transitionElement, Transition transition) {
    if (transition.getTo()!=null) {
      transitionElement.addAttribute( "to", transition.getTo().getName() );
    }
    if ( transition.getName()!=null ) {
      transitionElement.addAttribute( "name", transition.getName() );
    }
    Event transitionEvent = transition.getEvent(Event.EVENTTYPE_TRANSITION);
    if ( (transitionEvent!=null)
         && (transitionEvent.hasActions()) ){
      writeActions(transitionElement, transitionEvent.getActions());
    }
  }

  private void writeEvents(Element element, GraphElement graphElement) {
    if (graphElement.hasEvents()) {
      Iterator iter = graphElement.getEvents().values().iterator();
      while (iter.hasNext()) {
        Event event = (Event) iter.next();
        writeEvent( element.addElement("event"), event );
      }
    }
  }

  private void writeEvent(Element eventElement, Event event) {
    eventElement.addAttribute("type", event.getEventType());
    if (event.hasActions()) {
      Iterator actionIter = event.getActions().iterator();
      while (actionIter.hasNext()) {
        Action action = (Action) actionIter.next();
        writeAction( eventElement, action );
      }
    }
  }

  private void writeActions(Element parentElement, Collection actions) {
    Iterator actionIter = actions.iterator();
    while (actionIter.hasNext()) {
      Action action = (Action) actionIter.next();
      writeAction( parentElement, action );
    }
  }

  private void writeAction(Element parentElement, Action action ) {
    String actionName = ActionTypes.getActionName(action.getClass());
    Element actionElement = parentElement.addElement(actionName);

    if (action.getName()!=null) {
      actionElement.addAttribute("name", action.getName());
    }

    if (!action.acceptsPropagatedEvents()) {
      actionElement.addAttribute("accept-propagated-events", "false");
    }

    action.write(actionElement);
  }

  private void writeComment(Element element, String comment ) {
    element.addText( System.getProperty("line.separator") );
    element.addComment( " " + comment + " " );
  }

  private Element addElement( Element element, String elementName ) {
    Element newElement = element.addElement( elementName );
    return newElement;
  }

  private void addAttribute( Element e, String attributeName, String value ) {
    if ( value != null ) {
      e.addAttribute( attributeName, value );
    }
  }

  private String getTypeName( Object o ) {
    return ProcessFactory.getTypeName( (org.jbpm.graph.def.Node) o );
  }
  
  // private static final Log log = LogFactory.getLog(JpdlXmlWriter.class);
}
