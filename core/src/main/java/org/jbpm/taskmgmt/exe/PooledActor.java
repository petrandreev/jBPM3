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
import java.util.Iterator;
import java.util.Set;

public class PooledActor implements Serializable {

  private static final long serialVersionUID = 1L;

  long id = 0;
  int version = 0;
  protected String actorId = null;
  protected Set taskInstances = null;
  protected SwimlaneInstance swimlaneInstance = null;

  public static Set createPool(String[] actorIds,
      SwimlaneInstance swimlaneInstance, TaskInstance taskInstance) {
    Set pooledActors = new HashSet();
    for (int i = 0; i < actorIds.length; i++) {
      PooledActor pooledActor = new PooledActor(actorIds[i]);
      if (swimlaneInstance != null) {
        pooledActor.setSwimlaneInstance(swimlaneInstance);
      }
      if (taskInstance != null) {
        pooledActor.addTaskInstance(taskInstance);
      }
      pooledActors.add(pooledActor);
    }
    return pooledActors;
  }

  public static Set extractActorIds(Set poooledActors) {
    Set extractedActorIds = null;
    if (poooledActors != null) {
      extractedActorIds = new HashSet();
      Iterator iter = poooledActors.iterator();
      while (iter.hasNext()) {
        PooledActor pooledActor = (PooledActor) iter.next();
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
    if (taskInstances == null) taskInstances = new HashSet();
    taskInstances.add(taskInstance);
  }

  public Set getTaskInstances() {
    return taskInstances;
  }

  public void removeTaskInstance(TaskInstance taskInstance) {
    if (taskInstances != null) {
      taskInstances.remove(taskInstance);
    }
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PooledActor)) return false;

    PooledActor other = (PooledActor) o;
    if (id != 0 && id == other.getId()) return true;

    return actorId.equals(other.getActorId());
  }

  public int hashCode() {
    return actorId.hashCode();
  }

  public String toString() {
    return "PooledActor(" + actorId + ')';
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
