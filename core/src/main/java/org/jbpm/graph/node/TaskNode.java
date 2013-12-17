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
package org.jbpm.graph.node;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * is a node that relates to one or more tasks. Property <code>signal</code> specifies how task
 * completion triggers continuation of execution.
 */
public class TaskNode extends Node {

  private static final long serialVersionUID = 1L;

  /**
   * execution always continues, regardless whether tasks are created or still unfinished.
   */
  public static final int SIGNAL_UNSYNCHRONIZED = 0;
  /**
   * execution never continues, regardless whether tasks are created or still unfinished.
   */
  public static final int SIGNAL_NEVER = 1;
  /**
   * proceeds execution when the first task instance is completed. when no tasks are created on
   * entrance of this node, execution is continued.
   */
  public static final int SIGNAL_FIRST = 2;
  /**
   * proceeds execution when the first task instance is completed. when no tasks are created on
   * entrance of this node, execution waits in the task node till tasks are created.
   */
  public static final int SIGNAL_FIRST_WAIT = 3;
  /**
   * proceeds execution when the last task instance is completed. when no tasks are created on
   * entrance of this node, execution is continued.
   */
  public static final int SIGNAL_LAST = 4;
  /**
   * proceeds execution when the last task instance is completed. when no tasks are created on
   * entrance of this node, execution waits in the task node till tasks are created.
   */
  public static final int SIGNAL_LAST_WAIT = 5;

  public static int parseSignal(String text) {
    if ("unsynchronized".equalsIgnoreCase(text)) return SIGNAL_UNSYNCHRONIZED;
    if ("never".equalsIgnoreCase(text)) return SIGNAL_NEVER;
    if ("first".equalsIgnoreCase(text)) return SIGNAL_FIRST;
    if ("first-wait".equalsIgnoreCase(text)) return SIGNAL_FIRST_WAIT;
    if ("last-wait".equalsIgnoreCase(text)) return SIGNAL_LAST_WAIT;
    // default value
    return SIGNAL_LAST;
  }

  public static String signalToString(int signal) {
    switch (signal) {
    case SIGNAL_UNSYNCHRONIZED:
      return "unsynchronized";
    case SIGNAL_NEVER:
      return "never";
    case SIGNAL_FIRST:
      return "first";
    case SIGNAL_FIRST_WAIT:
      return "first-wait";
    case SIGNAL_LAST:
      return "last";
    case SIGNAL_LAST_WAIT:
      return "last-wait";
    default:
      return null;
    }
  }

  private Set tasks;
  private int signal = SIGNAL_LAST;
  private boolean createTasks = true;
  private boolean endTasks;

  public TaskNode() {
  }

  public TaskNode(String name) {
    super(name);
  }

  public NodeType getNodeType() {
    return NodeType.Task;
  }

  public void read(Element element, JpdlXmlReader jpdlReader) {
    // get the signal
    String signalText = element.attributeValue("signal");
    if (signalText != null) {
      signal = parseSignal(signalText);
    }

    // create tasks
    String createTasksText = element.attributeValue("create-tasks");
    createTasks = jpdlReader.readBoolean(createTasksText, true);

    // create tasks
    String removeTasksText = element.attributeValue("end-tasks");
    endTasks = jpdlReader.readBoolean(removeTasksText, false);

    // parse the tasks
    jpdlReader.readTasks(element, this);
  }

  public void addTask(Task task) {
    if (tasks == null) tasks = new HashSet();
    task.setTaskNode(this);
    tasks.add(task);
  }

  // node behaviour methods
  // ///////////////////////////////////////////////////////////////////////////

  public void execute(ExecutionContext executionContext) {
    TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();

    // if this tasknode should create instances
    if (createTasks && tasks != null) {
      for (Iterator iter = tasks.iterator(); iter.hasNext();) {
        Task task = (Task) iter.next();
        executionContext.setTask(task);
        if (evaluateTaskCondition(task.getCondition(), executionContext)) {
          tmi.createTaskInstance(task, executionContext);
        }
      }
    }

    // check if we should continue execution
    boolean continueExecution;
    switch (signal) {
    case SIGNAL_UNSYNCHRONIZED:
      continueExecution = true;
      break;
    case SIGNAL_FIRST:
    case SIGNAL_LAST:
      continueExecution = tmi.getSignallingTasks(executionContext).isEmpty();
      break;
    default:
      continueExecution = false;
    }

    if (continueExecution) leave(executionContext);
  }

  private boolean evaluateTaskCondition(String condition, ExecutionContext executionContext) {
    if (condition == null) return true;

    Boolean result = (Boolean) JbpmExpressionEvaluator
      .evaluate(condition, executionContext, Boolean.class);
    return Boolean.TRUE.equals(result);
  }

  public void leave(ExecutionContext executionContext, Transition transition) {
    TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();
    Token token = executionContext.getToken();
    if (tmi.hasBlockingTaskInstances(token)) {
      throw new IllegalStateException(this + " still has blocking tasks");
    }
    removeTaskInstanceSynchronization(token);
    super.leave(executionContext, transition);
  }

  // task behaviour methods
  // ///////////////////////////////////////////////////////////////////////////

  public boolean completionTriggersSignal(TaskInstance taskInstance) {
    boolean completionTriggersSignal;
    switch (signal) {
    case SIGNAL_FIRST:
    case SIGNAL_FIRST_WAIT:
      completionTriggersSignal = true;
      break;
    case SIGNAL_LAST:
    case SIGNAL_LAST_WAIT:
      completionTriggersSignal = isLastToComplete(taskInstance);
      break;
    default:
      completionTriggersSignal = false;
    }
    return completionTriggersSignal;
  }

  private boolean isLastToComplete(TaskInstance taskInstance) {
    Token token = taskInstance.getToken();
    TaskMgmtInstance tmi = taskInstance.getTaskMgmtInstance();

    boolean isLastToComplete = true;
    for (Iterator iter = tmi.getTaskInstances().iterator(); iter.hasNext() && isLastToComplete;) {
      TaskInstance other = (TaskInstance) iter.next();
      if (token != null && token.equals(other.getToken()) && !other.equals(taskInstance)
        && other.isSignalling() && !other.hasEnded()) {
        isLastToComplete = false;
      }
    }

    return isLastToComplete;
  }

  public void removeTaskInstanceSynchronization(Token token) {
    TaskMgmtInstance tmi = token.getProcessInstance().getTaskMgmtInstance();
    Collection taskInstances = tmi.getTaskInstances();
    if (taskInstances != null) {
      for (Iterator iter = taskInstances.iterator(); iter.hasNext();) {
        TaskInstance taskInstance = (TaskInstance) iter.next();
        if (token.equals(taskInstance.getToken())) {
          // remove signalling
          if (taskInstance.isSignalling()) {
            taskInstance.setSignalling(false);
          }
          // remove blocking
          if (taskInstance.isBlocking()) {
            taskInstance.setBlocking(false);
          }
          // if this is a non-finished task and all tasks should be finished
          if (!taskInstance.hasEnded() && endTasks && tasks.contains(taskInstance.getTask())) {
            // end this task
            taskInstance.end();
          }
        }
      }
    }
  }

  /**
   * is a Map with the tasks, keyed by task-name or an empty map in case no tasks are present in
   * this task-node.
   */
  public Map getTasksMap() {
    Map tasksMap = new HashMap();
    if (tasks != null) {
      for (Iterator iter = tasks.iterator(); iter.hasNext();) {
        Task task = (Task) iter.next();
        tasksMap.put(task.getName(), task);
      }
    }
    return tasksMap;
  }

  /**
   * is the task in this task-node with the given name or null if the given task does not exist
   * in this node.
   */
  public Task getTask(String taskName) {
    return (Task) getTasksMap().get(taskName);
  }

  public Set getTasks() {
    return tasks;
  }

  public int getSignal() {
    return signal;
  }

  public boolean getCreateTasks() {
    return createTasks;
  }

  public boolean isEndTasks() {
    return endTasks;
  }

  public void setCreateTasks(boolean createTasks) {
    this.createTasks = createTasks;
  }

  public void setEndTasks(boolean endTasks) {
    this.endTasks = endTasks;
  }

  public void setSignal(int signal) {
    this.signal = signal;
  }

  public void setTasks(Set tasks) {
    this.tasks = tasks;
  }
}
