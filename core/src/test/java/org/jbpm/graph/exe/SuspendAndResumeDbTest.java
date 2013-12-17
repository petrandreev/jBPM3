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

import java.util.Iterator;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class SuspendAndResumeDbTest extends AbstractDbTestCase {

  public void testSuspend() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='suspendable process'>"
      + "  <start-state>"
      + "    <transition to='so something usefull' />"
      + "  </start-state>"
      + "  <task-node name='so something usefull'>"
      + "    <timer duedate='20 minutes' transition='timer expired' />"
      + "    <task name='be silent'>"
      + "      <assignment actor-id='manager' />"
      + "    </task>"
      + "    <transition name='timer expired' to='error state' />"
      + "  </task-node>"
      + "  <state name='error state' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("suspendable process");
    processInstance.signal();
    jbpmContext.save(processInstance);

    newTransaction();
    List tasks = jbpmContext.getTaskList("manager");
    assertNotNull(tasks);
    assertEquals(1, tasks.size());

    assertEquals(1, getNbrOfJobsAvailable());

    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    processInstance.suspend();
    jbpmContext.save(processInstance);

    newTransaction();
    tasks = jbpmContext.getTaskList("manager");
    assertNotNull(tasks);
    assertEquals(0, tasks.size());

    assertNull(jobSession.getFirstAcquirableJob(null));
  }

  public void testResume() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='suspendable process'>"
      + "  <start-state>"
      + "    <transition to='so something usefull' />"
      + "  </start-state>"
      + "  <task-node name='so something usefull'>"
      + "    <timer duedate='20 minutes' transition='timer expired' />"
      + "    <task name='completedTask'>"
      + "      <assignment actor-id='manager' />"
      + "    </task>"
      + "    <task name='notCompletedTask'>"
      + "      <assignment actor-id='manager' />"
      + "    </task>"
      + "    <transition name='timer expired' to='error state' />"
      + "  </task-node>"
      + "  <state name='error state' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("suspendable process");
    processInstance.signal();

    TaskInstance completedTask = findTask(processInstance, "completedTask");
    assertTrue(completedTask.isOpen());
    completedTask.end();
    assertFalse(completedTask.isOpen());
    jbpmContext.save(processInstance);

    newTransaction();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    completedTask = findTask(processInstance, "completedTask");
    assertFalse(completedTask.isOpen());
    processInstance.suspend();
    jbpmContext.save(processInstance);

    newTransaction();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    processInstance.resume();
    jbpmContext.save(processInstance);

    newTransaction();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    completedTask = findTask(processInstance, "completedTask");
    assertFalse(completedTask.isOpen());

    List tasks = jbpmContext.getTaskList("manager");
    assertEquals(1, tasks.size());

    assertEquals(1, getNbrOfJobsAvailable());
  }

  private TaskInstance findTask(ProcessInstance processInstance, String taskName) {
    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    for (Iterator iter = tmi.getTaskInstances().iterator(); iter.hasNext();) {
      TaskInstance taskInstance = (TaskInstance) iter.next();
      if (taskName.equals(taskInstance.getName())) return taskInstance;
    }
    return null;
  }
}
