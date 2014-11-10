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
import org.jbpm.graph.node.TaskNode;
import org.jbpm.instantiation.Delegation;

public class SwimlaneDbTest extends AbstractDbTestCase {

  ProcessDefinition processDefinition;
  TaskMgmtDefinition taskMgmtDefinition;
  Swimlane buyer;
  Task laundry;
  Task dishes;

  protected void setUp() throws Exception {
    super.setUp();

    taskMgmtDefinition = new TaskMgmtDefinition();
    processDefinition = new ProcessDefinition();
    processDefinition.addDefinition(taskMgmtDefinition);
    buyer = new Swimlane("buyer");
    laundry = new Task("laundry");
    dishes = new Task("dishes");
  }

  public void testSwimlaneAddTask() {
    buyer.addTask(laundry);
    buyer.addTask(dishes);

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    assertEquals(2, buyer.getTasks().size());
    assertTrue(buyer.getTasks().contains(laundry));
    assertTrue(buyer.getTasks().contains(dishes));
  }

  public void testSwimlaneAddTaskInverseReference() {
    buyer.addTask(laundry);
    buyer.addTask(dishes);

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    assertSame(buyer, laundry.getSwimlane());
    assertSame(buyer, dishes.getSwimlane());
  }

  public void testTriangularRelation() {
    buyer.addTask(laundry);
    taskMgmtDefinition.addTask(laundry);
    taskMgmtDefinition.addSwimlane(buyer);

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    laundry = taskMgmtDefinition.getTask("laundry");
    assertEquals(1, taskMgmtDefinition.getTasks().size());
    assertEquals(1, buyer.getTasks().size());
    assertEquals("laundry", laundry.getName());
    assertSame(laundry, taskMgmtDefinition.getSwimlane("buyer").getTasks().iterator().next());
    assertSame(taskMgmtDefinition, laundry.getTaskMgmtDefinition());
    assertSame(taskMgmtDefinition.getSwimlane("buyer"), laundry.getSwimlane());
  }

  public void testSwimlaneAssignment() {
    processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='boss'>"
      + "    <assignment class='org.jbpm.TheOneAndOnly' />"
      + "  </swimlane>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    Swimlane boss = taskMgmtDefinition.getSwimlane("boss");
    assertNotNull(boss);
    Delegation bossAssignmentDelegation = boss.getAssignmentDelegation();
    assertNotNull(bossAssignmentDelegation);
    String assignmentHandlerClassName = bossAssignmentDelegation.getClassName();
    assertNotNull(assignmentHandlerClassName);
    assertEquals("org.jbpm.TheOneAndOnly", assignmentHandlerClassName);
  }

  public void testSwimlaneTaskMgmtTest() {
    processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='boss'>"
      + "    <assignment class='org.jbpm.TheOneAndOnly' />"
      + "  </swimlane>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    Swimlane boss = taskMgmtDefinition.getSwimlane("boss");
    assertNotNull(boss);
    assertSame(taskMgmtDefinition, boss.getTaskMgmtDefinition());
  }

  public void testTaskToSwimlane() {
    processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='boss'>"
      + "    <assignment class='org.jbpm.TheOneAndOnly' />"
      + "  </swimlane>"
      + "  <task-node name='work'>"
      + "    <task name='manage' swimlane='boss' />"
      + "  </task-node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();

    TaskNode work = (TaskNode) processDefinition.getNode("work");
    Task manage = work.getTask("manage");
    assertNotNull(manage);
    assertSame(taskMgmtDefinition.getTask("manage"), manage);

    assertNotNull(manage.getSwimlane());
    assertSame(taskMgmtDefinition.getSwimlane("boss"), manage.getSwimlane());
  }
}
