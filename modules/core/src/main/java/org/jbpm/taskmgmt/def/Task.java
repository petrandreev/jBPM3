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

import org.jbpm.JbpmException;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.instantiation.Delegation;

/**
 * defines a task and how the actor must be calculated at runtime.
 */
public class Task extends GraphElement {
  
  private static final long serialVersionUID = 1L;
  
  public static final int PRIORITY_HIGHEST = 1;
  public static final int PRIORITY_HIGH = 2;
  public static final int PRIORITY_NORMAL = 3;
  public static final int PRIORITY_LOW = 4;
  public static final int PRIORITY_LOWEST = 5;
  
  public static int parsePriority(String priorityText) {
    if ("highest".equalsIgnoreCase(priorityText)) return PRIORITY_HIGHEST;
    else if ("high".equalsIgnoreCase(priorityText)) return PRIORITY_HIGH;
    else if ("normal".equalsIgnoreCase(priorityText)) return PRIORITY_NORMAL;
    else if ("low".equalsIgnoreCase(priorityText)) return PRIORITY_LOW;
    else if ("lowest".equalsIgnoreCase(priorityText)) return PRIORITY_LOWEST;
    try {
      return Integer.parseInt(priorityText);
    } catch (NumberFormatException e) {
      throw new JbpmException("priority '"+priorityText+"' could not be parsed as a priority");
    }
  }

  protected boolean isBlocking = false;
  protected boolean isSignalling = true;
  protected String condition = null;
  protected String dueDate = null;
  protected int priority = PRIORITY_NORMAL;
  protected TaskNode taskNode = null;
  protected StartState startState = null;
  protected TaskMgmtDefinition taskMgmtDefinition = null;
  protected Swimlane swimlane = null;
  protected String actorIdExpression = null;
  protected String pooledActorsExpression = null;
  protected Delegation assignmentDelegation = null;
  protected TaskController taskController = null;
  
  public Task() {
  }

  public Task(String name) {
    this.name = name;
  }

  // event types //////////////////////////////////////////////////////////////

  static final String[] supportedEventTypes = new String[]{
    Event.EVENTTYPE_TASK_CREATE,
    Event.EVENTTYPE_TASK_ASSIGN,
    Event.EVENTTYPE_TASK_START,
    Event.EVENTTYPE_TASK_END
  };
  public String[] getSupportedEventTypes() {
    return supportedEventTypes;
  }

  // task instance factory methods ////////////////////////////////////////////
  
  /**
   * sets the taskNode unidirectionally.  use {@link TaskNode#addTask(Task)} to create 
   * a bidirectional relation.
   */
  public void setTaskNode(TaskNode taskNode) {
    this.taskNode = taskNode;
  }
  
  /**
   * sets the taskMgmtDefinition unidirectionally.  use TaskMgmtDefinition.addTask to create 
   * a bidirectional relation.
   */
  public void setTaskMgmtDefinition(TaskMgmtDefinition taskMgmtDefinition) {
    this.taskMgmtDefinition = taskMgmtDefinition;
  }

  /**
   * sets the swimlane.  Since a task can have max one of swimlane or assignmentHandler, 
   * this method removes the swimlane if it is set.
   */
  public void setAssignmentDelegation(Delegation assignmentDelegation) {
    this.actorIdExpression = null;
    this.pooledActorsExpression = null;
    this.assignmentDelegation = assignmentDelegation;
    this.swimlane = null;
  }
  /**
   * sets the actorId expression.  The assignmentExpression is a JSF-like
   * expression to perform assignment.  Since a task can have max one of swimlane or 
   * assignmentHandler, this method removes the swimlane and assignmentDelegation if 
   * it is set.
   */
  public void setActorIdExpression(String actorIdExpression) {
    this.actorIdExpression = actorIdExpression;
    // Note: combination of actorIdExpression and pooledActorsExpression is allowed 
    // this.pooledActorsExpression = null;
    this.assignmentDelegation = null;
    this.swimlane = null;
  }
  /**
   * sets the actorId expression.  The assignmentExpression is a JSF-like
   * expression to perform assignment.  Since a task can have max one of swimlane or 
   * assignmentHandler, this method removes the other forms of assignment.
   */
  public void setPooledActorsExpression(String pooledActorsExpression) {
    // Note: combination of actorIdExpression and pooledActorsExpression is allowed 
    // this.actorIdExpression = null;
    this.pooledActorsExpression = pooledActorsExpression;
    this.assignmentDelegation = null;
    this.swimlane = null;
  }
  /**
   * sets the swimlane unidirectionally.  Since a task can have max one of swimlane or assignmentHandler, 
   * this method removes the assignmentHandler and assignmentExpression if one of those isset.  To create 
   * a bidirectional relation, use {@link Swimlane#addTask(Task)}.
   */
  public void setSwimlane(Swimlane swimlane) {
    this.actorIdExpression = null;
    this.pooledActorsExpression = null;
    this.assignmentDelegation = null;
    this.swimlane = swimlane;
  }

  // parent ///////////////////////////////////////////////////////////////////

  public GraphElement getParent() {
    if (taskNode!=null) {
      return taskNode;
    } 
    if (startState!=null) {
      return startState;
    } 
    return processDefinition;
  }
  
  // getters and setters //////////////////////////////////////////////////////
  
  public TaskMgmtDefinition getTaskMgmtDefinition() {
    return taskMgmtDefinition;
  }
  public Swimlane getSwimlane() {
    return swimlane;
  }
  public boolean isBlocking() {
    return isBlocking;
  }
  public void setBlocking(boolean isBlocking) {
    this.isBlocking = isBlocking;
  }
  public TaskNode getTaskNode() {
    return taskNode;
  }
  public String getActorIdExpression() {
    return actorIdExpression;
  }
  public String getPooledActorsExpression() {
    return pooledActorsExpression;
  }
  public Delegation getAssignmentDelegation() {
    return assignmentDelegation;
  }
  public String getDueDate() {
    return dueDate;
  }
  public void setDueDate(String duedate) {
    this.dueDate = duedate;
  }
  public TaskController getTaskController() {
    return taskController;
  }
  public void setTaskController(TaskController taskController) {
    this.taskController = taskController;
  }
  public int getPriority() {
    return priority;
  }
  public void setPriority(int priority) {
    this.priority = priority;
  }
  public StartState getStartState() {
    return startState;
  }
  public void setStartState(StartState startState) {
    this.startState = startState;
  }
  public boolean isSignalling() {
    return isSignalling;
  }
  public void setSignalling(boolean isSignalling) {
    this.isSignalling = isSignalling;
  }
  public String getCondition() {
    return condition;
  }
  public void setCondition(String condition) {
    this.condition = condition;
  }
}
