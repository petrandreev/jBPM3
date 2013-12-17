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
package org.jbpm.examples.context;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class ContextTest extends AbstractJbpmTestCase {
  
  public void testContext() {
    // Also this example starts from the hello world process.
    // This time even without modification.
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='s' />" +
      "  </start-state>" +
      "  <state name='s'>" +
      "    <transition to='end' />" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance =
      new ProcessInstance(processDefinition);

    // Fetch the context instance from the process instance 
    // for working with the process variableInstances.
    ContextInstance contextInstance = 
      processInstance.getContextInstance();
    
    // Before the process has left the start-state, 
    // we are going to set some process variableInstances in the 
    // context of the process instance.
    contextInstance.setVariable("amount", new Integer(500));
    contextInstance.setVariable("reason", "i met my deadline");
    
    // From now on, these variableInstances are associated with the 
    // process instance.  The process variableInstances are now accessible 
    // by user code via the API shown here, but also in the actions 
    // and node implementations.  The process variableInstances are also  
    // stored into the database as a part of the process instance.
    
    processInstance.signal();
    
    // The variableInstances are accessible via the contextInstance. 
    
    assertEquals(new Integer(500), 
                 contextInstance.getVariable("amount"));
    assertEquals("i met my deadline", 
                 contextInstance.getVariable("reason"));
  }

}
