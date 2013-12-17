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

import java.util.Collection;
import java.util.Iterator;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;

public class TaskExecutionTest extends AbstractJbpmTestCase {

  public void testSignalLast() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='laundry' />"
      + "    <task name='dishes' />"
      + "    <task name='change nappy' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    assertNotNull(tmi);
    assertEquals(3, tmi.getTaskInstances().size());

    for (Iterator iter = tmi.getTaskInstances().iterator(); iter.hasNext();) {
      // before every task is completed, check if the token is still in node a
      assertSame(processDefinition.getNode("a"), token.getNode());
      TaskInstance taskInstance = (TaskInstance) iter.next();
      taskInstance.end();
    }

    // after the 3 tasks have been completed, check if the token has moved to b
    assertSame(processDefinition.getNode("b"), token.getNode());
  }

  public void testSignalFirst() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a' signal='first'>"
      + "    <task name='laundry' />"
      + "    <task name='dishes' />"
      + "    <task name='change nappy' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    assertNotNull(tmi);
    assertEquals(3, tmi.getTaskInstances().size());

    // only before the first task, the token should be in a
    assertSame(processDefinition.getNode("a"), token.getNode());

    for (Iterator iter = tmi.getTaskInstances().iterator(); iter.hasNext();) {
      // before every task is completed, check if the token is still in node a
      TaskInstance taskInstance = (TaskInstance) iter.next();
      taskInstance.end();

      // after each task, check if the token has moved to b
      assertSame(processDefinition.getNode("b"), token.getNode());
    }
  }

  public void testSignalNever() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a' signal='never'>"
      + "    <task name='laundry' />"
      + "    <task name='dishes' />"
      + "    <task name='change nappy' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    assertNotNull(tmi);
    assertEquals(3, tmi.getTaskInstances().size());

    // only before the first task, the token should be in a
    assertSame(processDefinition.getNode("a"), token.getNode());

    for (Iterator iter = tmi.getTaskInstances().iterator(); iter.hasNext();) {
      // before every task is completed, check if the token is still in node a
      TaskInstance taskInstance = (TaskInstance) iter.next();
      taskInstance.end();

      // after each task, check if the token remains in a
      assertSame(processDefinition.getNode("a"), token.getNode());
    }
    // signal='never' is used when an external trigger should trigger execution,
    // without any relation to the tasks finishing
    processInstance.signal();
    assertSame(processDefinition.getNode("b"), token.getNode());
  }

  public void testSignalUnsynchronized() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a' signal='unsynchronized'>"
      + "    <task name='laundry' />"
      + "    <task name='dishes' />"
      + "    <task name='change nappy' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    assertNotNull(tmi);
    assertEquals(3, tmi.getTaskInstances().size());

    // unsynchronized means execution continues right after creating the tasks
    assertSame(processDefinition.getNode("b"), token.getNode());

    for (Iterator iter = tmi.getTaskInstances().iterator(); iter.hasNext();) {
      // before every task is completed, check if the token is still in node a
      TaskInstance taskInstance = (TaskInstance) iter.next();
      taskInstance.end();

      // after each task, check if the token remains in b
      assertSame(processDefinition.getNode("b"), token.getNode());
    }
  }

  public void testCreateTasksDisabled() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a' create-tasks='false'>"
      + "    <task name='laundry' />"
      + "    <task name='dishes' />"
      + "    <task name='change nappy' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    assertNotNull(tmi);
    assertNull(tmi.getTaskInstances());

    // if signal is last (def) and no task is created, execution continues
    assertSame(processDefinition.getNode("b"), token.getNode());
  }

  public void testNonBlockingTask() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='laundry' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    Token token = processInstance.getRootToken();
    assertSame(processDefinition.getNode("a"), token.getNode());

    processInstance.signal();
    assertSame(processDefinition.getNode("b"), token.getNode());
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
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    assertSame(processDefinition.getNode("a"), token.getNode());
    try {
      processInstance.signal();
      fail("expected exception");
    }
    catch (IllegalStateException e) {
      // OK
    }
  }

  public void testTransitionNameInTaskEnd() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='change nappy' />"
      + "    <transition name='ok' to='a' />"
      + "    <transition name='messed all over the floor' to='b' />"
      + "  </task-node>"
      + "  <state name='a' />"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    assertSame(processDefinition.getNode("t"), token.getNode());

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();
    taskInstance.end("messed all over the floor");
    assertSame(processDefinition.getNode("b"), token.getNode());
  }

  public void testTransitionInTaskEnd() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='change nappy' />"
      + "    <transition name='ok' to='a' />"
      + "    <transition name='messed all over the floor' to='b' />"
      + "  </task-node>"
      + "  <state name='a' />"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    assertSame(processDefinition.getNode("t"), token.getNode());

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();

    Transition messedTransition = processDefinition.getNode("t")
      .getLeavingTransition("messed all over the floor");
    taskInstance.end(messedTransition);
    assertSame(processDefinition.getNode("b"), token.getNode());
  }

  public void testTaskPriorityHighest() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='change nappy' priority='highest' />"
      + "  </task-node>"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    assertSame(processDefinition.getNode("t"), token.getNode());

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();
    assertEquals(Task.PRIORITY_HIGHEST, taskInstance.getPriority());
  }

  public void testTaskPriorityOne() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='change nappy' priority='1' />"
      + "  </task-node>"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    assertSame(processDefinition.getNode("t"), token.getNode());

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();
    assertEquals(1, taskInstance.getPriority());
  }

  public void testTaskDescriptionEvaluation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a' >"
      + "    <task name='laundry' description='the #{company} case'>"
      + "      <controller>"
      + "        <variable name='company' />"
      + "      </controller>"
      + "    </task>"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("company", "jboss");
    processInstance.signal();

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getTaskInstances().iterator().next();
    assertEquals("the jboss case", taskInstance.getDescription());
  }

  public void testTaskDescriptionExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling' description='This task is about #{item}' />"
      + "  </task-node>"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("item", "shoes");
    processInstance.signal();

    Token token = processInstance.getRootToken();
    Collection taskInstances = processInstance.getTaskMgmtInstance().getUnfinishedTasks(token);
    assertEquals(1, taskInstances.size());

    TaskInstance taskInstance = (TaskInstance) taskInstances.iterator().next();
    assertEquals("This task is about shoes", taskInstance.getDescription());
  }
}
