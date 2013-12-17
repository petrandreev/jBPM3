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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class TaskInstanceDbTest extends AbstractDbTestCase {

  public void testTaskInstanceUnrelatedToAProcess() {
    TaskInstance taskInstance = new TaskInstance("do laundry", "someoneelse");
    session.save(taskInstance);
    newTransaction();

    taskInstance = (TaskInstance) session.load(TaskInstance.class, taskInstance.getId());
    assertNotNull(taskInstance);
    assertEquals("do laundry", taskInstance.getName());
    assertEquals("someoneelse", taskInstance.getActorId());

    session.delete(taskInstance);
  }

  public void testTaskInstanceBasicLifeCycle() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling' />"
        + "    <transition to='end' />"
        + "  </task-node>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);

      long tokenId = processInstance.getRootToken().getId();
      List<TaskInstance> taskInstances = taskMgmtSession.findTaskInstancesByToken(tokenId);
      assertEquals(1, taskInstances.size());

      TaskInstance taskInstance = taskInstances.get(0);
      assertFalse(taskInstance.hasEnded());
      assertEquals(tokenId, taskInstance.getToken().getId());
      // do some updates
      taskInstance.end();

      processInstance = saveAndReload(processInstance);
      taskInstance = taskMgmtSession.loadTaskInstance(taskInstance.getId());
      assertTrue(taskInstance.hasEnded());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }

  }

  public void testTaskName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling' />"
        + "    <transition to='end' />"
        + "  </task-node>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      List<TaskInstance> taskInstances = taskMgmtSession.findTaskInstancesByToken(processInstance.getRootToken()
          .getId());
      assertEquals(1, taskInstances.size());

      TaskInstance taskInstance = taskInstances.get(0);
      assertFalse(taskInstance.hasEnded());
      assertEquals("clean ceiling", taskInstance.getName());
      assertEquals("clean ceiling", taskInstance.getTask().getName());
      // do some updates
      taskInstance.setName("clean ceiling thoroughly");

      processInstance = saveAndReload(processInstance);
      taskInstance = taskMgmtSession.loadTaskInstance(taskInstance.getId());
      assertEquals("clean ceiling thoroughly", taskInstance.getName());
      assertEquals("clean ceiling", taskInstance.getTask().getName());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskComments() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling' />"
        + "    <transition to='end' />"
        + "  </task-node>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      List<TaskInstance> taskInstances = taskMgmtSession.findTaskInstancesByToken(processInstance.getRootToken()
          .getId());
      assertEquals(1, taskInstances.size());
      TaskInstance taskInstance = taskInstances.get(0);
      taskInstance.addComment("please hurry!");

      processInstance = saveAndReload(processInstance);
      taskInstance = taskMgmtSession.loadTaskInstance(taskInstance.getId());

      List<Comment> comments = taskInstance.getComments();
      assertEquals(1, comments.size());

      Comment comment = comments.get(0);
      assertEquals("please hurry!", comment.getMessage());
      assertSame(taskInstance, comment.getTaskInstance());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testBlockingTask() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='laundry' blocking='true' />"
        + "    <transition to='b' />"
        + "  </task-node>"
        + "  <state name='b' />"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      Token token = processInstance.getRootToken();
      token.signal();
      assertEquals("a", token.getNode().getName());

      processInstance = saveAndReload(processInstance);
      try {
        processInstance.signal();
        fail("expected exception");
      }
      catch (IllegalStateException e) {
        // OK
      }
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testConditionalTasksOne() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='release' />"
        + "  </start-state>"
        + "  <task-node name='release'>"
        + "    <task name='updateWebsite' condition='#{user.admin}' />"
        + "    <task name='addNewsItem' />"
        + "    <task name='publishRelease' condition='#{user.releaseManager}' />"
        + "  </task-node>"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.getContextInstance().setVariable("user", new User(true, false));
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance()
          .getTaskInstances();

      Set<String> taskNames = new HashSet<String>();
      for (TaskInstance taskInstance : taskInstances) {
        taskNames.add(taskInstance.getName());
      }
      assertEquals(2, taskNames.size());
      assert taskNames.contains("updateWebsite") : taskNames;
      assert taskNames.contains("addNewsItem") : taskNames;
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testConditionalTasksTwo() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='release' />"
        + "  </start-state>"
        + "  <task-node name='release'>"
        + "    <task name='updateWebsite' condition='#{user.admin}' />"
        + "    <task name='addNewsItem' />"
        + "    <task name='publishRelease' condition='#{user.releaseManager}' />"
        + "  </task-node>"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.getContextInstance().setVariable("user", new User(false, true));
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance()
          .getTaskInstances();
      Set<String> taskNames = new HashSet<String>();
      for (TaskInstance taskInstance : taskInstances) {
        taskNames.add(taskInstance.getName());
      }

      assertEquals(2, taskNames.size());
      assert taskNames.contains("addNewsItem") : taskNames;
      assert taskNames.contains("publishRelease") : taskNames;
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public static class User implements Serializable {
    private static final long serialVersionUID = 1L;
    boolean isAdmin;
    boolean isReleaseManager;

    public User(boolean isAdmin, boolean isReleaseManager) {
      this.isAdmin = isAdmin;
      this.isReleaseManager = isReleaseManager;
    }

    public boolean isAdmin() {
      return isAdmin;
    }

    public boolean isReleaseManager() {
      return isReleaseManager;
    }
  }

}
