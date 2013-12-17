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

import org.dom4j.Element;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.xml.JpdlXmlReader;

public class StartState extends Node {
  
  private static final long serialVersionUID = 1L;
  
  public StartState() {
  }

  public StartState(String name) {
    super(name);
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public NodeType getNodeType()
  {
    return NodeType.StartState;
  }

  // event types //////////////////////////////////////////////////////////////

  public static final String[] supportedEventTypes = new String[]{
    Event.EVENTTYPE_NODE_LEAVE,
    Event.EVENTTYPE_AFTER_SIGNAL
  };
  public String[] getSupportedEventTypes() {
    return supportedEventTypes;
  }

  // xml //////////////////////////////////////////////////////////////////////

  public void read(Element startStateElement, JpdlXmlReader jpdlReader) {
    // if the start-state has a task specified,
    Element startTaskElement = startStateElement.element("task");
    if (startTaskElement!=null) {
      // delegate the parsing of the start-state task to the jpdlReader
      jpdlReader.readStartStateTask(startTaskElement, this);
    }
  }

  public void write(Element nodeElement) {
  }
  
  public void leave(ExecutionContext executionContext, Transition transition) {
    // leave this node as usual
    super.leave(executionContext, transition);
  }
  
  public void execute(ExecutionContext executionContext) {
  }
  
  public Transition addArrivingTransition(Transition t) {
    throw new UnsupportedOperationException( "illegal operation : its not possible to add a transition that is arriving in a start state" );
  }
}
