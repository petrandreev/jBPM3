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
import java.util.HashSet;
import java.util.Set;

import org.jbpm.util.EqualsUtil;

public class PooledActor implements Serializable {

  private static final long serialVersionUID = 1L;
  
  long id = 0;
  int version = 0;
  protected String actorId = null;
  protected Set<TaskInstance> taskInstances = null;
  protected SwimlaneInstance swimlaneInstance = null;

  public static Set<PooledActor> createPool(String[] actorIds, SwimlaneInstance swimlaneInstance, TaskInstance taskInstance) {
    Set<PooledActor> pooledActors = new HashSet<PooledActor>();
    for (int i=0; i<actorIds.length; i++) {
      PooledActor pooledActor = new PooledActor(actorIds[i]);
      if (swimlaneInstance!=null) {
        pooledActor.setSwimlaneInstance(swimlaneInstance);
      }
      if (taskInstance!=null) {
        pooledActor.addTaskInstance(taskInstance);
      }
      pooledActors.add(pooledActor);
    }
    return pooledActors;
  }
  
  public static Set<String> extractActorIds(Set<PooledActor> pooledActors) {
    Set<String> extractedActorIds = null;
    if (pooledActors!=null) {
      extractedActorIds = new HashSet<String>();
      for (PooledActor pooledActor : pooledActors) {
        extractedActorIds.add(pooledActor.getActorId());
      }
    }
    return extractedActorIds;
  }

  public PooledActor() {
  }

  public PooledActor(String actorId) {
    this.actorId = actorId;
  }
  
  public void addTaskInstance(TaskInstance taskInstance) {
    if (taskInstances==null) taskInstances = new HashSet<TaskInstance>();
    taskInstances.add(taskInstance);
  }
  public Set<TaskInstance> getTaskInstances() {
    return taskInstances;
  }
  public void removeTaskInstance(TaskInstance taskInstance) {
    if (taskInstances!=null) {
      taskInstances.remove(taskInstance);
    }
  }

  // equals ///////////////////////////////////////////////////////////////////
  // hack to support comparing hibernate proxies against the real objects
  // since this always falls back to ==, we don't need to overwrite the hashcode
  public boolean equals(Object o) {
    return EqualsUtil.equals(this, o);
  }
  
  public String toString() {
    return "PooledActor("+actorId+")";
  }
  
  // getters and setters //////////////////////////////////////////////////////

  public String getActorId() {
    return actorId;
  }
  public void setActorId(String actorId) {
    this.actorId = actorId;
  }
  public SwimlaneInstance getSwimlaneInstance() {
    return swimlaneInstance;
  }
  public void setSwimlaneInstance(SwimlaneInstance swimlaneInstance) {
    this.swimlaneInstance = swimlaneInstance;
  }
  public long getId() {
    return id;
  }
}
