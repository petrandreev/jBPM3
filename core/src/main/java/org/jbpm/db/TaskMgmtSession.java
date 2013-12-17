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
package org.jbpm.db;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class TaskMgmtSession {

  final Session session;
  /** @deprecated */
  final JbpmSession jbpmSession;

  /** @deprecated use {@link #TaskMgmtSession(Session)} instead */
  public TaskMgmtSession(JbpmSession jbpmSession) {
    this.session = jbpmSession.getSession();
    this.jbpmSession = jbpmSession;
  }

  public TaskMgmtSession(Session session) {
    this.session = session;
    this.jbpmSession = null;
  }

  /**
   * get the task list for a given actor.
   */
  public List findTaskInstances(String actorId) {
    try {
      return session.getNamedQuery("TaskMgmtSession.findTaskInstancesByActorId")
        .setString("actorId", actorId)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find task instances by actor " + actorId, e);
    }
  }

  /**
   * get all the task instances for all the given actor identifiers.
   * 
   * @return a list of task instances. An empty list is returned in case no task
   * instances are found.
   */
  public List findTaskInstances(List actorIds) {
    if (actorIds.isEmpty()) return Collections.EMPTY_LIST;
    try {
      return session.getNamedQuery("TaskMgmtSession.findTaskInstancesByActorIds")
        .setParameterList("actorIds", actorIds)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find task instances by actors " + actorIds,
        e);
    }
  }

  /**
   * get all the task instances for all the given actorIds.
   */
  public List findTaskInstances(String[] actorIds) {
    return findTaskInstances(Arrays.asList(actorIds));
  }

  /**
   * get the task instances for which the given actor is in the pool.
   */
  public List findPooledTaskInstances(String actorId) {
    try {
      List taskInstanceIds = session.getNamedQuery("TaskMgmtSession.findPooledTaskInstancesByActorId")
        .setString("actorId", actorId)
        .list();
      return findTaskInstancesByIds(taskInstanceIds);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find pooled task instances by actor "
        + actorId, e);
    }
  }

  /**
   * get the task instances for which the given actor is in the pool.
   */
  public List findPooledTaskInstances(List actorIds) {
    if (actorIds.isEmpty()) return Collections.EMPTY_LIST;
    try {
      List taskInstanceIds = session.getNamedQuery("TaskMgmtSession.findPooledTaskInstancesByActorIds")
        .setParameterList("actorIds", actorIds)
        .list();
      return findTaskInstancesByIds(taskInstanceIds);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find pooled task instances by actors "
        + actorIds, e);
    }
  }

  /**
   * get active task instances for a given token.
   */
  public List findTaskInstancesByToken(long tokenId) {
    try {
      return session.getNamedQuery("TaskMgmtSession.findTaskInstancesByTokenId")
        .setLong("tokenId", tokenId)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find task instances by token " + tokenId, e);
    }
  }

  /**
   * get active task instances for a given process instance.
   */
  public List findTaskInstancesByProcessInstance(ProcessInstance processInstance) {
    try {
      return session.getNamedQuery("TaskMgmtSession.findTaskInstancesByProcessInstance")
        .setEntity("processInstance", processInstance)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find task instances by " + processInstance,
        e);
    }
  }

  /**
   * get the task instance for a given task instance-id.
   */
  public TaskInstance loadTaskInstance(long taskInstanceId) {
    TaskInstance taskInstance = null;
    try {
      taskInstance = (TaskInstance) session.load(TaskInstance.class, new Long(taskInstanceId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not load task instance " + taskInstanceId, e);
    }
    return taskInstance;
  }

  /**
   * get the task instance for a given task instance-id.
   */
  public TaskInstance getTaskInstance(long taskInstanceId) {
    TaskInstance taskInstance = null;
    try {
      taskInstance = (TaskInstance) session.get(TaskInstance.class, new Long(taskInstanceId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not get task instance " + taskInstanceId, e);
    }
    return taskInstance;
  }

  public List findTaskInstancesByIds(List taskInstanceIds) {
    if (taskInstanceIds.isEmpty()) return Collections.EMPTY_LIST;
    try {
      return session.getNamedQuery("TaskMgmtSession.findTaskInstancesByIds")
        .setParameterList("taskInstanceIds", taskInstanceIds)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find task instances by identifiers "
        + taskInstanceIds, e);
    }
  }

  private void handle(HibernateException exception) {
    // exception will be rethrown, no need to log here
    if (jbpmSession != null) jbpmSession.handleException();
  }
}
