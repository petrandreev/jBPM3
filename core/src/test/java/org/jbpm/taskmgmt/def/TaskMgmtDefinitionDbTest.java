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
import org.jbpm.graph.def.ProcessDefinition;

public class TaskMgmtDefinitionDbTest extends AbstractDbTestCase {

  ProcessDefinition processDefinition;
  TaskMgmtDefinition taskMgmtDefinition;
  Swimlane buyer;
  Swimlane seller;
  Task laundry;
  Task dishes;

  protected void setUp() throws Exception {
    super.setUp();

    processDefinition = new ProcessDefinition();
    taskMgmtDefinition = new TaskMgmtDefinition();
    processDefinition.addDefinition(taskMgmtDefinition);
    buyer = new Swimlane("buyer");
    seller = new Swimlane("seller");
    laundry = new Task("laundry");
    dishes = new Task("dishes");
  }

  protected void tearDown() throws Exception {
    jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    super.tearDown();
  }

  public void testTaskMgmtDefinitionAddSwimlanes() {
    taskMgmtDefinition.addSwimlane(buyer);
    taskMgmtDefinition.addSwimlane(seller);

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    assertEquals(2, taskMgmtDefinition.getSwimlanes().size());
    assertEquals("buyer", taskMgmtDefinition.getSwimlane("buyer").getName());
    assertEquals("seller", taskMgmtDefinition.getSwimlane("seller").getName());
  }

  public void testTaskMgmtDefinitionAddSwimlaneInverseReference() {
    taskMgmtDefinition.addSwimlane(buyer);
    taskMgmtDefinition.addSwimlane(seller);

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    assertSame(taskMgmtDefinition, taskMgmtDefinition.getSwimlane("buyer").getTaskMgmtDefinition());
    assertSame(taskMgmtDefinition, taskMgmtDefinition.getSwimlane("seller").getTaskMgmtDefinition());
  }

  public void testTaskMgmtDefinitionAddTasks() {
    taskMgmtDefinition.addTask(laundry);
    taskMgmtDefinition.addTask(dishes);

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    assertEquals(2, taskMgmtDefinition.getTasks().size());
    assertEquals("laundry", taskMgmtDefinition.getTask("laundry").getName());
    assertEquals("dishes", taskMgmtDefinition.getTask("dishes").getName());
  }

  public void testTaskMgmtDefinitionAddTasksInverseReference() {
    taskMgmtDefinition.addTask(laundry);
    taskMgmtDefinition.addTask(dishes);

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    assertSame(taskMgmtDefinition, taskMgmtDefinition.getTask("laundry").getTaskMgmtDefinition());
    assertSame(taskMgmtDefinition, taskMgmtDefinition.getTask("dishes").getTaskMgmtDefinition());
  }
}
