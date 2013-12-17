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
import org.jbpm.graph.exe.*;
import org.jbpm.jpdl.xml.JpdlXmlReader;

public class EndState extends Node {

  private static final long serialVersionUID = 1L;
  
  String endCompleteProcess = null;

  public EndState() {
  }
  
  public static final String[] supportedEventTypes = new String[]{Event.EVENTTYPE_NODE_ENTER};
  public String[] getSupportedEventTypes() {
    return supportedEventTypes;
  }

  public EndState(String name) {
    super(name);
  }
  
  @Override
  public NodeType getNodeType()
  {
    return NodeType.EndState;
  }

  public void read(Element nodeElement, JpdlXmlReader jpdlXmlReader) {
    endCompleteProcess = nodeElement.attributeValue("end-complete-process");
  }

  public void execute(ExecutionContext executionContext) {
    if ( (endCompleteProcess!=null)
         && (endCompleteProcess.equalsIgnoreCase("true"))
       ) {
      executionContext.getProcessInstance().end();
    } else {
      executionContext.getToken().end();
    }
  }
  
  public Transition addLeavingTransition(Transition t) {
    throw new UnsupportedOperationException("can't add a leaving transition to an end-state");
  }
}
