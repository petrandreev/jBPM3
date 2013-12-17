package org.jbpm.job.executor;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.job.Job;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;

public class LockMonitorThread extends Thread implements Deactivable {

  public static final String DEFAULT_NAME = "Monitor";

  private final JobExecutor jobExecutor;
  private volatile boolean active = true;

  public LockMonitorThread(JobExecutor jobExecutor) {
    this(DEFAULT_NAME, jobExecutor);
  }

  public LockMonitorThread(String name, JobExecutor jobExecutor) {
    super(jobExecutor.getThreadGroup(), name);
    this.jobExecutor = jobExecutor;
  }

  /**
   * @deprecated As of jBPM 3.2.6, replaced by {@link #LockMonitorThread(JobExecutor)}
   */
  public LockMonitorThread(JbpmConfiguration jbpmConfiguration, int lockMonitorInterval,
    int maxLockTime, int lockBufferTime) {
    jobExecutor = jbpmConfiguration.getJobExecutor();
  }

  public void run() {
    while (active) {
      try {
        unlockOverdueJobs();
      }
      catch (RuntimeException e) {
        log.error("exception in " + getName(), e);
      }
      if (active) {
        try {
          sleep(jobExecutor.getLockMonitorInterval());
        }
        catch (InterruptedException e) {
          if (log.isDebugEnabled()) log.debug(getName() + " got interrupted");
        }
      }
    }
    log.info(getName() + " leaves cyberspace");
  }

  protected void unlockOverdueJobs() {
    JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
    try {
      Date threshold = new Date(System.currentTimeMillis() - jobExecutor.getMaxLockTime());
      List overdueJobs = jbpmContext.getJobSession().findJobsWithOverdueLockTime(threshold);
      for (Iterator i = overdueJobs.iterator(); i.hasNext();) {
        Job job = (Job) i.next();
        if (log.isDebugEnabled()) log.debug("unlocking " + job);
        job.setLockOwner(null);
        job.setLockTime(null);
      }
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      if (!DbPersistenceService.isLockingException(e)) throw e;
      // keep locking exception quiet
      StaleObjectLogConfigurer.getStaleObjectExceptionsLog()
        .error("could not unlock overdue jobs", e);
    }
    catch (Error e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    finally {
      try {
        jbpmContext.close();
      }
      catch (RuntimeException e) {
        if (!DbPersistenceService.isLockingException(e)) throw e;
        // keep locking exception quiet
        StaleObjectLogConfigurer.getStaleObjectExceptionsLog()
          .error("could not unlock overdue jobs", e);
      }
    }
  }

  /**
   * @deprecated As of jBPM 3.2.3, replaced by {@link #deactivate()}
   */
  public void setActive(boolean isActive) {
    if (isActive == false) deactivate();
  }

  public void deactivate() {
    if (active) {
      active = false;
      interrupt();
    }
  }

  private static Log log = LogFactory.getLog(LockMonitorThread.class);
}
