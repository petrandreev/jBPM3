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
package org.jbpm.jbpm3171;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Test hashCode implementation of ProcessInstance and - as a result -
 * correct variable handling
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-3171">JBPM-3171</a>
 * @author Martin Weiler
 */
public class JBPM3171Test extends AbstractJbpmTestCase {

  ProcessDefinition processDefinition;

  protected void setUp() throws Exception {
    super.setUp();
    processDefinition = ProcessDefinition.createNewProcessDefinition();    
  }

  // test normal variable handling
  public void testVariableWithoutKey() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("red", new String("hat"));
    assertEquals("hat", contextInstance.getVariable("red"));
  }

  // test variable handling when process instance has been created with a key
  public void testVariableWithKey() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition, null, "key_at_instance_creation");
    ContextInstance contextInstance = processInstance.getContextInstance();      
    contextInstance.setVariable("j", new String("boss"));
    assertEquals("boss", contextInstance.getVariable("j"));
  }   

  // test variable handling when setKey is called on the process instance after creation  
  public void testVariableAfterSettingKey() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();      
    contextInstance.setVariable("hiber", new String("nate"));
    contextInstance.getProcessInstance().setKey("key_set_on_existing_instance");
    assertEquals("nate", contextInstance.getVariable("hiber"));
  } 

  // test if hashCode changes after setting a key on the process instance
  public void testHashCodeAfterSettingKey() {  
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    int hashCode = processInstance.hashCode();
    processInstance.setKey("key_set_on_existing_instance");
    assertEquals(hashCode, processInstance.hashCode());
  }
  
  // basic ProcessInstance.hashCode test
  public void testProcessInstanceHashCode() {
    ProcessInstance processInstance1 = new ProcessInstance(processDefinition);
    ProcessInstance processInstance2 = new ProcessInstance(processDefinition);
    assertTrue(processInstance1.hashCode()!=processInstance2.hashCode());      
  }
}
