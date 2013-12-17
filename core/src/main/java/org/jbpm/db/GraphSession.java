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
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.ProcessState;
import org.jbpm.persistence.JbpmPersistenceException;

/**
 * graph-related database operations.
 */
public class GraphSession {

  private final Session session;
  /** @deprecated */
  private final JbpmSession jbpmSession;

  /** @deprecated use {@link #GraphSession(Session)} instead */
  public GraphSession(JbpmSession jbpmSession) {
    this.session = jbpmSession.getSession();
    this.jbpmSession = jbpmSession;
  }

  public GraphSession(Session session) {
    this.session = session;
    this.jbpmSession = null;
  }

  // process definitions //////////////////////////////////////////////////////

  /**
   * assigns a version number to the given process definition and then makes it persistent.
   */
  public void deployProcessDefinition(ProcessDefinition processDefinition) {
    // versioning applies to named process definitions only
    String processName = processDefinition.getName();
    if (processName == null) {
      throw new JbpmException("process definition has no name");
    }
    // find the current latest process definition
    ProcessDefinition previousLatestVersion = findLatestProcessDefinition(processName);
    // if there is a current latest process definition
    if (previousLatestVersion != null) {
      // take the next version number
      processDefinition.setVersion(previousLatestVersion.getVersion() + 1);
    }
    else {
      // start from 1
      processDefinition.setVersion(1);
    }
    saveProcessDefinition(processDefinition);
  }

  /**
   * saves the process definition. this method does not assign a version number.
   * 
   * @see #deployProcessDefinition(ProcessDefinition)
   */
  public void saveProcessDefinition(ProcessDefinition processDefinition) {
    try {
      session.save(processDefinition);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not save " + processDefinition, e);
    }
  }

  /**
   * returns the persistent process definition with the given identifier, assuming the
   * definition exists. if the requested process definition does not exist in the database,
   * {@link ObjectNotFoundException} is thrown when the definition state is first accessed.
   */
  public ProcessDefinition loadProcessDefinition(long processDefinitionId) {
    try {
      return (ProcessDefinition) session.load(ProcessDefinition.class,
        new Long(processDefinitionId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not load process definition "
        + processDefinitionId, e);
    }
  }

  /**
   * returns the persistent process definition with the given identifier.
   * 
   * @return the referenced process definition, or <code>null</code> if there is no such
   * definition.
   */
  public ProcessDefinition getProcessDefinition(long processDefinitionId) {
    try {
      return (ProcessDefinition) session.get(ProcessDefinition.class,
        new Long(processDefinitionId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not get process definition "
        + processDefinitionId, e);
    }
  }

  /**
   * finds the process definition with the given name and version.
   */
  public ProcessDefinition findProcessDefinition(String name, int version) {
    try {
      return (ProcessDefinition) session.getNamedQuery("GraphSession.findProcessDefinitionByNameAndVersion")
        .setString("name", name)
        .setInteger("version", version)
        .uniqueResult();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find process definition by name '" + name
        + "' and version " + version, e);
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
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find process definition by name " + name, e);
    }
  }

  /**
   * queries the database for the latest version of each process definition. process definitions
   * are distinct by name.
   */
  public List findLatestProcessDefinitions() {
    try {
      List tuples = session.getNamedQuery("GraphSession.findLatestProcessDefinitions").list();
      if (tuples.isEmpty()) return Collections.EMPTY_LIST;

      List result = new ArrayList(tuples.size());
      for (Iterator i = tuples.iterator(); i.hasNext();) {
        Object[] tuple = (Object[]) i.next();
        String name = (String) tuple[0];
        Integer version = (Integer) tuple[1];
        result.add(findProcessDefinition(name, version.intValue()));
      }
      return result;
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find latest process definitions", e);
    }
  }

  public List findProcessDefinitions(Collection processDefinitionIds) {
    try {
      return session.createCriteria(ProcessDefinition.class)
        .add(Restrictions.in("id", processDefinitionIds))
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find process definitions by identifiers "
        + processDefinitionIds, e);
    }
  }

  /**
   * queries the database for all process definitions, ordered by name (ascending), then by
   * version (descending).
   */
  public List findAllProcessDefinitions() {
    try {
      return session.getNamedQuery("GraphSession.findAllProcessDefinitions").list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find all process definitions", e);
    }
  }

  /**
   * queries the database for all versions of process definitions with the given name, ordered
   * by version (descending).
   */
  public List findAllProcessDefinitionVersions(String name) {
    try {
      return session.getNamedQuery("GraphSession.findAllProcessDefinitionVersions")
        .setString("name", name)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find all process definitions by name "
        + name, e);
    }
  }

  public void deleteProcessDefinition(long processDefinitionId) {
    deleteProcessDefinition(loadProcessDefinition(processDefinitionId));
  }

  public void deleteProcessDefinition(ProcessDefinition processDefinition) {
    try {
      // delete all instances of the given process
      List processInstances = session.createCriteria(ProcessInstance.class)
        .add(Restrictions.eq("processDefinition", processDefinition))
        .setProjection(Projections.id())
        .list();
      for (Iterator iter = processInstances.iterator(); iter.hasNext();) {
        Long id = (Long) iter.next();
        ProcessInstance processInstance = (ProcessInstance) session.get(ProcessInstance.class, id);
        if (processInstance != null) deleteProcessInstance(processInstance);
      }

      List referencingProcessStates = findReferencingProcessStates(processDefinition);
      for (Iterator i = referencingProcessStates.iterator(); i.hasNext();) {
        ProcessState processState = (ProcessState) i.next();
        processState.setSubProcessDefinition(null);
      }

      // then delete the process definition
      session.delete(processDefinition);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not delete " + processDefinition, e);
    }
  }

  private List findReferencingProcessStates(ProcessDefinition subProcessDefinition) {
    return session.getNamedQuery("GraphSession.findReferencingProcessStates")
      .setEntity("subProcessDefinition", subProcessDefinition)
      .list();
  }

  // process instances ////////////////////////////////////////////////////////

  /**
   * @deprecated use {@link org.jbpm.JbpmContext#save(ProcessInstance)} instead.
   */
  public void saveProcessInstance(ProcessInstance processInstance) {
    JbpmContext.getCurrentJbpmContext().save(processInstance);
  }

  /**
   * returns the persistent process instance with the given identifier, assuming the instance
   * exists. if the requested process instance does not exist in the database,
   * {@link ObjectNotFoundException} is thrown when the instance state is first accessed.
   */
  public ProcessInstance loadProcessInstance(long processInstanceId) {
    try {
      return (ProcessInstance) session.load(ProcessInstance.class, new Long(processInstanceId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not load process instance "
        + processInstanceId, e);
    }
  }

  /**
   * returns the persistent process instance with the given identifier.
   * 
   * @return the referenced process instance, or <code>null</code> if there is no such instance
   */
  public ProcessInstance getProcessInstance(long processInstanceId) {
    try {
      return (ProcessInstance) session.get(ProcessInstance.class, new Long(processInstanceId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not get process instance "
        + processInstanceId, e);
    }
  }

  /**
   * returns the persistent token with the given identifier, assuming the token exists. if the
   * requested token does not exist in the database, {@link ObjectNotFoundException} is thrown
   * when the token state is first accessed.
   */
  public Token loadToken(long tokenId) {
    try {
      return (Token) session.load(Token.class, new Long(tokenId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not load token " + tokenId, e);
    }
  }

  /**
   * returns the persistent token with the given identifier.
   * 
   * @return the referenced token, or <code>null</code> if there is no such token.
   */
  public Token getToken(long tokenId) {
    try {
      return (Token) session.get(Token.class, new Long(tokenId));
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not get token " + tokenId, e);
    }
  }

  /**
   * obtains a pessimistic lock on the process instance with the given identifier.
   */
  public void lockProcessInstance(long processInstanceId) {
    try {
      session.load(ProcessInstance.class, new Long(processInstanceId), LockMode.UPGRADE);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not lock process instance "
        + processInstanceId, e);
    }
  }

  /**
   * obtains a pessimistic lock on the given process instance.
   */
  public void lockProcessInstance(ProcessInstance processInstance) {
    try {
      session.lock(processInstance, LockMode.UPGRADE);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not lock " + processInstance, e);
    }
  }

  /**
   * finds all instances of the given process definition.
   * 
   * @return a list of process instances ordered by start date, earliest first
   */
  public List findProcessInstances(long processDefinitionId) {
    try {
      return session.getNamedQuery("GraphSession.findAllProcessInstancesForDefinition")
        .setLong("processDefinitionId", processDefinitionId)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find instances for process definition "
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
      if (includeJobs) deleteJobs(processInstance);

      // delete logs
      deleteLogs(processInstance);

      // detach from superprocess token
      Token superProcessToken = processInstance.getSuperProcessToken();
      if (superProcessToken != null)
        detachFromSuperProcess(processInstance, superProcessToken);

      // delete subprocess instances
      deleteSubProcesses(processInstance);

      // delete tasks; since TaskLogs reference tasks, logs are deleted first
      if (includeTasks) deleteTasks(processInstance);

      // delete the process instance
      session.delete(processInstance);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not delete " + processInstance, e);
    }
  }

  private void deleteJobs(ProcessInstance processInstance) {
    session.getNamedQuery("GraphSession.deleteJobsForProcessInstance")
      .setEntity("processInstance", processInstance)
      .executeUpdate();
  }

  private void deleteLogs(ProcessInstance processInstance) {
    List logs = session.getNamedQuery("GraphSession.findLogsForProcessInstance")
      .setEntity("processInstance", processInstance)
      .list();
    for (Iterator i = logs.iterator(); i.hasNext();) {
      session.delete(i.next());
    }
  }

  private void detachFromSuperProcess(ProcessInstance processInstance, Token superProcessToken) {
    processInstance.setSuperProcessToken(null);
    superProcessToken.setSubProcessInstance(null);
  }

  private void deleteSubProcesses(ProcessInstance processInstance) {
    List subProcessInstances = session.getNamedQuery("GraphSession.findSubProcessInstances")
      .setEntity("processInstance", processInstance)
      .list();

    for (Iterator i = subProcessInstances.iterator(); i.hasNext();) {
      ProcessInstance subProcessInstance = (ProcessInstance) i.next();
      deleteProcessInstance(subProcessInstance);
    }
  }

  private void deleteTasks(ProcessInstance processInstance) {
    List tasks = session.getNamedQuery("GraphSession.findTaskInstancesForProcessInstance")
      .setEntity("processInstance", processInstance)
      .list();
    for (Iterator i = tasks.iterator(); i.hasNext();) {
      session.delete(i.next());
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

  public List calculateAverageTimeByNode(long processDefinitionId, long minumumDurationMillis) {
    try {
      List tuples = session.getNamedQuery("GraphSession.calculateAverageTimeByNode")
        .setLong("processDefinitionId", processDefinitionId)
        .setDouble("minimumDuration", minumumDurationMillis)
        .list();
      if (tuples.isEmpty()) return Collections.EMPTY_LIST;

      List results = new ArrayList();
      for (Iterator i = tuples.iterator(); i.hasNext();) {
        Object[] values = (Object[]) i.next();

        AverageNodeTimeEntry entry = new AverageNodeTimeEntry();
        entry.setNodeId(((Number) values[0]).longValue());
        entry.setNodeName((String) values[1]);
        entry.setCount(((Number) values[2]).intValue());
        entry.setAverageDuration(((Number) values[3]).longValue());
        entry.setMinDuration(((Number) values[4]).longValue());
        entry.setMaxDuration(((Number) values[5]).longValue());

        results.add(entry);
      }
      return results;
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not calculate average time by node "
        + "for process definition " + processDefinitionId, e);
    }
  }

  public List findActiveNodesByProcessInstance(ProcessInstance processInstance) {
    try {
      return session.getNamedQuery("GraphSession.findActiveNodesByProcessInstance")
        .setEntity("processInstance", processInstance)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not find active nodes for "
        + processInstance, e);
    }
  }

  /**
   * returns the instance of the given process definition with the specified business key.
   * 
   * @return the referenced instance, or <code>null</code> if there is no such instance
   */
  public ProcessInstance getProcessInstance(ProcessDefinition processDefinition, String key) {
    try {
      return (ProcessInstance) session.getNamedQuery("GraphSession.findProcessInstanceByKey")
        .setEntity("processDefinition", processDefinition)
        .setString("key", key)
        .uniqueResult();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("could not get process instance by key " + key, e);
    }
  }

  /**
   * returns the instance of the given process definition with the specified business key,
   * assuming the instance exists.
   * 
   * @throws JbpmPersistenceException if the referenced process instance does not exist
   */
  public ProcessInstance loadProcessInstance(ProcessDefinition processDefinition, String key) {
    ProcessInstance processInstance = getProcessInstance(processDefinition, key);
    if (processInstance == null) {
      throw new JbpmPersistenceException("could not load process instance by key " + key);
    }
    return processInstance;
  }

  private void handle(HibernateException exception) {
    // exception will be rethrown, no need to log here
    if (jbpmSession != null) jbpmSession.handleException();
  }
}
