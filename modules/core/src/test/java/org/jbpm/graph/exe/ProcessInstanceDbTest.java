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

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class ProcessInstanceDbTest extends AbstractDbTestCase {

  public void testProcessInstanceProcessDefinition() {
    ProcessDefinition processDefinition = new ProcessDefinition("definition");
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);

      processInstance = saveAndReload(processInstance);

      assertNotNull(processInstance);
      processDefinition = processInstance.getProcessDefinition();
      assertEquals("definition", processDefinition.getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testProcessInstanceDates() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end' />" +
      "  </start-state>" +
      "  <end-state name='end'/>" +
      "</process-definition>" );
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      
      assertNotNull(processInstance.getStart());
      assertNotNull(processInstance.getEnd());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testProcessInstanceRootToken() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);

      processInstance = saveAndReload(processInstance);
      
      assertNotNull(processInstance.getRootToken());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testProcessInstanceSuperProcessToken() 
  {
    ProcessDefinition superProcessDefinition = new ProcessDefinition("super");
    jbpmContext.deployProcessDefinition(superProcessDefinition);
    
    ProcessDefinition subProcessDefinition = new ProcessDefinition("sub");
    jbpmContext.deployProcessDefinition(subProcessDefinition);
    
    ProcessInstance superProcessInstance = new ProcessInstance(superProcessDefinition);
    ProcessInstance processInstance = new ProcessInstance(subProcessDefinition);
    try
    {
      Token superProcessToken = superProcessInstance.getRootToken();
      jbpmContext.save(superProcessInstance);
      
      processInstance.setSuperProcessToken(superProcessToken);

      processInstance = saveAndReload(processInstance);

      superProcessToken = processInstance.getSuperProcessToken();
      assertNotNull(superProcessToken);
      superProcessInstance = superProcessToken.getProcessInstance();
      assertNotNull(superProcessInstance);

      ProcessDefinition processDefinition = superProcessInstance.getProcessDefinition();
      assertEquals("super", processDefinition.getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(subProcessDefinition.getId());
      jbpmContext.getGraphSession().deleteProcessDefinition(superProcessDefinition.getId());
    }
  }
  
  public void testProcessInstanceModuleInstances()
  {
    ProcessDefinition processDefinition = new ProcessDefinition("modinst");
    jbpmContext.deployProcessDefinition(processDefinition);
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.addInstance(new ContextInstance());
    processInstance.addInstance(new TaskMgmtInstance());

    processInstance = saveAndReload(processInstance);
    try
    {
      assertNotNull(processInstance.getInstances());
      assertEquals(2, processInstance.getInstances().size());
      assertNotNull(processInstance.getContextInstance());
      assertNotNull(processInstance.getTaskMgmtInstance());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testProcessInstanceRuntimeActions()
  {
	ProcessDefinition processDefinition = new ProcessDefinition("modinst");
	jbpmContext.deployProcessDefinition(processDefinition);

	ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.addRuntimeAction(new RuntimeAction());
    processInstance.addRuntimeAction(new RuntimeAction());
    processInstance.addRuntimeAction(new RuntimeAction());
    processInstance.addRuntimeAction(new RuntimeAction());

    processInstance = saveAndReload(processInstance);
    try
    {
      assertNotNull(processInstance.getRuntimeActions());
      assertEquals(4, processInstance.getRuntimeActions().size());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
