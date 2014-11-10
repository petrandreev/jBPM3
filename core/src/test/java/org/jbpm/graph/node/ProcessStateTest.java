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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class ProcessStateTest extends AbstractJbpmTestCase {

  public void testBasicScenario() {
    ProcessDefinition superProcessDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='subprocessnode' />" +
      "  </start-state>" +
      "  <process-state name='subprocessnode'>" +
      "    <transition to='end' />" +
      "  </process-state>" +
      "  <end-state name='end' />" +
      "</process-definition>" 
    );

    ProcessDefinition subProcessDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='state' />" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end' />" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>" 
    );
    
    ProcessState processState = (ProcessState) superProcessDefinition.getNode("subprocessnode");
    processState.setSubProcessDefinition(subProcessDefinition);
    
    ProcessInstance superProcessInstance = new ProcessInstance(superProcessDefinition);
    superProcessInstance.signal();

    Token superToken = superProcessInstance.getRootToken();
    assertSame(processState, superToken.getNode());
    
    ProcessInstance subProcessInstance = superToken.getSubProcessInstance();
    assertSame(subProcessDefinition, subProcessInstance.getProcessDefinition());
    Token subToken = subProcessInstance.getRootToken();

    assertSame(subProcessDefinition.getNode("state"), subToken.getNode());

    subToken.signal();

    assertSame(subProcessDefinition.getNode("end"), subToken.getNode());    
    assertTrue(subToken.hasEnded());
    assertTrue(subProcessInstance.hasEnded());

    assertSame(superProcessDefinition.getNode("end"), superToken.getNode());    
    assertTrue(superToken.hasEnded());
    assertTrue(superProcessInstance.hasEnded());
  }

  public void testScenarioWithVariables() {
    ProcessDefinition superProcessDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='subprocessnode' />" +
      "  </start-state>" +
      "  <process-state name='subprocessnode'>" +
      "    <variable name='a' mapped-name='aa' />" +
      "    <variable name='b' mapped-name='bb' />" +
      "    <transition to='end' />" +
      "  </process-state>" +
      "  <end-state name='end' />" +
      "</process-definition>" 
    );
    superProcessDefinition.addDefinition(new ContextDefinition());

    ProcessDefinition subProcessDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='state' />" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end' />" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>" 
    );
    subProcessDefinition.addDefinition(new ContextDefinition());
    
    // bind the sub-process to the super process definition
    ProcessState processState = (ProcessState) superProcessDefinition.getNode("subprocessnode");
    processState.setSubProcessDefinition(subProcessDefinition);
    
    // create the super process definition
    ProcessInstance superProcessInstance = new ProcessInstance(superProcessDefinition);
    Token superToken = superProcessInstance.getRootToken();
    
    // set some variableInstances in the super process
    ContextInstance superContextInstance = superProcessInstance.getContextInstance();
    superContextInstance.setVariable("a", "hello");
    superContextInstance.setVariable("b", new Integer(3));
    
    // start execution of the super process
    superProcessInstance.signal();

    // check if the variableInstances have been copied properly into the sub process
    ProcessInstance subProcessInstance = superToken.getSubProcessInstance();
    ContextInstance subContextInstance = subProcessInstance.getContextInstance();

    assertEquals("hello", subContextInstance.getVariable("aa"));
    assertEquals(new Integer(3), subContextInstance.getVariable("bb"));
    // update variable aa
    subContextInstance.setVariable("aa", "new hello");

    // end the subprocess
    subProcessInstance.signal();
    
    // now check if the subprocess variableInstances have been copied into the super process
    assertEquals("new hello", superContextInstance.getVariable("a"));
    assertEquals(new Integer(3), superContextInstance.getVariable("b"));
  }
}
