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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.Delegation;
import org.jbpm.instantiation.UserCodeInterceptor;
import org.jbpm.instantiation.UserCodeInterceptorConfig;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.security.SecurityHelper;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.TaskInstanceFactory;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.log.TaskCreateLog;
import org.jbpm.util.Clock;

/**
 * process instance extension for managing tasks on a process instance.
 */
public class TaskMgmtInstance extends ModuleInstance {

  private static final long serialVersionUID = 1L;

  private TaskMgmtDefinition taskMgmtDefinition;
  private Map swimlaneInstances;
  private Set taskInstances;

  /** stores task instances having variable updates. not persisted. */
  private Collection taskInstanceVariableUpdates;

  public TaskMgmtInstance() {
  }

  public TaskMgmtInstance(TaskMgmtDefinition taskMgmtDefinition) {
    this.taskMgmtDefinition = taskMgmtDefinition;
  }

  // task instances ///////////////////////////////////////////////////////////

  public TaskInstance createTaskInstance() {
    return createTaskInstance(null, (ExecutionContext) null);
  }

  public TaskInstance createTaskInstance(Task task) {
    return createTaskInstance(task, (ExecutionContext) null);
  }

  public TaskInstance createTaskInstance(Token token) {
    return createTaskInstance(null, new ExecutionContext(token));
  }

  /**
   * creates an instance of the given task, for the given token.
   */
  public TaskInstance createTaskInstance(Task task, Token token) {
    ExecutionContext executionContext = new ExecutionContext(token);
    executionContext.setTask(task);
    return createTaskInstance(task, executionContext);
  }

  /**
   * creates an instance of the given task, in the given execution context.
   */
  public TaskInstance createTaskInstance(Task task, ExecutionContext executionContext) {
    // create new task instance
    TaskInstance taskInstance = instantiateNewTaskInstance(executionContext);
    // assign database identifier
    Services.assignId(taskInstance);
    // set task definition
    if (task != null) taskInstance.setTask(task);
    // bind task instance to this task management instance
    addTaskInstance(taskInstance);

    if (executionContext != null) {
      // set token
      Token token = executionContext.getToken();
      taskInstance.setToken(token);
      taskInstance.setProcessInstance(token.getProcessInstance());
      // initialize variables
      taskInstance.initializeVariables();

      // calculate due date
      String dueDateText;
      if (task != null && (dueDateText = task.getDueDate()) != null) {
        Date dueDate;

        // evaluate base date expression
        if (dueDateText.startsWith("#{") || dueDateText.startsWith("${")) {
          int braceIndex = dueDateText.indexOf('}');
          if (braceIndex == -1) {
            throw new JbpmException("invalid due date, closing brace missing: " + dueDateText);
          }

          String baseDateExpression = dueDateText.substring(0, braceIndex + 1);
          Object result = JbpmExpressionEvaluator
            .evaluate(baseDateExpression, executionContext);

          Date baseDate;
          if (result instanceof Date) {
            baseDate = (Date) result;
          }
          else if (result instanceof Calendar) {
            Calendar calendar = (Calendar) result;
            baseDate = calendar.getTime();
          }
          else {
            throw new JbpmException(baseDateExpression + " returned " + result
              + " instead of date or calendar");
          }

          String durationText = dueDateText.substring(braceIndex + 1).trim();
          if (durationText.length() > 0) {
            char durationSeparator = durationText.charAt(0);
            if (durationSeparator != '+' && durationSeparator != '-') {
              throw new JbpmException("invalid due date, '+' or '-' missing after expression: "
                + dueDateText);
            }
            dueDate = new BusinessCalendar().add(baseDate, new Duration(durationText));
          }
          else {
            dueDate = baseDate;
          }
        }
        // take current time as base date
        else {
          dueDate = new BusinessCalendar().add(Clock.getCurrentTime(), new Duration(dueDateText));
        }
        taskInstance.setDueDate(dueDate);
      }

      try {
        // update the executionContext
        executionContext.setTask(task);
        executionContext.setTaskInstance(taskInstance);
        executionContext.setEventSource(task);

        // evaluate the description
        if (task != null) {
          String description = task.getDescription();
          if (description != null) {
            String result = (String) JbpmExpressionEvaluator
              .evaluate(description, executionContext, String.class);
            if (result != null) {
              taskInstance.setDescription(result);
            }
          }
        }

        // create the task instance
        taskInstance.create(executionContext);

        // if task definition is present, perform assignment
        if (task != null) taskInstance.assign(executionContext);
      }
      finally {
        // clean the executionContext
        executionContext.setTask(null);
        executionContext.setTaskInstance(null);
        executionContext.setEventSource(null);
      }

      // log this creation
      // WARNING: The events create and assign are fired in the right order, but
      // the logs are still not ordered properly.
      token.addLog(new TaskCreateLog(taskInstance, taskInstance.getActorId()));
    }
    else {
      taskInstance.create();
    }
    return taskInstance;
  }

  public SwimlaneInstance getInitializedSwimlaneInstance(ExecutionContext executionContext,
    Swimlane swimlane) {
    // initialize the swimlane
    if (swimlaneInstances == null) swimlaneInstances = new HashMap();
    SwimlaneInstance swimlaneInstance = (SwimlaneInstance) swimlaneInstances.get(swimlane
      .getName());
    if (swimlaneInstance == null) {
      swimlaneInstance = new SwimlaneInstance(swimlane);
      addSwimlaneInstance(swimlaneInstance);
      // assign the swimlaneInstance
      performAssignment(swimlane.getAssignmentDelegation(), swimlane.getActorIdExpression(), swimlane
        .getPooledActorsExpression(), swimlaneInstance, executionContext);
    }

    return swimlaneInstance;
  }

  public void performAssignment(Delegation assignmentDelegation, String actorIdExpression,
    String pooledActorsExpression, Assignable assignable, ExecutionContext executionContext) {
    if (assignmentDelegation != null) {
      performAssignmentDelegation(assignmentDelegation, assignable, executionContext);
    }
    else {
      if (actorIdExpression != null) {
        performAssignmentActorIdExpr(actorIdExpression, assignable, executionContext);
      }
      if (pooledActorsExpression != null) {
        performAssignmentPooledActorsExpr(pooledActorsExpression, assignable, executionContext);
      }
    }
  }

  private void performAssignmentDelegation(Delegation assignmentDelegation,
    Assignable assignable, ExecutionContext executionContext) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      // set context class loader correctly for delegation class
      // https://jira.jboss.org/jira/browse/JBPM-1448
      ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(executionContext
        .getProcessDefinition());
      Thread.currentThread().setContextClassLoader(processClassLoader);

      // instantiate the assignment handler
      AssignmentHandler assignmentHandler = (AssignmentHandler) assignmentDelegation
        .instantiate();
      // invoke the assignment handler
      UserCodeInterceptor userCodeInterceptor = UserCodeInterceptorConfig
        .getUserCodeInterceptor();
      try {
        if (userCodeInterceptor != null) {
          userCodeInterceptor.executeAssignment(assignmentHandler, assignable, executionContext);
        }
        else {
          assignmentHandler.assign(assignable, executionContext);
        }
      }
      catch (Exception e) {
        GraphElement eventSource = executionContext.getEventSource();
        if (eventSource == null) {
          throw e instanceof JbpmException ? (JbpmException) e
            : new DelegationException("event source is null", e);
        }
        eventSource.raiseException(e, executionContext);
      }
    }
    finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  private void performAssignmentActorIdExpr(String actorIdExpression, Assignable assignable,
    ExecutionContext executionContext) {
    String actorId = (String) JbpmExpressionEvaluator
      .evaluate(actorIdExpression, executionContext, String.class);
    if (actorId == null) {
      throw new JbpmException(actorIdExpression + " returned null");
    }
    assignable.setActorId(actorId);
  }

  private void performAssignmentPooledActorsExpr(String pooledActorsExpression,
    Assignable assignable, ExecutionContext executionContext) {
    Object result = JbpmExpressionEvaluator.evaluate(pooledActorsExpression, executionContext);

    String[] pooledActors;
    if (result instanceof String) {
      String csv = (String) result;
      pooledActors = csv.split(",");
      for (int i = 0; i < pooledActors.length; i++) {
        pooledActors[i] = pooledActors[i].trim();
      }
    }
    else if (result instanceof String[]) {
      pooledActors = (String[]) result;
    }
    else if (result instanceof Collection) {
      Collection collection = (Collection) result;
      pooledActors = (String[]) collection.toArray(new String[collection.size()]);
    }
    else {
      throw new JbpmException(pooledActorsExpression + " returned " + result
        + " instead of comma-separated string, collection or string array");
    }
    assignable.setPooledActors(pooledActors);
  }

  /**
   * creates a task instance on the rootToken, and assigns it to the currently authenticated
   * user.
   */
  public TaskInstance createStartTaskInstance() {
    TaskInstance taskInstance = null;
    Task startTask = taskMgmtDefinition.getStartTask();
    if (startTask != null) {
      Token rootToken = processInstance.getRootToken();
      ExecutionContext executionContext = new ExecutionContext(rootToken);
      taskInstance = createTaskInstance(startTask, executionContext);
      taskInstance.setActorId(SecurityHelper.getAuthenticatedActorId());
    }
    return taskInstance;
  }

  private TaskInstance instantiateNewTaskInstance(ExecutionContext executionContext) {
    if (Configs.hasObject("jbpm.task.instance.factory")) {
      TaskInstanceFactory factory =
        (TaskInstanceFactory) Configs.getObject("jbpm.task.instance.factory");
      return factory.createTaskInstance(executionContext);
    }
    return new TaskInstance();
  }

  /**
   * is true if the given token has task instances that keep the token from leaving the current
   * node.
   */
  public boolean hasBlockingTaskInstances(Token token) {
    if (taskInstances != null) {
      for (Iterator i = taskInstances.iterator(); i.hasNext();) {
        TaskInstance taskInstance = (TaskInstance) i.next();
        if (!taskInstance.hasEnded() && taskInstance.isBlocking() && token != null
          && token.equals(taskInstance.getToken())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * is true if the given token has task instances that are not yet ended.
   */
  public boolean hasUnfinishedTasks(Token token) {
    return (getUnfinishedTasks(token).size() > 0);
  }

  /**
   * is the collection of {@link TaskInstance}s on the given token that are not ended.
   */
  public Collection getUnfinishedTasks(Token token) {
    Collection unfinishedTasks = new ArrayList();
    if (taskInstances != null) {
      for (Iterator i = taskInstances.iterator(); i.hasNext();) {
        TaskInstance task = (TaskInstance) i.next();
        if (!task.hasEnded() && token != null && token.equals(task.getToken())) {
          unfinishedTasks.add(task);
        }
      }
    }
    return unfinishedTasks;
  }

  /**
   * is true if there are {@link TaskInstance}s on the given token that can trigger the token to
   * continue.
   */
  public boolean hasSignallingTasks(ExecutionContext executionContext) {
    return (getSignallingTasks(executionContext).size() > 0);
  }

  /**
   * is the collection of {@link TaskInstance}s for the given token that can trigger the token
   * to continue.
   */
  public Collection getSignallingTasks(ExecutionContext executionContext) {
    Collection signallingTasks = new ArrayList();
    if (taskInstances != null) {
      for (Iterator i = taskInstances.iterator(); i.hasNext();) {
        TaskInstance taskInstance = (TaskInstance) i.next();
        if (taskInstance.isSignalling()
          && (executionContext.getToken().equals(taskInstance.getToken()))) {
          signallingTasks.add(taskInstance);
        }
      }
    }
    return signallingTasks;
  }

  /**
   * returns all the taskInstances for the this process instance. This includes task instances
   * that have been completed previously.
   */
  public Collection getTaskInstances() {
    return taskInstances;
  }

  public void addTaskInstance(TaskInstance taskInstance) {
    if (taskInstances == null) taskInstances = new HashSet();
    taskInstances.add(taskInstance);
    taskInstance.setTaskMgmtInstance(this);
  }

  public void removeTaskInstance(TaskInstance taskInstance) {
    if (taskInstances != null) {
      taskInstances.remove(taskInstance);
    }
  }

  // swimlane instances ///////////////////////////////////////////////////////

  public Map getSwimlaneInstances() {
    return swimlaneInstances;
  }

  public void addSwimlaneInstance(SwimlaneInstance swimlaneInstance) {
    if (swimlaneInstances == null) swimlaneInstances = new HashMap();
    swimlaneInstances.put(swimlaneInstance.getName(), swimlaneInstance);
    swimlaneInstance.setTaskMgmtInstance(this);
  }

  public SwimlaneInstance getSwimlaneInstance(String swimlaneName) {
    return swimlaneInstances != null ? (SwimlaneInstance) swimlaneInstances.get(swimlaneName)
      : null;
  }

  public SwimlaneInstance createSwimlaneInstance(String swimlaneName) {
    Swimlane swimlane = taskMgmtDefinition != null ? taskMgmtDefinition
      .getSwimlane(swimlaneName) : null;
    if (swimlane == null) {
      throw new JbpmException("swimlane does not exist: " + swimlaneName);
    }
    return createSwimlaneInstance(swimlane);
  }

  public SwimlaneInstance createSwimlaneInstance(Swimlane swimlane) {
    if (swimlaneInstances == null) swimlaneInstances = new HashMap();

    SwimlaneInstance swimlaneInstance = new SwimlaneInstance(swimlane);
    swimlaneInstance.setTaskMgmtInstance(this);
    swimlaneInstances.put(swimlaneInstance.getName(), swimlaneInstance);
    return swimlaneInstance;
  }

  // getters and setters //////////////////////////////////////////////////////

  public TaskMgmtDefinition getTaskMgmtDefinition() {
    return taskMgmtDefinition;
  }

  /**
   * suspends all task instances for this process instance.
   */
  public void suspend(Token token) {
    if (token == null) {
      throw new JbpmException("can't suspend task instances for token null");
    }
    if (taskInstances != null) {
      for (Iterator i = taskInstances.iterator(); i.hasNext();) {
        TaskInstance taskInstance = (TaskInstance) i.next();
        if ((token.equals(taskInstance.getToken())) && (taskInstance.isOpen())) {
          taskInstance.suspend();
        }
      }
    }
  }

  /**
   * resumes all task instances for this process instance.
   */
  public void resume(Token token) {
    if (token == null) {
      throw new JbpmException("can't suspend task instances for token null");
    }
    if (taskInstances != null) {
      for (Iterator i = taskInstances.iterator(); i.hasNext();) {
        TaskInstance taskInstance = (TaskInstance) i.next();
        if ((token.equals(taskInstance.getToken())) && (taskInstance.isOpen())) {
          taskInstance.resume();
        }
      }
    }
  }

  void notifyVariableUpdate(TaskInstance taskInstance) {
    if (taskInstanceVariableUpdates == null) {
      taskInstanceVariableUpdates = new HashSet();
    }
    taskInstanceVariableUpdates.add(taskInstance);
  }

  /**
   * returns the collection of task instance with variable updates.
   */
  public Collection getTaskInstancesWithVariableUpdates() {
    return taskInstanceVariableUpdates;
  }

  /**
   * convenience method to end all tasks related to a given process instance.
   */
  public void endAll() {
    if (taskInstances != null) {
      for (Iterator i = taskInstances.iterator(); i.hasNext();) {
        TaskInstance taskInstance = (TaskInstance) i.next();
        if (!taskInstance.hasEnded()) {
          taskInstance.end();
        }
      }
    }
  }

  /**
   * removes signalling capabilities from all task instances related to the given token.
   */
  public void removeSignalling(Token token) {
    if (token != null && taskInstances != null) {
      for (Iterator i = taskInstances.iterator(); i.hasNext();) {
        TaskInstance taskInstance = (TaskInstance) i.next();
        if (token.equals(taskInstance.getToken())) {
          taskInstance.setSignalling(false);
        }
      }
    }
  }
}
