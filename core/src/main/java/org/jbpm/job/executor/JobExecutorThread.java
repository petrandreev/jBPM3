package org.jbpm.job.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.JobSession;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;

public class JobExecutorThread extends Thread implements Deactivable {

  private final JobExecutor jobExecutor;
  private volatile boolean active = true;
  private Random random = new Random() ;

  public JobExecutorThread(String name, JobExecutor jobExecutor) {
    super(jobExecutor.getThreadGroup(), name);
    this.jobExecutor = jobExecutor;
  }

  public void run() {
    while (active) {
      // take on next job
      Job job = jobExecutor.getJob();
      // if an exception occurs, acquireJob() returns null
      if (job != null) {
        try {
          executeJob(job);
        }
        catch (Exception e) {
          // save exception stack trace
          // if another exception occurs, it is not rethrown
          saveJobException(job, e);
        }
        catch (Error e) {
          // unlock job so it can be dispatched again
          // if another exception occurs, it is not rethrown
          unlockJob(job);
          throw e;
        }
      }
    }
    log.info(getName() + " leaves cyberspace");
  }

  protected void executeJob(Job job) throws Exception {
    JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
    try {
      // reattach job to persistence context
      JobSession jobSession = jbpmContext.getJobSession();
      jobSession.reattachJob(job);
      
      // register process instance for automatic save
      // https://jira.jboss.org/browse/JBPM-1015
      ProcessInstance processInstance = job.getProcessInstance();
      jbpmContext.addAutoSaveProcessInstance(processInstance);

      // if job is exclusive, lock process instance
      if (job.isExclusive()) {
        jbpmContext.getGraphSession().lockProcessInstance(processInstance);
      }

      if (log.isDebugEnabled()) log.debug("executing " + job);
      if (job.execute(jbpmContext)) jobSession.deleteJob(job);
    }
    catch (Exception e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    catch (Error e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    finally {
      jbpmContext.close();
    }
  }

  private void saveJobException(Job job, Exception exception) {
    // if this is a locking exception, keep it quiet
    if (DbPersistenceService.isLockingException(exception)) {
      StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error("failed to execute " + job, exception);
    }
    else {
      log.error("failed to execute " + job, exception);
    }

    JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
    try {
      // do not reattach existing job as it contains undesired updates
      jbpmContext.getSession().refresh(job);

      // print and save exception
      StringWriter out = new StringWriter();
      exception.printStackTrace(new PrintWriter(out));
      job.setException(out.toString());

      // unlock job so it can be dispatched again
      job.setLockOwner(null);
      job.setLockTime(null);
      int waitPeriod = jobExecutor.getRetryInterval() / 2;
      waitPeriod += random.nextInt(waitPeriod) ;
      job.setDueDate(new Date(System.currentTimeMillis() + waitPeriod)) ;
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      log.warn("failed to save exception for " + job, e);
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
        log.warn("failed to save exception for " + job, e);
      }
    }
    // notify job executor
    synchronized (jobExecutor) {
      jobExecutor.notify();
    }
  }

  private void unlockJob(Job job) {
    JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
    try {
      // do not reattach existing job as it contains undesired updates
      jbpmContext.getSession().refresh(job);

      // unlock job
      job.setLockOwner(null);
      job.setLockTime(null);
      if (job.getException() != null)
      {
    	  job.setRetries(job.getRetries()+1) ;
      }
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      log.warn("failed to unlock " + job, e);
    }
    catch (Error e) {
      jbpmContext.setRollbackOnly();
      // do not rethrow as this method is already called in response to an Error
      log.warn("failed to unlock " + job, e);
    }
    finally {
      try {
        jbpmContext.close();
      }
      catch (RuntimeException e) {
        log.warn("failed to unlock " + job, e);
      }
    }
    // notify job executor
    synchronized (jobExecutor) {
      jobExecutor.notify();
    }
  }

  public void deactivate() {
    if (active) {
      active = false;
      interrupt();
    }
  }

  private static final Log log = LogFactory.getLog(JobExecutorThread.class);
  
  /**
   * ====================
   * CRUFT! CRUFT! CRUFT!
   * ====================
   */
  
  /**
   * @deprecated use {@link #JobExecutorThread(String, JobExecutor)} instead
   */
  public JobExecutorThread(String name, JobExecutor jobExecutor,
    JbpmConfiguration jbpmConfiguration, int idleInterval, int maxIdleInterval,
    long maxLockTime, int maxHistory) {
    super(jobExecutor.getThreadGroup(), name);
    this.jobExecutor = jobExecutor;
  }
 
  /**
   * @deprecated As of jBPM 3.2.3, replaced by {@link #deactivate()}
   */
  public void setActive(boolean isActive) {
    if (isActive == false) deactivate();
  }

}
