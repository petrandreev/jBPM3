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
import java.util.Iterator;
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
    Serializable id = session.save(taskInstance);
    newTransaction();

    taskInstance = (TaskInstance) session.load(TaskInstance.class, id);
    assertNotNull(taskInstance);
    assertEquals("do laundry", taskInstance.getName());
    assertEquals("someoneelse", taskInstance.getActorId());

    session.delete(taskInstance);
  }

  public void testTaskInstanceBasicLifeCycle() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling' />"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    Token rootToken = processInstance.getRootToken();
    List taskInstances = taskMgmtSession.findTaskInstancesByToken(rootToken.getId());
    assertEquals(1, taskInstances.size());

    TaskInstance taskInstance = (TaskInstance) taskInstances.get(0);
    assertFalse(taskInstance.hasEnded());
    assertEquals(rootToken, taskInstance.getToken());
    // do some update
    taskInstance.end();

    processInstance = saveAndReload(processInstance);
    taskInstance = taskMgmtSession.loadTaskInstance(taskInstance.getId());
    assertTrue("expected " + taskInstance + " to have ended", taskInstance.hasEnded());
  }

  public void testTaskName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling' />"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    List taskInstances = taskMgmtSession.findTaskInstancesByToken(processInstance.getRootToken()
      .getId());
    assertEquals(1, taskInstances.size());

    TaskInstance taskInstance = (TaskInstance) taskInstances.get(0);
    assertFalse(taskInstance.hasEnded());
    assertEquals("clean ceiling", taskInstance.getName());
    assertEquals("clean ceiling", taskInstance.getTask().getName());
    // do some update
    taskInstance.setName("clean ceiling thoroughly");

    processInstance = saveAndReload(processInstance);
    taskInstance = taskMgmtSession.loadTaskInstance(taskInstance.getId());
    assertEquals("clean ceiling thoroughly", taskInstance.getName());
    assertEquals("clean ceiling", taskInstance.getTask().getName());
  }

  public void testTaskComments() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling' />"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    List taskInstances = taskMgmtSession.findTaskInstancesByToken(processInstance.getRootToken()
      .getId());
    assertEquals(1, taskInstances.size());
    TaskInstance taskInstance = (TaskInstance) taskInstances.get(0);
    taskInstance.addComment("please hurry!");

    processInstance = saveAndReload(processInstance);
    taskInstance = taskMgmtSession.loadTaskInstance(taskInstance.getId());

    List comments = taskInstance.getComments();
    assertEquals(1, comments.size());

    Comment comment = (Comment) comments.get(0);
    assertEquals("please hurry!", comment.getMessage());
    assertSame(taskInstance, comment.getTaskInstance());
  }

  public void testBlockingTask() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='laundry' blocking='true' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

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

  public void testConditionalTasksOne() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='release' />"
      + "  </start-state>"
      + "  <task-node name='release'>"
      + "    <task name='updateWebsite' condition='#{user.admin}' />"
      + "    <task name='addNewsItem' />"
      + "    <task name='publishRelease' condition='#{user.releaseManager}' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("user", new User(true, false));
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    Collection taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();

    Set taskNames = new HashSet();
    for (Iterator i = taskInstances.iterator(); i.hasNext();) {
      TaskInstance taskInstance = (TaskInstance) i.next();
      taskNames.add(taskInstance.getName());
    }
    assertEquals(2, taskNames.size());
    assert taskNames.contains("updateWebsite") : taskNames;
    assert taskNames.contains("addNewsItem") : taskNames;
  }

  public void testConditionalTasksTwo() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='release' />"
      + "  </start-state>"
      + "  <task-node name='release'>"
      + "    <task name='updateWebsite' condition='#{user.admin}' />"
      + "    <task name='addNewsItem' />"
      + "    <task name='publishRelease' condition='#{user.releaseManager}' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("user", new User(false, true));
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    Collection taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
    Set taskNames = new HashSet();
    for (Iterator i = taskInstances.iterator(); i.hasNext();) {
      TaskInstance taskInstance = (TaskInstance) i.next();
      taskNames.add(taskInstance.getName());
    }

    assertEquals(2, taskNames.size());
    assert taskNames.contains("addNewsItem") : taskNames;
    assert taskNames.contains("publishRelease") : taskNames;
  }

  public static class User implements Serializable {
    boolean isAdmin;
    boolean isReleaseManager;

    private static final long serialVersionUID = 1L;

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
