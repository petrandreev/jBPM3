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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskInstance;

public abstract class AbstractDbTestCase extends AbstractJbpmTestCase {

  protected JbpmConfiguration jbpmConfiguration;
  protected JbpmContext jbpmContext;

  protected Session session;
  protected GraphSession graphSession;
  protected TaskMgmtSession taskMgmtSession;
  protected ContextSession contextSession;
  protected JobSession jobSession;
  protected LoggingSession loggingSession;

  protected JobExecutor jobExecutor;

  private List processDefinitionIds;

  private static final long JOB_TIMEOUT = 90 * 1000;

  protected void setUp() throws Exception {
    super.setUp();
    createJbpmContext();
  }

  protected void runTest() throws Throwable {
    try {
      super.runTest();
    }
    catch (Exception e) {
      // prevent unsafe use of the session after an exception occurs
      if (!jbpmContext.isClosed()) jbpmContext.setRollbackOnly();
      throw e;
    }
  }

  protected void tearDown() throws Exception {
    if (processDefinitionIds != null) deleteProcessDefinitions();
    closeJbpmContext();
    ensureCleanDatabase();

    super.tearDown();
  }

  protected void deleteProcessDefinitions() {
    if( processDefinitionIds == null ) return;
    for (Iterator i = processDefinitionIds.iterator(); i.hasNext();) {
      newTransaction();
      try {
        Long processDefinitionId = (Long) i.next();
        graphSession.deleteProcessDefinition(processDefinitionId.longValue());
      }
      catch (RuntimeException e) {
        jbpmContext.setRollbackOnly();
      }
    }
  }

  protected void ensureCleanDatabase() {
    DbPersistenceServiceFactory persistenceServiceFactory =
      (DbPersistenceServiceFactory) getJbpmConfiguration().getServiceFactory("persistence");
    if (persistenceServiceFactory == null) return;

    boolean hasLeftOvers = false;
    Configuration configuration = persistenceServiceFactory.getConfiguration();
    JbpmSchema jbpmSchema = new JbpmSchema(configuration);

    for (Iterator i = jbpmSchema.getRowsPerTable().entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) i.next();
      Long count = (Long) entry.getValue();
      if (count.intValue() != 0) {
        hasLeftOvers = true;
        log.error(getName() + " left " + count + " records in " + entry.getKey());
      }
    }

    if (hasLeftOvers) {
      jbpmSchema.cleanSchema();
    }
  }

  protected String getHibernateDialect() {
    DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
    return persistenceServiceFactory.getConfiguration().getProperty(Environment.DIALECT);
  }

  /** @deprecated call {@link #createJbpmContext()} instead */
  protected void beginSessionTransaction() {
    createJbpmContext();
  }

  /** @deprecated call {@link #closeJbpmContext()} instead */
  protected void commitAndCloseSession() {
    closeJbpmContext();
  }

  protected void newTransaction() {
    closeJbpmContext();
    createJbpmContext();
  }

  protected ProcessInstance saveAndReload(ProcessInstance pi) {
    jbpmContext.save(pi);
    newTransaction();
    return graphSession.loadProcessInstance(pi.getId());
  }

  protected TaskInstance saveAndReload(TaskInstance taskInstance) {
    jbpmContext.save(taskInstance);
    newTransaction();
    return (TaskInstance) session.load(TaskInstance.class, new Long(taskInstance.getId()));
  }

  protected ProcessDefinition saveAndReload(ProcessDefinition pd) {
    graphSession.saveProcessDefinition(pd);
    registerForDeletion(pd);
    return graphSession.loadProcessDefinition(pd.getId());
  }

  protected ProcessLog saveAndReload(ProcessLog processLog) {
    loggingSession.saveProcessLog(processLog);
    newTransaction();
    return loggingSession.loadProcessLog(processLog.getId());
  }

  protected void createSchema() {
    getJbpmConfiguration().createSchema();
  }

  protected void cleanSchema() {
    getJbpmConfiguration().cleanSchema();
  }

  protected void dropSchema() {
    getJbpmConfiguration().dropSchema();
  }

  protected String getJbpmTestConfig() {
    return null;
  }

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      String configurationResource = getJbpmTestConfig();
      jbpmConfiguration = JbpmConfiguration.getInstance(configurationResource);
    }
    return jbpmConfiguration;
  }

  protected void createJbpmContext() {
    jbpmContext = getJbpmConfiguration().createJbpmContext();
    initializeMembers();
  }

  protected void closeJbpmContext() {
    if (jbpmContext != null) {
      resetMembers();

      jbpmContext.close();
      jbpmContext = null;
    }
  }

  protected void startJobExecutor() {
    jobExecutor = getJbpmConfiguration().getJobExecutor();
    jobExecutor.start();
  }

  /**
   * Waits until all jobs are processed or a specified amount of time has elapsed. Unlike
   * {@link #processJobs(long)}, this method is not concerned about the job executor or
   * the jBPM context.
   */
  protected void waitForJobs(final long timeout) {
    final long startTime = System.currentTimeMillis();
    long previousTime = 0;
    long waitPeriod = 500;

    for (int currentCount, previousCount = 0; (currentCount = getNbrOfJobsAvailable()) > 0;) {
      long currentTime = System.currentTimeMillis();

      long elapsedTime = currentTime - startTime;
      if (elapsedTime > timeout) {
        fail("test execution exceeded threshold of " + timeout + " ms");
      }

      if (currentCount < previousCount) {
        waitPeriod = currentCount * (currentTime - previousTime)
          / (previousCount - currentCount);
        if (waitPeriod < 500) waitPeriod = 500;
      }
      else {
        waitPeriod <<= 1;
      }

      if (waitPeriod > 5000) {
        waitPeriod = 5000;
      }
      else {
        long remainingTime = timeout - elapsedTime;
        if (waitPeriod > remainingTime) waitPeriod = remainingTime;
      }

      if (log.isDebugEnabled()) {
        log.debug("waiting " + waitPeriod + " ms for " + currentCount + " jobs");
      }
      try {
        Thread.sleep(waitPeriod);
      }
      catch (InterruptedException e) {
        fail("wait for jobs got interrupted");
      }

      previousCount = currentCount;
      previousTime = currentTime;
    }
  }

  protected int getNbrOfJobsAvailable() {
    if (session != null) {
      return getJobCount(session);
    }
    else {
      createJbpmContext();
      try {
        return getJobCount(session);
      }
      finally {
        closeJbpmContext();
      }
    }
  }

  private int getJobCount(Session session) {
    Number jobCount = (Number) session.createCriteria(Job.class)
      .add(Restrictions.gt("retries", new Integer(0)))
      .setProjection(Projections.rowCount())
      .uniqueResult();
    return jobCount.intValue();
  }

  protected int getTimerCount() {
    Number timerCount = (Number) session.createCriteria(Timer.class)
      .add(Restrictions.gt("retries", new Integer(0)))
      .setProjection(Projections.rowCount())
      .uniqueResult();
    return timerCount.intValue();
  }

  /**
   * Starts the job executor and waits until all jobs are processed or a predefined amount of
   * time has elapsed. The current jBPM context is closed before waiting and a new one is opened
   * after processing the jobs.
   */
  protected void processJobs() {
    processJobs(JOB_TIMEOUT);
  }

  /**
   * Starts the job executor and waits until all jobs are processed or a specified amount of
   * time has elapsed. The current jBPM context is closed before waiting and a new one is opened
   * after processing the jobs.
   */
  protected void processJobs(long timeout) {
    closeJbpmContext();
    try {
      startJobExecutor();
      waitForJobs(timeout);
    }
    finally {
      stopJobExecutor();
      createJbpmContext();
    }
  }

  protected void stopJobExecutor() {
    if (jobExecutor != null) {
      try {
        jobExecutor.stopAndJoin();
      }
      catch (InterruptedException e) {
        fail("wait for job executor to stop got interrupted");
      }
      finally {
        jobExecutor = null;
      }
    }
  }

  protected void deployProcessDefinition(ProcessDefinition processDefinition) {
    jbpmContext.deployProcessDefinition(processDefinition);
    registerForDeletion(processDefinition);
  }

  private void registerForDeletion(ProcessDefinition processDefinition) {
    // start new transaction to avoid registering an uncommitted process definition
    newTransaction();
    if (processDefinitionIds == null) processDefinitionIds = new ArrayList();
    processDefinitionIds.add(new Long(processDefinition.getId()));
  }

  protected void initializeMembers() {
    session = jbpmContext.getSession();
    graphSession = jbpmContext.getGraphSession();
    taskMgmtSession = jbpmContext.getTaskMgmtSession();
    loggingSession = jbpmContext.getLoggingSession();
    jobSession = jbpmContext.getJobSession();
    contextSession = jbpmContext.getContextSession();
  }

  protected void resetMembers() {
    session = null;
    graphSession = null;
    taskMgmtSession = null;
    loggingSession = null;
    jobSession = null;
    contextSession = null;
  }
}
