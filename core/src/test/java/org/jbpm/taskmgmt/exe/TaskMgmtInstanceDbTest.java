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
package org.jbpm.taskmgmt.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

public class TaskMgmtInstanceDbTest extends AbstractDbTestCase {

  Task laundry;
  Task dishes;

  ProcessInstance processInstance;
  TaskMgmtInstance taskMgmtInstance;

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    TaskMgmtDefinition taskMgmtDefinition = new TaskMgmtDefinition();
    processDefinition.addDefinition(taskMgmtDefinition);
    Swimlane buyer = new Swimlane("buyer");
    taskMgmtDefinition.addSwimlane(buyer);
    Swimlane seller = new Swimlane("seller");
    taskMgmtDefinition.addSwimlane(seller);
    laundry = new Task("laundry");
    taskMgmtDefinition.addTask(laundry);
    dishes = new Task("dishes");
    taskMgmtDefinition.addTask(dishes);

    deployProcessDefinition(processDefinition);

    processInstance = new ProcessInstance(processDefinition);
    processInstance = saveAndReload(processInstance);

    processDefinition = processInstance.getProcessDefinition();
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
    buyer = taskMgmtDefinition.getSwimlane("buyer");
    seller = taskMgmtDefinition.getSwimlane("seller");
    laundry = taskMgmtDefinition.getTask("laundry");
    dishes = taskMgmtDefinition.getTask("dishes");
    taskMgmtInstance = processInstance.getTaskMgmtInstance();
  }

  public void testTaskMgmtInstanceTaskInstances() {
    taskMgmtInstance.createTaskInstance(laundry, processInstance.getRootToken());
    taskMgmtInstance.createTaskInstance(dishes, processInstance.getRootToken());

    processInstance = saveAndReload(processInstance);
    taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals(2, taskMgmtInstance.getTaskInstances().size());
  }

  public void testTaskMgmtInstanceSwimlaneInstances() {
    SwimlaneInstance swimlaneInstance = taskMgmtInstance.createSwimlaneInstance("buyer");
    swimlaneInstance.setActorId("john doe");

    swimlaneInstance = taskMgmtInstance.createSwimlaneInstance("seller");
    swimlaneInstance.setActorId("joe smoe");

    processInstance = saveAndReload(processInstance);

    taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals(2, taskMgmtInstance.getSwimlaneInstances().size());
  }

  public static class MultipleAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext)
      throws Exception {
      assignable.setPooledActors(new String[] { "me", "you", "them" });
    }
  }

}
