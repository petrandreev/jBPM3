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

import java.util.Map;

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

  public String getName() {
    return name;
  }

  public NodeType getNodeType() {
    return NodeType.StartState;
  }

  // event types //////////////////////////////////////////////////////////////

  private static final String[] EVENT_TYPES = {
    Event.EVENTTYPE_NODE_LEAVE, Event.EVENTTYPE_AFTER_SIGNAL
  };

  /**
   * @deprecated arrays are mutable and thus vulnerable to external
   * manipulation; call {@link #getSupportedEventTypes()} instead
   */
  public static final String[] supportedEventTypes = (String[]) EVENT_TYPES.clone();

  public String[] getSupportedEventTypes() {
    return (String[]) EVENT_TYPES.clone();
  }

  // xml //////////////////////////////////////////////////////////////////////

  public void read(Element startStateElement, JpdlXmlReader jpdlReader) {
    // if the start-state has a task specified,
    Element startTaskElement = startStateElement.element("task");
    if (startTaskElement != null) {
      // delegate the parsing of the start-state task to the jpdlReader
      jpdlReader.readStartStateTask(startTaskElement, this);
    }
  }

  public void write(Element nodeElement) {
  }

  public void execute(ExecutionContext executionContext) {
  }

  public Transition addArrivingTransition(Transition t) {
    throw new UnsupportedOperationException("cannot add arriving transition to start state");
  }

  /**
   * @deprecated start states do not have arriving transitions
   * @throws UnsupportedOperationException to prevent invocation
   */
  public void setArrivingTransitions(Map arrivingTransitions) {
    throw new UnsupportedOperationException("cannot set arriving transitions of start state");
  }
}
