package org.jbpm.job.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.JobSession;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;

public class JobExecutorThread extends Thread {

  final JobExecutor jobExecutor;
  final JbpmConfiguration jbpmConfiguration;
  final int idleInterval;
  final int maxIdleInterval;
  final long maxLockTime;

  int currentIdleInterval;
  volatile boolean isActive = true;

  public JobExecutorThread(String name, JobExecutor jobExecutor) {
    super(name);
    this.jobExecutor = jobExecutor;
    this.jbpmConfiguration = jobExecutor.getJbpmConfiguration();
    this.idleInterval = jobExecutor.getIdleInterval();
    this.maxIdleInterval = jobExecutor.getMaxIdleInterval();
    this.maxLockTime = jobExecutor.getMaxLockTime();
  }

  /** @deprecated As of jBPM 3.2.6, replaced by {@link #JobExecutorThread(String, JobExecutor)} */
  @Deprecated
  public JobExecutorThread(String name, JobExecutor jobExecutor,
      JbpmConfiguration jbpmConfiguration, int idleInterval, int maxIdleInterval, long maxLockTime,
      int maxHistory) {
    super(name);
    this.jobExecutor = jobExecutor;
    this.jbpmConfiguration = jbpmConfiguration;
    this.idleInterval = idleInterval;
    this.maxIdleInterval = maxIdleInterval;
    this.maxLockTime = maxLockTime;
  }

  public void run() {
    currentIdleInterval = idleInterval;
    while (isActive) {
      try {
        Collection<Job> acquiredJobs = acquireJobs();

        if (!acquiredJobs.isEmpty()) {
          for (Job job : acquiredJobs) {
            executeJob(job);
            if (!isActive) break;
          }
        }
        else if (isActive) {
          long waitPeriod = getWaitPeriod();
          if (waitPeriod > 0) {
            synchronized (jobExecutor) {
              jobExecutor.wait(waitPeriod);
            }
          }
        }

        // no exception, resetting the currentIdleInterval
        currentIdleInterval = idleInterval;
      }
      catch (InterruptedException e) {
        log.info((isActive ? "active" : "inactive")
            + " job executor thread '"
            + getName()
            + "' got interrupted");
      }
      catch (Exception e) {
        log.error("exception in job executor thread. waiting "
            + currentIdleInterval
            + " milliseconds", e);
        try {
          synchronized (jobExecutor) {
            jobExecutor.wait(currentIdleInterval);
          }
        }
        catch (InterruptedException e2) {
          log.debug("delay after exception got interrupted", e2);
        }
        // after an exception, the current idle interval is doubled to prevent
        // continuous exception generation when e.g. the db is unreachable
        currentIdleInterval *= 2;
        if (currentIdleInterval > maxIdleInterval || currentIdleInterval < 0) {
          currentIdleInterval = maxIdleInterval;
        }
      }
    }
    log.info(getName() + " leaves cyberspace");
  }

  protected Collection<Job> acquireJobs() {
    Collection<Job> acquiredJobs = Collections.emptyList();
    synchronized (jobExecutor) {
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        log.debug("querying for acquirable job...");
        String lockOwner = getName();
        JobSession jobSession = jbpmContext.getJobSession();
        Job job = jobSession.getFirstAcquirableJob(lockOwner);

        if (job != null) {
          if (job.isExclusive()) {
            ProcessInstance processInstance = job.getProcessInstance();
            log.debug("loaded exclusive " + job + ", finding exclusive jobs for " + processInstance);
            acquiredJobs = jobSession.findExclusiveJobs(lockOwner, processInstance);
            log.debug("trying to obtain locks on " + acquiredJobs + " for " + processInstance);
          }
          else {
            acquiredJobs = Collections.singletonList(job);
            log.debug("trying to obtain lock on " + job);
          }

          Date lockTime = new Date();
          for (Job acquiredJob : acquiredJobs) {
            acquiredJob.setLockOwner(lockOwner);
            acquiredJob.setLockTime(lockTime);
          }
        }
        else {
          log.debug("no acquirable jobs");
        }
      }
      finally {
        try {
          jbpmContext.close();
          log.debug("obtained lock on jobs: " + acquiredJobs);
        }
        catch (JbpmPersistenceException e) {
          // if this is a stale state exception, keep it quiet
          if (DbPersistenceService.isStaleStateException(e)) {
            log.debug("optimistic locking failed, could not acquire jobs " + acquiredJobs);
            StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error(
                "optimistic locking failed, could not acquire jobs " + acquiredJobs, e);
            acquiredJobs = Collections.emptyList();
          }
          else {
            throw e;
          }
        }
      }
    }
    return acquiredJobs;
  }

  protected void executeJob(Job job) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      JobSession jobSession = jbpmContext.getJobSession();
      job = jobSession.loadJob(job.getId());

      log.debug("executing " + job);
      try {
        if (job.execute(jbpmContext)) {
          jobSession.deleteJob(job);
        }
      }
      catch (Exception e) {
        log.debug("exception while executing " + job, e);
        if (!DbPersistenceService.isPersistenceException(e)) {
          StringWriter memoryWriter = new StringWriter();
          e.printStackTrace(new PrintWriter(memoryWriter));
          job.setException(memoryWriter.toString());
          job.setRetries(job.getRetries() - 1);
        }
        else {
          // allowing a transaction to proceed after a persistence exception is unsafe
          jbpmContext.setRollbackOnly();
        }
      }

      // if this job is locked too long
      long totalLockTimeInMillis = System.currentTimeMillis() - job.getLockTime().getTime();
      if (totalLockTimeInMillis > maxLockTime) {
        jbpmContext.setRollbackOnly();
      }
    }
    finally {
      try {
        jbpmContext.close();
      }
      catch (JbpmPersistenceException e) {
        // if this is a stale state exception, keep it quiet
        if (DbPersistenceService.isStaleStateException(e)) {
          log.debug("optimistic locking failed, could not complete job " + job);
          StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error(
              "optimistic locking failed, could not complete job " + job, e);
        }
        else {
          throw e;
        }
      }
    }
  }

  protected Date getNextDueDate() {
    Date nextDueDate = null;
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      JobSession jobSession = jbpmContext.getJobSession();
      Collection<Long> jobIdsToIgnore = jobExecutor.getMonitoredJobIds();
      Job job = jobSession.getFirstDueJob(getName(), jobIdsToIgnore);
      if (job != null) {
        nextDueDate = job.getDueDate();
        jobExecutor.addMonitoredJobId(getName(), job.getId());
      }
    }
    finally {
      try {
        jbpmContext.close();
      }
      catch (JbpmPersistenceException e) {
        // if this is a stale state exception, keep it quiet
        if (DbPersistenceService.isStaleStateException(e)) {
          log.debug("optimistic locking failed, could not return next due date: " + nextDueDate);
          StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error(
              "optimistic locking failed, could not return next due date: " + nextDueDate, e);
          nextDueDate = null;
        }
        else {
          throw e;
        }
      }
    }
    return nextDueDate;
  }

  protected long getWaitPeriod() {
    long interval = currentIdleInterval;
    Date nextDueDate = getNextDueDate();
    if (nextDueDate != null) {
      long currentTime = System.currentTimeMillis();
      long nextDueTime = nextDueDate.getTime();
      if (nextDueTime < currentTime + currentIdleInterval) {
        interval = nextDueTime - currentTime;
      }
    }
    if (interval < 0) {
      interval = 0;
    }
    return interval;
  }

  /**
   * @deprecated As of jBPM 3.2.3, replaced by {@link #deactivate()}
   */
  public void setActive(boolean isActive) {
    if (isActive == false) deactivate();
  }

  /**
   * Indicates that this thread should stop running. Execution will cease shortly afterwards.
   */
  public void deactivate() {
    if (isActive) {
      isActive = false;
      interrupt();
    }
  }

  private static Log log = LogFactory.getLog(JobExecutorThread.class);
}
