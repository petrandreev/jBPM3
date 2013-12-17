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
package org.jbpm.taskmgmt.log;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class TaskLogDbTest extends AbstractDbTestCase {

  TaskInstance taskInstance;

  protected void setUp() throws Exception {
    super.setUp();
    taskInstance = new TaskInstance();
    session.save(taskInstance);
  }

  protected void tearDown() throws Exception {
    session.delete(taskInstance);
    super.tearDown();
  }

  public void testTaskCreateLog() {
    TaskCreateLog taskLog = new TaskCreateLog(taskInstance, "someone else");
    session.save(taskLog);

    newTransaction();
    taskLog = (TaskCreateLog) session.load(TaskCreateLog.class, new Long(taskLog.getId()));
    assertEquals(taskInstance.getId(), (taskInstance = taskLog.getTaskInstance()).getId());
    assertEquals("someone else", taskLog.getTaskActorId());

    session.delete(taskLog);
  }

  public void testTaskAssignLog() {
    TaskAssignLog taskLog = new TaskAssignLog(taskInstance, "me", "toyou");
    session.save(taskLog);

    newTransaction();
    taskLog = (TaskAssignLog) session.load(TaskAssignLog.class, new Long(taskLog.getId()));
    assertEquals(taskInstance.getId(), (taskInstance = taskLog.getTaskInstance()).getId());
    assertEquals("me", taskLog.getTaskOldActorId());
    assertEquals("toyou", taskLog.getTaskNewActorId());

    session.delete(taskLog);
  }

  public void testTaskEndLog() {
    TaskEndLog taskLog = new TaskEndLog(taskInstance);
    session.save(taskLog);

    newTransaction();
    taskLog = (TaskEndLog) session.load(TaskEndLog.class, new Long(taskLog.getId()));
    assertEquals(taskInstance.getId(), (taskInstance = taskLog.getTaskInstance()).getId());

    session.delete(taskLog);
  }
}
