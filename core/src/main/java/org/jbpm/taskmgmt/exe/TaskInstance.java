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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.context.exe.VariableContainer;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Identifiable;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.security.SecurityHelper;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.log.TaskAssignLog;
import org.jbpm.taskmgmt.log.TaskEndLog;
import org.jbpm.util.Clock;

/**
 * is one task instance that can be assigned to an actor (read: put in someone's task list) and
 * that can trigger the continuation of execution of the token upon completion.
 */
public class TaskInstance extends VariableContainer implements Identifiable, Assignable {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected String name;
  protected String description;
  protected String actorId;
  protected Date create;
  protected Date start;
  protected Date end;
  protected Date dueDate;
  protected int priority = Task.PRIORITY_NORMAL;
  protected boolean isCancelled;
  protected boolean isSuspended;
  protected boolean isOpen = true;
  protected boolean isSignalling = true;
  protected boolean isBlocking;
  protected Task task;
  protected Token token;
  protected SwimlaneInstance swimlaneInstance;
  protected TaskMgmtInstance taskMgmtInstance;
  protected ProcessInstance processInstance;
  protected Set pooledActors;
  protected List comments;

  // not persisted. extra information for task-assign event listeners
  protected String previousActorId;

  public TaskInstance() {
  }

  public TaskInstance(String taskName) {
    this.name = taskName;
  }

  public TaskInstance(String taskName, String actorId) {
    this.name = taskName;
    this.actorId = actorId;
  }

  public void setTask(Task task) {
    this.name = task.getName();
    this.description = task.getDescription();
    this.task = task;
    this.isBlocking = task.isBlocking();
    this.priority = task.getPriority();
    this.isSignalling = task.isSignalling();
  }

  private void submitVariables() {
    TaskController taskController;
    // if a task controller is present,
    if (task != null && (taskController = task.getTaskController()) != null) {
      // the task controller copies variables back into the process
      taskController.submitParameters(this);
    }
    // otherwise, all task-local variables are flushed to the process
    else if (token != null && variableInstances != null) {
      ContextInstance contextInstance = token.getProcessInstance().getContextInstance();
      boolean debug = log.isDebugEnabled();

      for (Iterator iter = variableInstances.values().iterator(); iter.hasNext();) {
        VariableInstance variableInstance = (VariableInstance) iter.next();
        String variableName = variableInstance.getName();
        if (debug) log.debug(this + " writes '" + variableName + '\'');
        contextInstance.setVariable(variableName, variableInstance.getValue(), token);
      }
    }
  }

  void initializeVariables() {
    if (task != null) {
      TaskController taskController = task.getTaskController();
      if (taskController != null) taskController.initializeVariables(this);
    }
  }

  public void create() {
    create(null);
  }

  public void create(ExecutionContext executionContext) {
    if (create != null) {
      throw new IllegalStateException(this + " was already created");
    }
    create = Clock.getCurrentTime();

    // if this task instance is associated with a task...
    if (task != null && executionContext != null) {
      // fire task create event
      // WARNING: The events create and assign are fired in the right order,
      // yet the logs are still not ordered properly
      executionContext.setTaskInstance(this);
      executionContext.setTask(task);
      task.fireEvent(Event.EVENTTYPE_TASK_CREATE, executionContext);
    }
  }

  public void assign(ExecutionContext executionContext) {
    TaskMgmtInstance taskMgmtInstance = executionContext.getTaskMgmtInstance();
    Swimlane swimlane = task.getSwimlane();
    // if this task is in a swimlane
    if (swimlane != null) {
      // if this is a task assignment for a start-state
      if (isStartTaskInstance()) {
        // initialize the swimlane
        swimlaneInstance = new SwimlaneInstance(swimlane);
        taskMgmtInstance.addSwimlaneInstance(swimlaneInstance);
        // with the current authenticated actor
        swimlaneInstance.setActorId(SecurityHelper.getAuthenticatedActorId());
      }
      // lazily initialize the swimlane...
      else {
        // get the swimlane instance (if there is any)
        swimlaneInstance = taskMgmtInstance.getInitializedSwimlaneInstance(executionContext, swimlane);
        // copy the swimlaneInstance assignment into the taskInstance assignment
        copySwimlaneInstanceAssignment(swimlaneInstance);
      }
    }
    else { // this task is not in a swimlane
      taskMgmtInstance.performAssignment(task.getAssignmentDelegation(), task.getActorIdExpression(), task.getPooledActorsExpression(), this, executionContext);
    }

    updatePooledActorsReferences(swimlaneInstance);
  }

  public boolean isStartTaskInstance() {
    if (taskMgmtInstance != null) {
      TaskMgmtDefinition taskMgmtDefinition = taskMgmtInstance.getTaskMgmtDefinition();
      if (taskMgmtDefinition != null) {
        return task != null
          && task.equals(taskMgmtInstance.getTaskMgmtDefinition().getStartTask());
      }
    }
    return false;
  }

  private void updatePooledActorsReferences(SwimlaneInstance swimlaneInstance) {
    if (pooledActors != null) {
      for (Iterator iter = pooledActors.iterator(); iter.hasNext();) {
        PooledActor pooledActor = (PooledActor) iter.next();
        pooledActor.setSwimlaneInstance(swimlaneInstance);
        pooledActor.addTaskInstance(this);
      }
    }
  }

  /**
   * copies the assignment (that includes both the swimlaneActorId and the set of pooledActors)
   * of the given swimlane into this taskInstance.
   */
  public void copySwimlaneInstanceAssignment(SwimlaneInstance swimlaneInstance) {
    setSwimlaneInstance(swimlaneInstance);
    setActorId(swimlaneInstance.getActorId());
    setPooledActors(swimlaneInstance.getPooledActors());
  }

  /**
   * gets the pool of actors for this task instance. If this task has a simlaneInstance and no
   * pooled actors, the pooled actors of the swimlane instance are returned.
   */
  public Set getPooledActors() {
    if (swimlaneInstance != null && (pooledActors == null || pooledActors.isEmpty())) {
      return swimlaneInstance.getPooledActors();
    }
    return pooledActors;
  }

  /**
   * (re)assign this task to the given actor. If this task is related to a swimlane instance,
   * that swimlane instance will be updated as well.
   */
  public void setActorId(String actorId) {
    setActorId(actorId, true);
  }

  /**
   * (re)assign this task to the given actor.
   * 
   * @param actorId is reference to the person that is assigned to this task.
   * @param overwriteSwimlane specifies if the related swimlane should be overwritten with the
   * given swimlaneActorId.
   */
  public void setActorId(String actorId, boolean overwriteSwimlane) {
    // do the actual assignment
    this.previousActorId = this.actorId;
    this.actorId = actorId;
    if (swimlaneInstance != null && overwriteSwimlane) {
      if (log.isDebugEnabled()) log.debug("assigning " + this + " to '" + actorId + '\'');
      swimlaneInstance.setActorId(actorId);
    }

    if (token != null) {
      // log this assignment
      token.addLog(new TaskAssignLog(this, previousActorId, actorId));

      if (task != null) {
        // fire task assign event
        // WARNING: The events create and assign are fired in the right order,
        // but the logs are still not ordered properly
        ExecutionContext executionContext = new ExecutionContext(token);
        executionContext.setTask(task);
        executionContext.setTaskInstance(this);
        task.fireEvent(Event.EVENTTYPE_TASK_ASSIGN, executionContext);
      }
    }
  }

  /** takes a set of String's as the actorIds */
  public void setPooledActors(String[] actorIds) {
    this.pooledActors = PooledActor.createPool(actorIds, null, this);
  }

  /**
   * can optionally be used to indicate that the actor is starting to work on this task
   * instance.
   */
  public void start() {
    if (start != null) {
      throw new IllegalStateException(this + " is already started");
    }
    start = Clock.getCurrentTime();

    // fire task start event
    if (token != null && task != null) {
      ExecutionContext executionContext = new ExecutionContext(token);
      executionContext.setTask(task);
      executionContext.setTaskInstance(this);
      task.fireEvent(Event.EVENTTYPE_TASK_START, executionContext);
    }
  }

  /**
   * convenience method that combines a {@link #setActorId(String)} and a {@link #start()}.
   */
  public void start(String actorId) {
    start(actorId, true);
  }

  /**
   * convenience method that combines a {@link #setActorId(String,boolean)} and a
   * {@link #start()}.
   */
  public void start(String actorId, boolean overwriteSwimlane) {
    setActorId(actorId, overwriteSwimlane);
    start();
  }

  /**
   * overwrite start date
   */
  public void setStart(Date date) {
    start = null;
  }

  private void markAsCancelled() {
    this.isCancelled = true;
    this.isOpen = false;
  }

  /**
   * cancels this task. This task instance will be marked as cancelled and as ended. But
   * cancellation doesn't influence signalling and continuation of process execution.
   */
  public void cancel() {
    markAsCancelled();
    end();
  }

  /**
   * cancels this task, takes the specified transition. This task intance will be marked as
   * cancelled and as ended. But cancellation doesn't influence singalling and continuation of
   * process execution.
   */
  public void cancel(Transition transition) {
    markAsCancelled();
    end(transition);
  }

  /**
   * cancels this task, takes the specified transition. This task intance will be marked as
   * cancelled and as ended. But cancellation doesn't influence singalling and continuation of
   * process execution.
   */
  public void cancel(String transitionName) {
    markAsCancelled();
    end(transitionName);
  }

  /**
   * marks this task as done. If this task is related to a task node this might trigger a signal
   * on the token.
   * 
   * @see #end(Transition)
   */
  public void end() {
    end((Transition) null);
  }

  /**
   * marks this task as done and specifies the name of a transition leaving the task-node for
   * the case that the completion of this task instances triggers a signal on the token. If this
   * task leads to a signal on the token, the given transition name will be used in the signal.
   * If this task completion does not trigger execution to move on, the transitionName is
   * ignored.
   */
  public void end(String transitionName) {
    if (task == null) {
      throw new JbpmException(this + " has no task definition");
    }

    Node node = task.getTaskNode();
    if (node == null) {
      GraphElement parent = task.getParent();
      if (!(parent instanceof Node)) {
        throw new JbpmException(this + " has no enclosing node");
      }
      node = (Node) parent;
    }

    Transition leavingTransition = node.getLeavingTransition(transitionName);
    if (leavingTransition == null) {
      throw new JbpmException(node + " has no leaving transition named " + transitionName);
    }
    end(leavingTransition);
  }

  /**
   * marks this task as done and specifies a transition leaving the task-node for the case that
   * the completion of this task instances triggers a signal on the token. If this task leads to
   * a signal on the token, the given transition name will be used in the signal. If this task
   * completion does not trigger execution to move on, the transition is ignored.
   */
  public void end(Transition transition) {
    if (end != null) {
      throw new IllegalStateException(this + " has ended");
    }
    if (isSuspended) {
      throw new JbpmException(this + " is suspended");
    }

    // record the end time
    this.end = Clock.getCurrentTime();
    this.isOpen = false;

    // fire the task instance end event
    if (token != null) {
      // submit the variables
      submitVariables();

      // log task completion
      token.addLog(new TaskEndLog(this));

      if (task != null) {
        // fire task end event
        ExecutionContext executionContext = new ExecutionContext(token);
        executionContext.setTask(task);
        executionContext.setTaskInstance(this);
        task.fireEvent(Event.EVENTTYPE_TASK_END, executionContext);

        // check whether completion triggers token signal
        if (isSignalling) {
          isSignalling = false;

          TaskNode taskNode = task.getTaskNode();
          if (isStartTaskInstance() // ending start task leads to signal
            || (taskNode != null && taskNode.completionTriggersSignal(this))) {
            boolean debug = log.isDebugEnabled();
            if (transition == null) {
              if (debug) {
                log.debug("taking default transition after completing " + task);
              }
              token.signal();
            }
            else {
              if (debug) {
                log.debug("taking" + transition + " after completing " + task);
              }
              token.signal(transition);
            }
          }
        }
      }
    }
  }

  public boolean hasEnded() {
    return end != null;
  }

  /**
   * suspends a process execution.
   */
  public void suspend() {
    if (!isOpen) {
      throw new JbpmException("task is not open");
    }
    isSuspended = true;
  }

  /**
   * resumes a process execution.
   */
  public void resume() {
    if (!isOpen) {
      throw new JbpmException("task is not open");
    }
    isSuspended = false;
  }

  // comments /////////////////////////////////////////////////////////////////

  public void addComment(String message) {
    addComment(new Comment(message));
  }

  public void addComment(Comment comment) {
    if (comment != null) {
      if (comments == null) comments = new ArrayList();
      comments.add(comment);
      comment.setTaskInstance(this);
      if (token != null) {
        comment.setToken(token);
        token.addComment(comment);
      }
    }
  }

  public List getComments() {
    return comments;
  }

  // task form ////////////////////////////////////////////////////////////////

  public boolean isLast() {
    return token != null && taskMgmtInstance != null
      && !taskMgmtInstance.hasUnfinishedTasks(token);
  }

  /**
   * is the list of transitions that can be used in the end method and it is null in case this
   * is not the last task instance.
   */
  public List getAvailableTransitions() {
    List transitions = null;
    if (!isLast() && token != null) {
      transitions = new ArrayList(token.getAvailableTransitions());
    }
    return transitions;
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TaskInstance)) return false;

    // task instance has no notion of equality other than identity
    // see Wfp15MiWithAPrioriRuntimeKnowledgeTest
    TaskInstance other = (TaskInstance) o;
    return id != 0 && id == other.getId();
  }

  public String toString() {
    return "TaskInstance"
      + (name != null ? '(' + name + ')' : id != 0 ? "(" + id + ')'
        : '@' + Integer.toHexString(hashCode()));
  }

  // private //////////////////////////////////////////////////////////////////

  /** takes a set of {@link PooledActor}s */
  public void setPooledActors(Set pooledActors) {
    if (pooledActors != null) {
      this.pooledActors = new HashSet(pooledActors);
      for (Iterator iter = pooledActors.iterator(); iter.hasNext();) {
        PooledActor pooledActor = (PooledActor) iter.next();
        pooledActor.addTaskInstance(this);
      }
    }
    else {
      this.pooledActors = null;
    }
  }

  // protected ////////////////////////////////////////////////////////////////

  protected VariableContainer getParentVariableContainer() {
    ContextInstance contextInstance = getContextInstance();
    return contextInstance != null ? contextInstance.getOrCreateTokenVariableMap(token) : null;
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getActorId() {
    return actorId;
  }

  public Date getDueDate() {
    return dueDate;
  }

  public void setDueDate(Date dueDate) {
    this.dueDate = dueDate;
  }

  public Date getEnd() {
    return end;
  }

  public void setEnd(Date end) {
    this.end = end;
  }

  public void setCreate(Date create) {
    this.create = create;
  }

  public long getId() {
    return id;
  }

  /**
   * This method has no effect.
   * 
   * @deprecated database identifier is not meant to be mutable
   */
  public void setId(long id) {
  }

  public Date getStart() {
    return start;
  }

  public TaskMgmtInstance getTaskMgmtInstance() {
    return taskMgmtInstance;
  }

  public void setTaskMgmtInstance(TaskMgmtInstance taskMgmtInstance) {
    this.taskMgmtInstance = taskMgmtInstance;
  }

  public Token getToken() {
    return token;
  }

  public void setToken(Token token) {
    this.token = token;
  }

  public void setSignalling(boolean isSignalling) {
    this.isSignalling = isSignalling;
  }

  public boolean isSignalling() {
    return isSignalling;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isBlocking() {
    return isBlocking;
  }

  public void setBlocking(boolean isBlocking) {
    this.isBlocking = isBlocking;
  }

  public Date getCreate() {
    return create;
  }

  public Task getTask() {
    return task;
  }

  public SwimlaneInstance getSwimlaneInstance() {
    return swimlaneInstance;
  }

  public void setSwimlaneInstance(SwimlaneInstance swimlaneInstance) {
    this.swimlaneInstance = swimlaneInstance;
  }

  public String getPreviousActorId() {
    return previousActorId;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public boolean isOpen() {
    return isOpen;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isSuspended() {
    return isSuspended;
  }

  public ProcessInstance getProcessInstance() {
    return processInstance;
  }

  public void setProcessInstance(ProcessInstance processInstance) {
    this.processInstance = processInstance;
  }

  private static final Log log = LogFactory.getLog(TaskInstance.class);
}
