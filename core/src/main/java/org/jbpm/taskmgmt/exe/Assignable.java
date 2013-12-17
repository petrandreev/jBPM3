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

/**
 * common superclass for {@link org.jbpm.taskmgmt.exe.TaskInstance}s and 
 * {@link org.jbpm.taskmgmt.exe.SwimlaneInstance}s used by 
 * the {@link org.jbpm.taskmgmt.def.AssignmentHandler} interface.
 */
public interface Assignable extends Serializable {

  /**
   * sets the responsible for this assignable object.
   * Use this method to assign the task into a user's personal 
   * task list.
   */
  public void setActorId(String actorId);
  
  /**
   * sets the resource pool for this assignable as a set of {@link PooledActor}s.
   * Use this method to offer the task to a group of users.  Each user in the group
   * can then take the task by calling the {@link #setActorId(String)}.
   */
  public void setPooledActors(String[] pooledActors);
}
