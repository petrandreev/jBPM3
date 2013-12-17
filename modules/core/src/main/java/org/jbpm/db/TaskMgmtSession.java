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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.util.CollectionUtil;

public class TaskMgmtSession implements Serializable {

  private static final long serialVersionUID = 1L;

  final Session session;
  final JbpmSession jbpmSession;

  public TaskMgmtSession(JbpmSession jbpmSession) {
    this.session = jbpmSession.getSession();
    this.jbpmSession = jbpmSession;
  }

  public TaskMgmtSession(Session session) {
    this.session = session;
    this.jbpmSession = null;
  }

  /**
   * get the tasklist for a given actor.
   */
  public List<TaskInstance> findTaskInstances(String actorId) {
    try {
      Query query = session.getNamedQuery("TaskMgmtSession.findTaskInstancesByActorId");
      query.setString("actorId", actorId);
      return CollectionUtil.checkList(query.list(), TaskInstance.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get task instances list for actor '" + actorId + "'", e);
    }
  }

  /**
   * get all the task instances for all the given actorIds.
   * 
   * @return a list of task instances. An empty list is returned in case no task instances are
   * found.
   */
  public List<TaskInstance> findTaskInstances(List<String> actorIds) {
    try {
      Query query = session.getNamedQuery("TaskMgmtSession.findTaskInstancesByActorIds");
      query.setParameterList("actorIds", actorIds);
      return CollectionUtil.checkList(query.list(), TaskInstance.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get task instances list for actors '" + actorIds + "'", e);
    }
  }

  /**
   * get all the task instances for all the given actorIds.
   */
  public List<TaskInstance> findTaskInstances(String[] actorIds) {
    return findTaskInstances(Arrays.asList(actorIds));
  }

  /**
   * get the task instances for which the given actor is in the pool.
   */
  public List<TaskInstance> findPooledTaskInstances(String actorId) {
    try {
      Query query = session.getNamedQuery("TaskMgmtSession.findPooledTaskInstancesByActorId");
      query.setString("actorId", actorId);
      List<Long> taskInstanceIds = CollectionUtil.checkList(query.list(), Long.class);
      return findTaskInstancesByIds(taskInstanceIds);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException(
          "couldn't get pooled task instances list for actor '" + actorId + "'", e);
    }
  }

  /**
   * get the task instances for which the given actor is in the pool.
   */
  public List<TaskInstance> findPooledTaskInstances(List<String> actorIds) {
    try {
      Query query = session.getNamedQuery("TaskMgmtSession.findPooledTaskInstancesByActorIds");
      query.setParameterList("actorIds", actorIds);
      List<Long> taskInstanceIds = CollectionUtil.checkList(query.list(), Long.class);
      return findTaskInstancesByIds(taskInstanceIds);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get pooled task instances list for actors '"
          + actorIds
          + "'", e);
    }
  }

  /**
   * get active task instances for a given token.
   */
  public List<TaskInstance> findTaskInstancesByToken(long tokenId) {
    try {
      Query query = session.getNamedQuery("TaskMgmtSession.findTaskInstancesByTokenId");
      query.setLong("tokenId", tokenId);
      return CollectionUtil.checkList(query.list(), TaskInstance.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get task instances by token '" + tokenId + "'", e);
    }
  }

  /**
   * get active task instances for a given process instance.
   */
  public List<TaskInstance> findTaskInstancesByProcessInstance(ProcessInstance processInstance) {
    try {
      Query query = session.getNamedQuery("TaskMgmtSession.findTaskInstancesByProcessInstance");
      query.setEntity("processInstance", processInstance);
      return CollectionUtil.checkList(query.list(), TaskInstance.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get task instances by process instance '"
          + processInstance
          + "'", e);
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
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get task instance '" + taskInstanceId + "'", e);
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
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get task instance '" + taskInstanceId + "'", e);
    }
    return taskInstance;
  }

  public List<TaskInstance> findTaskInstancesByIds(List<Long> taskInstanceIds) {
    List<TaskInstance> result;
    if (taskInstanceIds.isEmpty()) {
      result = Collections.emptyList();
    }
    else {
      try {
        Query query = session.getNamedQuery("TaskMgmtSession.findTaskInstancesByIds");
        query.setParameterList("taskInstanceIds", taskInstanceIds);
        result = CollectionUtil.checkList(query.list(), TaskInstance.class);
      }
      catch (Exception e) {
        handle(e);
        throw new JbpmException("couldn't get task instances by ids '" + taskInstanceIds + "'", e);
      }
    }
    return result;
  }

  private void handle(Exception exception) {
    log.error(exception);
    if (jbpmSession != null)
      jbpmSession.handleException();
  }

  private static final Log log = LogFactory.getLog(TaskMgmtSession.class);
}
