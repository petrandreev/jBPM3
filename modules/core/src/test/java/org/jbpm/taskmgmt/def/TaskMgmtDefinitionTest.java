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

import org.jbpm.AbstractJbpmTestCase;

public class TaskMgmtDefinitionTest extends AbstractJbpmTestCase {
  
  TaskMgmtDefinition taskMgmtDefinition = new TaskMgmtDefinition();
  Swimlane buyer = new Swimlane("buyer");
  Swimlane seller = new Swimlane("seller");
  Task laudry = new Task("laundry");
  Task dishes = new Task("dishes");
  
  public void testTaskMgmtDefinitionAddSwimlanes() {
    taskMgmtDefinition.addSwimlane(buyer);
    taskMgmtDefinition.addSwimlane(seller);
    assertEquals(2, taskMgmtDefinition.getSwimlanes().size());
    assertTrue(taskMgmtDefinition.getSwimlanes().containsValue(buyer));
    assertTrue(taskMgmtDefinition.getSwimlanes().containsValue(seller));
  }

  public void testTaskMgmtDefinitionAddSwimlaneInverseReference() {
    taskMgmtDefinition.addSwimlane(buyer);
    taskMgmtDefinition.addSwimlane(seller);
    assertSame(taskMgmtDefinition, buyer.getTaskMgmtDefinition());
    assertSame(taskMgmtDefinition, seller.getTaskMgmtDefinition());
  }

  public void testTaskMgmtDefinitionAddTasks() {
    taskMgmtDefinition.addTask(laudry);
    taskMgmtDefinition.addTask(dishes);
    assertEquals(2, taskMgmtDefinition.getTasks().size());
    assertTrue(taskMgmtDefinition.getTasks().containsValue(laudry));
    assertTrue(taskMgmtDefinition.getTasks().containsValue(dishes));
  }
  
  public void testTaskMgmtDefinitionAddTasksInverseReference() {
    taskMgmtDefinition.addTask(laudry);
    taskMgmtDefinition.addTask(dishes);
    assertSame(taskMgmtDefinition, laudry.getTaskMgmtDefinition());
    assertSame(taskMgmtDefinition, dishes.getTaskMgmtDefinition());
  }
}
