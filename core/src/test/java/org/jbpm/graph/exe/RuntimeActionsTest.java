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
package org.jbpm.graph.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;

public class RuntimeActionsTest extends AbstractJbpmTestCase {
  
  private ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
    "<process-definition>" +
    "  <start-state name='start'>" +
    "    <transition to='a' />" +
    "  </start-state>" +
    "  <state name='a'>" +
    "    <transition to='a' />" +
    "  </state>" +
    "  <action name='plusplus' class='org.jbpm.graph.exe.RuntimeActionsTest$PlusPlus' />" +
    "</process-definition>"
  );
  
  static int count = 0;

  public static class PlusPlus implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      // increase the static counter of the test class
      count++;
    }
  }
  
  public void testRuntimeAction() throws Exception {
    // start the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    // make sure node a was entered once before the runtime action was added 
    processInstance.signal();

    // no action was added on enter of node a yet...
    assertEquals(0,count);

    // add the runtime action on entrance of node a
    Action plusplusAction = processDefinition.getAction("plusplus");
    Event enterB = new Event(Event.EVENTTYPE_NODE_ENTER);
    processDefinition.getNode("a").addEvent(enterB);
    RuntimeAction runtimeAction = new RuntimeAction(enterB,plusplusAction);
    processInstance.addRuntimeAction(runtimeAction);

    // loop back to node a, firing event node-enter for the second time
    processInstance.signal();
    
    // only the second time, the counter should have been plusplussed
    assertEquals(1,count);
  }
}
