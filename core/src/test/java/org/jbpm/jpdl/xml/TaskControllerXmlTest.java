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
package org.jbpm.jpdl.xml;

import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.def.Access;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.instantiation.Delegation;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskController;

public class TaskControllerXmlTest extends AbstractJbpmTestCase {

  public void testTaskControllerWithVariableAccesses() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t'>" +
      "    <task name='clean ceiling'>" +
      "      <controller>" +
      "        <variable name='a' access='read,write' mapped-name='x' />" +
      "        <variable name='b' access='read,write' mapped-name='y' />" +
      "        <variable name='c' access='read,write' />" +
      "      </controller>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>" 
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    Task task = taskNode.getTask("clean ceiling");
    TaskController taskController = task.getTaskController();
    assertNotNull(taskController);
    assertNull(taskController.getTaskControllerDelegation());
    List variableAccesses = taskController.getVariableAccesses();
    assertNotNull(variableAccesses);
    assertEquals(3, variableAccesses.size());
    VariableAccess variableAccess = (VariableAccess) variableAccesses.get(0);
    assertNotNull(variableAccesses);
    assertEquals("a", variableAccess.getVariableName());
    assertEquals(new Access("read,write"), variableAccess.getAccess());
    assertEquals("x", variableAccess.getMappedName());
    variableAccess = (VariableAccess) variableAccesses.get(2);
    assertNotNull(variableAccesses);
    assertEquals("c", variableAccess.getMappedName());
  }
  
  public void testTaskControllerWithDelegation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t'>" +
      "    <task name='clean ceiling'>" +
      "      <controller class='my-own-task-controller-handler-class'>" +
      "        --here comes the configuration of the task controller handler--" +
      "      </controller>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>" 
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    Task task = taskNode.getTask("clean ceiling");
    TaskController taskController = task.getTaskController();
    assertNull(taskController.getVariableAccesses());
    Delegation taskControllerDelegation = taskController.getTaskControllerDelegation();
    assertNotNull(taskControllerDelegation);
    assertEquals("my-own-task-controller-handler-class", taskControllerDelegation.getClassName());
  }
  
  public void testStartTaskController() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state name='t'>" +
      "    <task name='task to start this process'>" +
      "      <controller />" +
      "    </task>" +
      "  </start-state>" +
      "</process-definition>" 
    );
    Task task = processDefinition.getTaskMgmtDefinition().getStartTask();
    assertNotNull(task.getTaskController());
  }
  
}
