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

public class EndState extends Node {

  private static final long serialVersionUID = 1L;

  String endCompleteProcess;

  public EndState() {
  }

  public EndState(String name) {
    super(name);
  }

  public NodeType getNodeType() {
    return NodeType.EndState;
  }

  // event types //////////////////////////////////////////////////////////////

  private static final String[] EVENT_TYPES = {
    Event.EVENTTYPE_NODE_ENTER
  };

  /**
   * @deprecated arrays are mutable and thus vulnerable to external
   * manipulation. use {@link #getSupportedEventTypes()} instead
   */
  public static final String[] supportedEventTypes = (String[]) EVENT_TYPES.clone();

  public String[] getSupportedEventTypes() {
    return (String[]) EVENT_TYPES.clone();
  }

  // xml //////////////////////////////////////////////////////////////////////

  public void read(Element nodeElement, JpdlXmlReader jpdlXmlReader) {
    endCompleteProcess = nodeElement.attributeValue("end-complete-process");
  }

  public void execute(ExecutionContext executionContext) {
    if ("true".equalsIgnoreCase(endCompleteProcess)) {
      executionContext.getProcessInstance().end();
    }
    else {
      executionContext.getToken().end();
    }
  }

  public Transition addLeavingTransition(Transition t) {
    throw new UnsupportedOperationException("cannot add leaving transition to end state");
  }
}
