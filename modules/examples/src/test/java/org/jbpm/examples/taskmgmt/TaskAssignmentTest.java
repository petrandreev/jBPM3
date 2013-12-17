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
package org.jbpm.examples.taskmgmt;

import junit.framework.TestCase;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class TaskAssignmentTest extends TestCase 
{

  public void testTaskAssignment() 
  {
    // The process shown below is based on the hello world process.
    // The state node is replaced by a task-node.  The task-node 
    // is a node in JPDL that represents a wait state and generates 
    // task(s) to be completed before the process can continue to 
    // execute.  
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='the baby process'>" +
      "  <start-state>" +
      "    <transition name='baby cries' to='t' />" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task name='change nappy'>" +
      "      <assignment class='org.jbpm.examples.taskmgmt.NappyAssignmentHandler' />" +
      "    </task>" +
      "    <transition to='end' />" +
      "  </task-node>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    // Create an execution of the process definition.
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();

    // Let's start the process execution, leaving the start-state
    // over its default transition.
    token.signal();
    // The signal method will block until the process execution
    // enters a wait state. In this case, that is the task-node.
    assertSame(processDefinition.getNode("t"), token.getNode());

    // When execution arrived in the task-node, a task 'change nappy'
    // was created and the NappyAssignmentHandler was called to determine
    // to whom the task should be assigned. The NappyAssignmentHandler
    // returned 'papa'.

    // In a real environment, the tasks would be fetched from the
    // database with the methods in the org.jbpm.db.TaskMgmtSession.
    // Since we don't want to include the persistence complexity in
    // this example, we just take the first task-instance of this
    // process instance (we know there is only one in this test
    // scenario.
    TaskInstance taskInstance = (TaskInstance)processInstance.getTaskMgmtInstance().getTaskInstances().iterator().next();

    // Now, we check if the taskInstance was actually assigned to 'papa'.
    assertEquals("papa", taskInstance.getActorId());

    // Now suppose that 'papa' has done his duties and marks the task
    // as done.
    taskInstance.end();
    // Since this was the last (only) task to do, the completion of this
    // task triggered the continuation of the process instance execution.

    assertSame(processDefinition.getNode("end"), token.getNode());
  }

}
