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
package org.jbpm.examples.action;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class ActionTest extends AbstractJbpmTestCase {

  // Each test will start with setting the static isExecuted 
  // member of MyActionHandler to false.
  protected void setUp() throws Exception {
  	super.setUp();
    MyActionHandler.isExecuted = false;
  }

  public void testTransitionAction() {
    // The next process is a variant of the hello world process.
    // We have added an action on the transition from state s 
    // to the end-state.  The purpose of this test is to show 
    // how easy it is to integrate java code in a jBPM process.
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='s' />" +
      "  </start-state>" +
      "  <state name='s'>" +
      "    <transition to='end'>" +
      "      <action class='org.jbpm.examples.action.MyActionHandler' />" +
      "    </transition>" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    // Let's start a new execution for the process definition.
    ProcessInstance processInstance = 
      new ProcessInstance(processDefinition);
    
    // The next signal will cause the execution to leave the start 
    // state and enter the state 's'
    processInstance.signal();

    // Here we show that MyActionHandler was not yet executed. 
    assertFalse(MyActionHandler.isExecuted);
    // ... and that the the main path of execution is positioned in 
    // the state 's'
    assertSame(processDefinition.getNode("s"), 
               processInstance.getRootToken().getNode());
    
    // The next signal will trigger the execution of the root 
    // token.  The token will take the transition with the
    // action and the action will be executed during the  
    // call to the signal method.
    processInstance.signal();
    
    // Here we can see that MyActionHandler was executed during 
    // the call to the signal method.
    assertTrue(MyActionHandler.isExecuted);
  }

  public void testNodeActions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='s' />" +
      "  </start-state>" +
      "  <state name='s'>" +
      "    <event type='node-enter'>" +
      "      <action class='org.jbpm.examples.action.MyActionHandler' />" +
      "    </event>" +
      "    <event type='node-leave'>" +
      "      <action class='org.jbpm.examples.action.MyActionHandler' />" +
      "    </event>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = 
      new ProcessInstance(processDefinition);
    
    assertFalse(MyActionHandler.isExecuted);
    // The next signal will cause the execution to leave the start 
    // state and enter the state 's'.  So the state 's' is entered
    // and hence the action is executed.
    processInstance.signal();
    assertTrue(MyActionHandler.isExecuted);
    
    // Let's reset the MyActionHandler.isExecuted 
    MyActionHandler.isExecuted = false;
  
    // The next signal will trigger execution to leave the 
    // state 's'.  So the action will be executed again.
    processInstance.signal();
    // Voila. 
    assertTrue(MyActionHandler.isExecuted);
  }
}
