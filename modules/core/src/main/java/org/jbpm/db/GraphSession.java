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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.ProcessState;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.util.CollectionUtil;

/**
 * are the graph related database operations.
 */
public class GraphSession {

  final Session session;
  final JbpmSession jbpmSession;

  public GraphSession(JbpmSession jbpmSession) {
    this.session = jbpmSession.getSession();
    this.jbpmSession = jbpmSession;
  }

  public GraphSession(Session session) {
    this.session = session;
    this.jbpmSession = null;
  }

  // process definitions //////////////////////////////////////////////////////

  public void deployProcessDefinition(ProcessDefinition processDefinition) {
    String processDefinitionName = processDefinition.getName();
    // if the process definition has a name (versioning applies to named process definitions only)
    if (processDefinitionName != null) {
      // find the current latest process definition
      ProcessDefinition previousLatestVersion = findLatestProcessDefinition(processDefinitionName);
      // if there is a current latest process definition
      if (previousLatestVersion != null) {
        // take the next version number
        processDefinition.setVersion(previousLatestVersion.getVersion() + 1);
      }
      else {
        // start from 1
        processDefinition.setVersion(1);
      }
      session.save(processDefinition);
    }
    else {
      throw new JbpmException("process definition does not have a name");
    }
  }

  /**
   * saves the process definitions. this method does not assign a version number. that is the
   * responsibility of the {@link #deployProcessDefinition(ProcessDefinition)
   * deployProcessDefinition} method.
   */
  public void saveProcessDefinition(ProcessDefinition processDefinition) {
    try {
      session.save(processDefinition);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not save " + processDefinition, e);
    }
  }

  /**
   * loads a process definition from the database by the identifier.
   * 
   * @throws JbpmException in case the referenced process definition doesn't exist.
   */
  public ProcessDefinition loadProcessDefinition(long processDefinitionId) {
    try {
      return (ProcessDefinition) session.load(ProcessDefinition.class,
          new Long(processDefinitionId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not load process definition " + processDefinitionId, e);
    }
  }

  /**
   * gets a process definition from the database by the identifier.
   * 
   * @return the referenced process definition or null in case it doesn't exist.
   */
  public ProcessDefinition getProcessDefinition(long processDefinitionId) {
    try {
      return (ProcessDefinition) session.get(ProcessDefinition.class, new Long(processDefinitionId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not get process definition " + processDefinitionId, e);
    }
  }

  /**
   * queries the database for a process definition with the given name and version.
   */
  public ProcessDefinition findProcessDefinition(String name, int version) {
    try {
      return (ProcessDefinition) session.getNamedQuery("GraphSession.findProcessDefinitionByNameAndVersion")
          .setString("name", name)
          .setInteger("version", version)
          .uniqueResult();
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not find process definition '"
          + name
          + "' at version "
          + version, e);
    }
  }

  /**
   * queries the database for the latest version of a process definition with the given name.
   */
  public ProcessDefinition findLatestProcessDefinition(String name) {
    try {
      return (ProcessDefinition) session.getNamedQuery("GraphSession.findLatestProcessDefinitionQuery")
          .setString("name", name)
          .setMaxResults(1)
          .uniqueResult();
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not find process definition '" + name + "'", e);
    }
  }

  /**
   * queries the database for the latest version of each process definition. Process definitions are
   * distinct by name.
   */
  public List<ProcessDefinition> findLatestProcessDefinitions() {
    try {
      List<?> tuples = session.getNamedQuery("GraphSession.findLatestProcessDefinitions")
          .list();
      List<ProcessDefinition> result = new ArrayList<ProcessDefinition>();
      for (Object[] tuple : CollectionUtil.checkList(tuples, Object[].class)) {
        String name = (String) tuple[0];
        Integer version = (Integer) tuple[1];
        result.add(findProcessDefinition(name, version));
      }
      return result;
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not find latest versions of process definitions", e);
    }
  }

  public List<ProcessDefinition> findProcessDefinitions(Collection<Long> processDefinitionIds) {
    List<?> processDefinitions = session.createCriteria(ProcessDefinition.class)
        .add(Restrictions.in("id", processDefinitionIds))
        .list();
    return CollectionUtil.checkList(processDefinitions, ProcessDefinition.class);
  }

  /**
   * queries the database for all process definitions, ordered by name (ascending), then by version
   * (descending).
   */
  public List<ProcessDefinition> findAllProcessDefinitions() {
    try {
      List<?> processDefinitions = session.getNamedQuery("GraphSession.findAllProcessDefinitions")
          .list();
      return CollectionUtil.checkList(processDefinitions, ProcessDefinition.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not find all process definitions", e);
    }
  }

  /**
   * queries the database for all versions of process definitions with the given name, ordered by
   * version (descending).
   */
  public List<ProcessDefinition> findAllProcessDefinitionVersions(String name) {
    try {
      List<?> processDefinitions = session.getNamedQuery("GraphSession.findAllProcessDefinitionVersions")
          .setString("name", name)
          .list();
      return CollectionUtil.checkList(processDefinitions, ProcessDefinition.class);
    }
    catch (HibernateException e) {
      log.error(e);
      throw new JbpmException("could not find all versions of process definition '" + name + "'", e);
    }
  }

  public void deleteProcessDefinition(long processDefinitionId) {
    deleteProcessDefinition(loadProcessDefinition(processDefinitionId));
  }

  public void deleteProcessDefinition(ProcessDefinition processDefinition) {
    if (processDefinition == null) {
      throw new IllegalArgumentException("processDefinition cannot be null");
    }

    try {
      // delete all the process instances of this definition
      List<?> processInstanceIds = session.getNamedQuery("GraphSession.findAllProcessInstanceIdsForDefinition")
          .setLong("processDefinitionId", processDefinition.getId())
          .list();
      for (Long processInstanceId : CollectionUtil.checkList(processInstanceIds, Long.class)) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        if (processInstance != null) {
          deleteProcessInstance(processInstance);
        }
        else {
          log.debug("process instance " + processInstanceId + " has been deleted already");
        }
      }

      List<ProcessState> referencingProcessStates = findReferencingProcessStates(processDefinition);
      for (ProcessState processState : referencingProcessStates) {
        processState.setSubProcessDefinition(null);
      }

      // then delete the process definition
      session.delete(processDefinition);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not delete " + processDefinition, e);
    }
  }

  List<ProcessState> findReferencingProcessStates(ProcessDefinition subProcessDefinition) {
    List<?> processStates = session.getNamedQuery("GraphSession.findReferencingProcessStates")
        .setEntity("subProcessDefinition", subProcessDefinition)
        .list();
    return CollectionUtil.checkList(processStates, ProcessState.class);
  }

  // process instances ////////////////////////////////////////////////////////

  /**
   * @deprecated use {@link org.jbpm.JbpmContext#save(ProcessInstance)} instead.
   * @throws UnsupportedOperationException
   */
  public void saveProcessInstance(ProcessInstance processInstance) {
    throw new UnsupportedOperationException("use JbpmContext.save(ProcessInstance) instead");
  }

  /**
   * loads a process instance from the database by the identifier. This throws an exception in case
   * the process instance doesn't exist.
   * 
   * @see #getProcessInstance(long)
   * @throws JbpmException in case the process instance doesn't exist.
   */
  public ProcessInstance loadProcessInstance(long processInstanceId) {
    try {
      return (ProcessInstance) session.load(ProcessInstance.class, new Long(processInstanceId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not load process instance " + processInstanceId, e);
    }
  }

  /**
   * gets a process instance from the database by the identifier. This method returns null in case
   * the given process instance doesn't exist.
   */
  public ProcessInstance getProcessInstance(long processInstanceId) {
    try {
      return (ProcessInstance) session.get(ProcessInstance.class, new Long(processInstanceId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not get process instance " + processInstanceId, e);
    }
  }

  /**
   * loads a token from the database by the identifier.
   * 
   * @return the token.
   * @throws JbpmException in case the referenced token doesn't exist.
   */
  public Token loadToken(long tokenId) {
    try {
      return (Token) session.load(Token.class, new Long(tokenId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not load token " + tokenId, e);
    }
  }

  /**
   * gets a token from the database by the identifier.
   * 
   * @return the token or null in case the token doesn't exist.
   */
  public Token getToken(long tokenId) {
    try {
      return (Token) session.get(Token.class, new Long(tokenId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not get token " + tokenId, e);
    }
  }

  /**
   * locks a process instance in the database.
   */
  public void lockProcessInstance(long processInstanceId) {
    try {
      session.load(ProcessInstance.class, processInstanceId, LockMode.UPGRADE);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not lock process instance " + processInstanceId, e);
    }
  }

  /**
   * locks a process instance in the database.
   */
  public void lockProcessInstance(ProcessInstance processInstance) {
    try {
      session.lock(processInstance, LockMode.UPGRADE);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not lock " + processInstance, e);
    }
  }

  /**
   * fetches all processInstances for the given process definition from the database. The returned
   * list of process instances is sorted start date, youngest first.
   */
  public List<ProcessInstance> findProcessInstances(long processDefinitionId) {
    try {
      List<?> processInstances = session.getNamedQuery("GraphSession.findAllProcessInstancesForDefinition")
          .setLong("processDefinitionId", processDefinitionId)
          .list();
      return CollectionUtil.checkList(processInstances, ProcessInstance.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not find process instances for process definition "
          + processDefinitionId, e);
    }
  }

  public void deleteProcessInstance(long processInstanceId) {
    deleteProcessInstance(loadProcessInstance(processInstanceId));
  }

  public void deleteProcessInstance(ProcessInstance processInstance) {
    deleteProcessInstance(processInstance, true, true);
  }

  public void deleteProcessInstance(ProcessInstance processInstance, boolean includeTasks,
      boolean includeJobs) {
    if (processInstance == null) {
      throw new IllegalArgumentException("processInstance cannot be null");
    }

    try {
      // delete outstanding jobs
      if (includeJobs) {
        log.debug("deleting jobs for " + processInstance);
        int entityCount = session.getNamedQuery("GraphSession.deleteJobsForProcessInstance")
            .setEntity("processInstance", processInstance)
            .executeUpdate();
        log.debug("deleted " + entityCount + " jobs for " + processInstance);
      }

      // delete logs
      log.debug("deleting logs for " + processInstance);
      deleteLogs(processInstance);

      // detach from parent process token
      Token superProcessToken = processInstance.getSuperProcessToken();
      if (superProcessToken != null) {
        log.debug("detaching "
            + processInstance
            + " from super process token "
            + superProcessToken.getId());
        processInstance.setSuperProcessToken(null);
        superProcessToken.setSubProcessInstance(null);
      }

      // delete tokens and subprocess instances
      log.debug("deleting subprocesses for " + processInstance);
      deleteSubProcesses(processInstance);

      // delete tasks (TaskLogs reference tasks, so tasks must be deleted after logs)
      if (includeTasks) {
        log.debug("deleting tasks for " + processInstance);
        List<?> tasks = session.getNamedQuery("GraphSession.findTaskInstancesForProcessInstance")
            .setEntity("processInstance", processInstance)
            .list();
        for (Object task : tasks) {
          session.delete(task);
        }
      }

      // delete the process instance
      log.debug("deleting " + processInstance);
      session.delete(processInstance);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not delete " + processInstance, e);
    }
  }

  void deleteLogs(ProcessInstance processInstance) {
    List<?> logs = session.getNamedQuery("GraphSession.findLogsForProcessInstance")
        .setEntity("processInstance", processInstance)
        .list();
    for (ProcessLog processLog : CollectionUtil.checkList(logs, ProcessLog.class)) {
      session.delete(processLog);
    }
  }

  void deleteSubProcesses(ProcessInstance processInstance) {
    if (processInstance != null) {
      List<?> subProcessInstances = session.getNamedQuery("GraphSession.findSubProcessInstances")
          .setEntity("processInstance", processInstance)
          .list();

      if (subProcessInstances.isEmpty()) {
        log.debug("no subprocesses to delete for " + processInstance);
        return;
      }

      for (ProcessInstance subProcessInstance : CollectionUtil.checkList(subProcessInstances, ProcessInstance.class)) {
        log.debug("preparing to delete sub process instance " + subProcessInstance.getId());
        deleteProcessInstance(subProcessInstance);
      }
    }
  }

  public static class AverageNodeTimeEntry {

    private long nodeId;
    private String nodeName;
    private int count;
    private long averageDuration;
    private long minDuration;
    private long maxDuration;

    public long getNodeId() {
      return nodeId;
    }

    public void setNodeId(final long nodeId) {
      this.nodeId = nodeId;
    }

    public String getNodeName() {
      return nodeName;
    }

    public void setNodeName(final String nodeName) {
      this.nodeName = nodeName;
    }

    public int getCount() {
      return count;
    }

    public void setCount(final int count) {
      this.count = count;
    }

    public long getAverageDuration() {
      return averageDuration;
    }

    public void setAverageDuration(final long averageDuration) {
      this.averageDuration = averageDuration;
    }

    public long getMinDuration() {
      return minDuration;
    }

    public void setMinDuration(final long minDuration) {
      this.minDuration = minDuration;
    }

    public long getMaxDuration() {
      return maxDuration;
    }

    public void setMaxDuration(final long maxDuration) {
      this.maxDuration = maxDuration;
    }
  }

  public List<AverageNodeTimeEntry> calculateAverageTimeByNode(long processDefinitionId,
      long minumumDurationMillis) {
    try {
      List<?> tuples = session.getNamedQuery("GraphSession.calculateAverageTimeByNode")
          .setLong("processDefinitionId", processDefinitionId)
          .setDouble("minimumDuration", minumumDurationMillis)
          .list();

      List<AverageNodeTimeEntry> results;
      if (!tuples.isEmpty()) {
        results = new ArrayList<AverageNodeTimeEntry>();

        for (Object[] values : CollectionUtil.checkList(tuples, Object[].class)) {
          AverageNodeTimeEntry averageNodeTimeEntry = new AverageNodeTimeEntry();
          averageNodeTimeEntry.setNodeId(((Number) values[0]).longValue());
          averageNodeTimeEntry.setNodeName((String) values[1]);
          averageNodeTimeEntry.setCount(((Number) values[2]).intValue());
          averageNodeTimeEntry.setAverageDuration(((Number) values[3]).longValue());
          averageNodeTimeEntry.setMinDuration(((Number) values[4]).longValue());
          averageNodeTimeEntry.setMaxDuration(((Number) values[5]).longValue());

          results.add(averageNodeTimeEntry);
        }
      }
      else {
        results = Collections.emptyList();
      }
      return results;
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not calculate average time by node for process definition "
          + processDefinitionId, e);
    }
  }

  public List<Node> findActiveNodesByProcessInstance(ProcessInstance processInstance) {
    try {
      List<?> nodes = session.getNamedQuery("GraphSession.findActiveNodesByProcessInstance")
          .setEntity("processInstance", processInstance)
          .list();
      return CollectionUtil.checkList(nodes, Node.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not find active nodes for " + processInstance, e);
    }
  }

  public ProcessInstance getProcessInstance(ProcessDefinition processDefinition, String key) {
    try {
      return (ProcessInstance) session.getNamedQuery("GraphSession.findProcessInstanceByKey")
          .setEntity("processDefinition", processDefinition)
          .setString("key", key)
          .uniqueResult();
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("could not get process instance with key '" + key + "'", e);
    }
  }

  public ProcessInstance loadProcessInstance(ProcessDefinition processDefinition, String key) {
    ProcessInstance processInstance = getProcessInstance(processDefinition, key);
    if (processInstance == null) {
      throw new JbpmException("no process instance was found with key '" + key + "'");
    }
    return processInstance;
  }

  private void handle(Exception e) {
    log.error(e);
    if (jbpmSession != null) jbpmSession.handleException();
  }

  private static final Log log = LogFactory.getLog(GraphSession.class);
}
