package org.jbpm.job.executor;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.JobSession;
import org.jbpm.job.Job;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;

public class LockMonitorThread extends Thread {

  JbpmConfiguration jbpmConfiguration;
  int lockMonitorInterval;
  int maxLockTime;
  int lockBufferTime;

  volatile boolean isActive = true;

  public LockMonitorThread(JobExecutor jobExecutor) {
    jbpmConfiguration = jobExecutor.getJbpmConfiguration();
    lockMonitorInterval = jobExecutor.getLockMonitorInterval();
    maxLockTime = jobExecutor.getMaxLockTime();
    lockBufferTime = jobExecutor.getLockBufferTime();
  }

  /** @deprecated As of jBPM 3.2.6, replaced by {@link #LockMonitorThread(JobExecutor)} */
  @Deprecated
  public LockMonitorThread(JbpmConfiguration jbpmConfiguration, int lockMonitorInterval,
      int maxLockTime, int lockBufferTime) {
    this.jbpmConfiguration = jbpmConfiguration;
    this.lockMonitorInterval = lockMonitorInterval;
    this.maxLockTime = maxLockTime;
    this.lockBufferTime = lockBufferTime;
  }

  public void run() {
    try {
      while (isActive) {
        try {
          unlockOverdueJobs();
          if (isActive && lockMonitorInterval > 0) {
            sleep(lockMonitorInterval);
          }
        }
        catch (InterruptedException e) {
          log.info("lock monitor thread '" + getName() + "' got interrupted");
        }
        catch (Exception e) {
          log.error("exception in lock monitor thread. waiting "
              + lockMonitorInterval
              + " milliseconds", e);
          try {
            sleep(lockMonitorInterval);
          }
          catch (InterruptedException e2) {
            log.debug("delay after exception got interrupted", e2);
          }
        }
      }
    }
    catch (Exception e) {
      log.error("exception in lock monitor thread", e);
    }
    finally {
      log.info(getName() + " leaves cyberspace");
    }
  }

  protected void unlockOverdueJobs() {
    List<Job> overdueJobs = null;
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      Date threshold = new Date(System.currentTimeMillis() - maxLockTime - lockBufferTime);
      JobSession jobSession = jbpmContext.getJobSession();
      overdueJobs = jobSession.findJobsWithOverdueLockTime(threshold);
      for (Job job : overdueJobs) {
        log.debug("unlocking " + job + " owned by thread " + job.getLockOwner());
        job.setLockOwner(null);
        job.setLockTime(null);
      }
    }
    finally {
      try {
        jbpmContext.close();
      }
      catch (JbpmPersistenceException e) {
        // if this is a stale state exception, keep it quiet
        if (DbPersistenceService.isStaleStateException(e)) {
          log.debug("optimistic locking failed, could not unlock overdue jobs: " + overdueJobs);
          StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error(
              "problem unlocking overdue jobs: optimistic locking failed", e);
        }
        else {
          throw e;
        }
      }
    }
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

  private static Log log = LogFactory.getLog(LockMonitorThread.class);
}
