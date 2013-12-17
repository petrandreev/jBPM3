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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class CommentDbTest extends AbstractDbTestCase {

  public void testComments() {
    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    jbpmContext.setActorId("miketyson");
    try {
      Token token = processInstance.getRootToken();
      token.addComment("first");
      token.addComment("second");
      token.addComment("third");
    }
    finally {
      jbpmContext.setActorId(null);
    }

    processInstance = saveAndReload(processInstance);
    Token token = processInstance.getRootToken();
    List comments = token.getComments();

    assertNotNull(comments);
    assertEquals(3, comments.size());

    assertEquals("miketyson", ((Comment) comments.get(0)).getActorId());
    assertNotNull(((Comment) comments.get(0)).getTime());
    assertEquals("first", ((Comment) comments.get(0)).getMessage());

    assertEquals("miketyson", ((Comment) comments.get(1)).getActorId());
    assertNotNull(((Comment) comments.get(1)).getTime());
    assertEquals("second", ((Comment) comments.get(1)).getMessage());

    assertEquals("miketyson", ((Comment) comments.get(2)).getActorId());
    assertNotNull(((Comment) comments.get(2)).getTime());
    assertEquals("third", ((Comment) comments.get(2)).getMessage());
  }

  public void testCommentsOnDifferentTokens() {
    Token tokenOne = new Token();
    tokenOne.addComment("one");
    tokenOne.addComment("two");
    tokenOne.addComment("three");
    session.save(tokenOne);
    long firstTokenId = tokenOne.getId();

    Token tokenTwo = new Token();
    tokenTwo.addComment("first");
    tokenTwo.addComment("second");
    tokenTwo.addComment("third");
    session.save(tokenTwo);
    long secondTokenId = tokenTwo.getId();

    newTransaction();
    tokenOne = (Token) session.load(Token.class, new Long(firstTokenId));
    List comments = tokenOne.getComments();
    assertEquals(3, comments.size());
    assertEquals("one", ((Comment) comments.get(0)).getMessage());
    assertEquals("two", ((Comment) comments.get(1)).getMessage());
    assertEquals("three", ((Comment) comments.get(2)).getMessage());

    tokenTwo = (Token) session.load(Token.class, new Long(secondTokenId));
    comments = tokenTwo.getComments();
    assertEquals(3, comments.size());
    assertEquals("first", ((Comment) comments.get(0)).getMessage());
    assertEquals("second", ((Comment) comments.get(1)).getMessage());
    assertEquals("third", ((Comment) comments.get(2)).getMessage());

    session.delete(tokenOne);
    session.delete(tokenTwo);
  }

  public void testTaskInstanceComment() {
    TaskInstance taskInstance = new TaskInstance();
    taskInstance.addComment("one");
    taskInstance.addComment("two");
    taskInstance.addComment("three");
    session.save(taskInstance);

    newTransaction();
    taskInstance = (TaskInstance) session.load(TaskInstance.class, new Long(
      taskInstance.getId()));
    List comments = taskInstance.getComments();
    assertEquals(3, comments.size());

    Comment comment = (Comment) comments.get(0);
    assertEquals("one", comment.getMessage());
    assertSame(taskInstance, comment.getTaskInstance());

    assertEquals("two", ((Comment) comments.get(1)).getMessage());
    assertEquals("three", ((Comment) comments.get(2)).getMessage());

    session.delete(taskInstance);
  }

  public void testCommentToTokenAndTaskInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getTaskInstances().iterator().next();
    taskInstance.addComment("one");
    taskInstance.addComment("two");
    taskInstance.addComment("three");

    processInstance = saveAndReload(processInstance);
    Token rootToken = processInstance.getRootToken();

    taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();
    assertEquals(3, taskInstance.getComments().size());
    assertEquals(3, rootToken.getComments().size());

    ArrayList tokenComments = new ArrayList(rootToken.getComments());
    ArrayList taskComments = new ArrayList(taskInstance.getComments());
    assertEquals(tokenComments, taskComments);
  }

  public void testTaskCommentAndLoadProcessInstance() {
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
    Collection unfinishedTasks = processInstance.getTaskMgmtInstance()
      .getUnfinishedTasks(processInstance.getRootToken());
    TaskInstance taskInstance = (TaskInstance) unfinishedTasks.iterator().next();
    taskInstance.addComment("please hurry!");

    processInstance = saveAndReload(processInstance);
    taskMgmtSession.loadTaskInstance(taskInstance.getId());
    graphSession.deleteProcessInstance(processInstance.getId());
  }
}
