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

import java.io.*;
import java.util.*;

import org.jbpm.instantiation.*;
import org.jbpm.util.EqualsUtil;

/**
 * is a process role (aka participant).
 */
public class Swimlane implements Serializable {
  
  private static final long serialVersionUID = 1L;

  long id = 0;
  protected String name = null;  
  protected String actorIdExpression = null;
  protected String pooledActorsExpression = null;
  protected Delegation assignmentDelegation = null;
  protected TaskMgmtDefinition taskMgmtDefinition = null;
  protected Set<Task> tasks = null;
  
  public Swimlane() {
  }

  public Swimlane(String name) {
    this.name = name;
  }

  /**
   * sets the taskMgmtDefinition unidirectionally.  use TaskMgmtDefinition.addSwimlane to create 
   * a bidirectional relation.
   */
  public void setTaskMgmtDefinition(TaskMgmtDefinition taskMgmtDefinition) {
    this.taskMgmtDefinition = taskMgmtDefinition;
  }

  // tasks ////////////////////////////////////////////////////////////////////

  public void addTask( Task task ) {
    if (tasks==null) tasks = new HashSet<Task>();
    tasks.add(task);
    task.setSwimlane(this);
  }

  public Set<Task> getTasks() {
    return tasks;
  }

  // equals ///////////////////////////////////////////////////////////////////
  // hack to support comparing hibernate proxies against the real objects
  // since this always falls back to ==, we don't need to overwrite the hashcode
  public boolean equals(Object o) {
    return EqualsUtil.equals(this, o);
  }

  public void setActorIdExpression(String actorIdExpression) {
    this.actorIdExpression = actorIdExpression;
    // Note: combination of actorIdExpression and pooledActorsExpression is allowed 
    // this.pooledActorsExpression = null;
    this.assignmentDelegation = null;
  }
  public void setPooledActorsExpression(String pooledActorsExpression) {
    // Note: combination of actorIdExpression and pooledActorsExpression is allowed 
    // this.actorIdExpression = null;
    this.pooledActorsExpression = pooledActorsExpression;
    this.assignmentDelegation = null;
  }
  public void setAssignmentDelegation(Delegation assignmentDelegation) {
    // assignment expressions and assignmentDelegation are mutually exclusive
    this.actorIdExpression = null;
    this.pooledActorsExpression = null;
    this.assignmentDelegation = assignmentDelegation;
  }

  // getters and setters //////////////////////////////////////////////////////

  public TaskMgmtDefinition getTaskMgmtDefinition() {
    return taskMgmtDefinition;
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
  public String getName() {
    return name;
  }
  public long getId() {
    return id;
  }
}
