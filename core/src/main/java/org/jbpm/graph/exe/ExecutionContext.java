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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.job.Timer;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class ExecutionContext {

  protected Token token;
  protected Event event;
  protected GraphElement eventSource;
  protected Action action;
  protected Throwable exception;
  protected Transition transition;
  protected Node transitionSource;
  protected Task task;
  protected Timer timer;
  protected TaskInstance taskInstance;
  protected ProcessInstance subProcessInstance;

  public ExecutionContext(Token token) {
    this.token = token;
    this.subProcessInstance = token.getSubProcessInstance();
  }

  public ExecutionContext(ExecutionContext other) {
    this.token = other.token;
    this.event = other.event;
    this.action = other.action;
  }

  public Node getNode() {
    return token.getNode();
  }

  public ProcessDefinition getProcessDefinition() {
    ProcessInstance processInstance = getProcessInstance();
    return (processInstance != null ? processInstance.getProcessDefinition() : null);
  }

  public void setAction(Action action) {
    this.action = action;
    if (action != null) this.event = action.getEvent();
  }

  public ProcessInstance getProcessInstance() {
    return token.getProcessInstance();
  }

  public String toString() {
    return "ExecutionContext(" + token.getName() + ')';
  }

  // convenience methods //////////////////////////////////////////////////////

  /**
   * set a process variable.
   */
  public void setVariable(String name, Object value) {
    if (taskInstance != null) {
      taskInstance.setVariable(name, value);
    }
    else {
      getContextInstance().setVariable(name, value, token);
    }
  }

  /**
   * get a process variable.
   */
  public Object getVariable(String name) {
    return taskInstance != null ? taskInstance.getVariable(name)
      : getContextInstance().getVariable(name, token);
  }

  /**
   * leave this node over the default transition. This method is only available
   * on node actions. Not on actions that are executed on events. Actions on
   * events cannot change the flow of execution.
   */
  public void leaveNode() {
    getNode().leave(this);
  }

  /**
   * leave this node over the given transition. This method is only available on
   * node actions. Not on actions that are executed on events. Actions on events
   * cannot change the flow of execution.
   */
  public void leaveNode(String transitionName) {
    getNode().leave(this, transitionName);
  }

  /**
   * leave this node over the given transition. This method is only available on
   * node actions. Not on actions that are executed on events. Actions on events
   * cannot change the flow of execution.
   */
  public void leaveNode(Transition transition) {
    getNode().leave(this, transition);
  }

  public ModuleDefinition getDefinition(Class clazz) {
    return getProcessDefinition().getDefinition(clazz);
  }

  public ModuleInstance getInstance(Class clazz) {
    if (token != null) {
      ProcessInstance processInstance = token.getProcessInstance();
      if (processInstance != null) return processInstance.getInstance(clazz);
    }
    return null;
  }

  public ContextInstance getContextInstance() {
    return (ContextInstance) getInstance(ContextInstance.class);
  }

  public TaskMgmtInstance getTaskMgmtInstance() {
    return (TaskMgmtInstance) getInstance(TaskMgmtInstance.class);
  }

  public JbpmContext getJbpmContext() {
    return JbpmContext.getCurrentJbpmContext();
  }

  // getters and setters //////////////////////////////////////////////////////

  public void setTaskInstance(TaskInstance taskInstance) {
    this.taskInstance = taskInstance;
    this.task = taskInstance != null ? taskInstance.getTask() : null;
  }

  public Token getToken() {
    return token;
  }

  public Action getAction() {
    return action;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public Throwable getException() {
    return exception;
  }

  public void setException(Throwable exception) {
    this.exception = exception;
  }

  public Transition getTransition() {
    return transition;
  }

  public void setTransition(Transition transition) {
    this.transition = transition;
  }

  public Node getTransitionSource() {
    return transitionSource;
  }

  public void setTransitionSource(Node transitionSource) {
    this.transitionSource = transitionSource;
  }

  public GraphElement getEventSource() {
    return eventSource;
  }

  public void setEventSource(GraphElement eventSource) {
    this.eventSource = eventSource;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

  public TaskInstance getTaskInstance() {
    return taskInstance;
  }

  public ProcessInstance getSubProcessInstance() {
    return subProcessInstance;
  }

  public void setSubProcessInstance(ProcessInstance subProcessInstance) {
    this.subProcessInstance = subProcessInstance;
  }

  public Timer getTimer() {
    return timer;
  }

  public void setTimer(Timer timer) {
    this.timer = timer;
  }

  // thread local execution context

  static ThreadLocal threadLocalContextStack = new ThreadLocal() {
    protected Object initialValue() {
      return new ArrayList();
    }
  };

  static List getContextStack() {
    return (List) threadLocalContextStack.get();
  }

  public static void pushCurrentContext(ExecutionContext executionContext) {
    getContextStack().add(executionContext);
  }

  public static void popCurrentContext(ExecutionContext executionContext) {
    List stack = getContextStack();
    int index = stack.lastIndexOf(executionContext);
    if (index == -1) {
      log.warn(executionContext + " was not found in thread-local stack;"
        + " do not access ExecutionContext instances from multiple threads");
    }
    else {
      if (index < stack.size() - 1) {
        log.warn(executionContext + " was not popped in reverse push order;"
          + " make sure you pop every context you push");
      }
      // remove execution context from stack, no matter what
      stack.remove(index);
    }
  }

  public static ExecutionContext currentExecutionContext() {
    List stack = getContextStack();
    return stack.isEmpty() ? null : (ExecutionContext) stack.get(stack.size() - 1);
  }

  private static final Log log = LogFactory.getLog(ExecutionContext.class);
}
