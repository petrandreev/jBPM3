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
package org.jbpm.taskmgmt.def;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ExceptionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.node.TaskNode;

public class TaskDbTest extends AbstractDbTestCase {

  public void testTaskName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task name='wash car' />" +
      "</process-definition>"
    );

    processDefinition = saveAndReload(processDefinition);
    try
    {
      TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
      Task task = taskMgmtDefinition.getTask("wash car");
      assertNotNull(task);
      assertEquals("wash car", task.getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
  
  public void testTaskDescription() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task name='wash car' description='wash the car till the paint is all gone' />" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      Task task = processDefinition.getTaskMgmtDefinition().getTask("wash car");
      assertEquals("wash the car till the paint is all gone", task.getDescription());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskBlocking() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task name='wash car' blocking='true' />" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      Task task = processDefinition.getTaskMgmtDefinition().getTask("wash car");
      assertTrue(task.isBlocking());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskNode() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='saturday afternoon'>" +
      "    <task name='wash car' />" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      TaskNode taskNode = (TaskNode) processDefinition.getNode("saturday afternoon");
      Task task = taskNode.getTask("wash car");

      assertNotNull(task.getTaskNode());
      assertSame(taskNode, task.getTaskNode());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskMgmtDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='saturday afternoon'>" +
      "    <task name='wash car' />" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      TaskNode taskNode = (TaskNode) processDefinition.getNode("saturday afternoon");
      Task task = taskNode.getTask("wash car");

      assertNotNull(task.getTaskMgmtDefinition());
      assertSame(processDefinition.getTaskMgmtDefinition(), task.getTaskMgmtDefinition());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskSwimlane() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <swimlane name='butler' />" +
      "  <task-node name='saturday afternoon'>" +
      "    <task name='wash car' swimlane='butler' />" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
      Swimlane butler = taskMgmtDefinition.getSwimlane("butler");
      TaskNode taskNode = (TaskNode) processDefinition.getNode("saturday afternoon");
      Task task = taskNode.getTask("wash car");

      assertNotNull(task.getSwimlane());
      assertSame(butler, task.getSwimlane());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskAssignmentDelegation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='saturday afternoon'>" +
      "    <task name='wash car'>" +
      "      <assignment class='the-wash-car-assignment-handler-class-name' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      TaskNode taskNode = (TaskNode) processDefinition.getNode("saturday afternoon");
      Task task = taskNode.getTask("wash car");

      assertNotNull(task.getAssignmentDelegation());
      assertEquals("the-wash-car-assignment-handler-class-name", task.getAssignmentDelegation().getClassName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task name='wash car'>" +
      "    <event type='task-create' />" +
      "  </task>" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      Task task = processDefinition.getTaskMgmtDefinition().getTask("wash car");
      Event event = task.getEvent("task-create");
      assertNotNull(event);
      assertSame(task, event.getGraphElement());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskExceptionHandler() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task name='wash car'>" +
      "    <exception-handler exception-class='TooIntelligentException' />" +
      "  </task>" +
      "</process-definition>"
    );
    
    processDefinition = saveAndReload(processDefinition);
    try
    {
      Task task = processDefinition.getTaskMgmtDefinition().getTask("wash car");
      ExceptionHandler exceptionHandler = (ExceptionHandler) task.getExceptionHandlers().get(0);
      assertNotNull(exceptionHandler);
      assertEquals("TooIntelligentException",exceptionHandler.getExceptionClassName());
      assertSame(task, exceptionHandler.getGraphElement());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
