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
package org.jbpm.job.executor;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.job.Job;

/**
 * Acquires jobs and then dispatches them to the job executor thread pool.
 * 
 */
public class DispatcherThread extends Thread implements Deactivable {

  private final JobExecutor jobExecutor;
  private volatile boolean active = true;

  static final String DEFAULT_NAME = "Dispatcher";
  private static final Log log = LogFactory.getLog(DispatcherThread.class);

  public DispatcherThread(JobExecutor jobExecutor) {
    this(DEFAULT_NAME, jobExecutor);
  }

  public DispatcherThread(String name, JobExecutor jobExecutor) {
    super(jobExecutor.getThreadGroup(), name);
    this.jobExecutor = jobExecutor;
  }

  public void run() {
    while (active) {
      if (jobExecutor.waitForFreeExecutorThread()) {
        // acquire job; on exception, call returns null
        Job job = acquireJob();
        // submit job
        if (job != null) {
          submitJob(job);
          continue ;
        }
      }

      // if still active, wait or sleep
      if (active) {
        try {
          int currentIdleInterval = jobExecutor.getIdleInterval();
          // wait for next due job
          Date nextDueDate = getNextDueDateWithinIdleInterval(currentIdleInterval);
          long waitPeriod = getWaitPeriod(currentIdleInterval, nextDueDate);
          if (waitPeriod > 0) {
            synchronized (jobExecutor) {
              if (active) {
                jobExecutor.wait(waitPeriod);
              }
            }
          }
        }
        catch (InterruptedException e) {
          if (log.isDebugEnabled()) log.debug(getName() + " got interrupted");
        }
      }
      
    } // while(active)
    
    log.info(getName() + " leaves cyberspace");
  }

  private Job acquireJob() {
    Job job = null;
    boolean debug = log.isDebugEnabled();
    // acquire job executor's monitor before creating context and allocating resources
    synchronized (jobExecutor) {
      JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
      try {
        // look for available job
        Job firstJob = jbpmContext.getJobSession().getFirstAcquirableJob(null);
        // is there a job?
        if (firstJob != null) {
          // lock job
          firstJob.setLockOwner(getName());
          firstJob.setLockTime(new Date());
          // has job failed previously?
          if (firstJob.getException() != null) {
            // decrease retry count
            int retries = firstJob.getRetries() - 1;
            firstJob.setRetries(retries);
            if (debug) log.debug(firstJob + " has " + retries + " retries remaining");
          }
          // deliver result
          if (debug) log.debug("acquired " + firstJob);
          job = firstJob;
        }
        else if (debug) log.debug("no acquirable job found");
      }
      catch (RuntimeException e) {
        jbpmContext.setRollbackOnly();
        if (debug) log.debug("failed to acquire job", e);
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
          job = null;
          if (debug) log.debug("failed to acquire job", e);
        }
      }
    }
    return job;
  }

  private void submitJob(Job job) {
    if (!jobExecutor.submitJob(job)) {
      unlockJob(job);
    }
  }

  private void unlockJob(Job job) {
    JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
    try {
      // reattach job to persistence context
      jbpmContext.getJobSession().reattachJob(job);

      // unlock job so it can be dispatched again
      job.setLockOwner(null);
      job.setLockTime(null);
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      log.warn("failed to unlock " + job, e);
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
        log.warn("failed to unlock " + job, e);
      }
    }
  }

  static long getWaitPeriod(int currentIdleInterval, Date nextDueDate) {
    // Have we found a job that should be called before now + current idle interval? 
    if (nextDueDate != null) {
      long waitPeriod = nextDueDate.getTime() - System.currentTimeMillis();
      
      // next due date > now
      if (waitPeriod < currentIdleInterval) { 
        return waitPeriod;
      }
    }
    return currentIdleInterval;
  }

  private Date getNextDueDateWithinIdleInterval(int currentIdleInterval) { 
    Date nextDueDate = null;
    JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
    try {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.MILLISECOND, currentIdleInterval);
      Date wakeUpDate = cal.getTime();
      
      nextDueDate = jbpmContext.getJobSession().getNextUnownedDueJobDueDate(wakeUpDate);
      if (log.isDebugEnabled() && nextDueDate == null) { 
        log.debug("no next waiting due job found before wake up date: possible future jobs after sleep");
      }
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      if (log.isDebugEnabled()) log.debug("failed to retrieve next waiting due job", e);
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
        nextDueDate = null;
        if (log.isDebugEnabled()) log.debug("failed to retrieve next waiting due job", e);
      }
    }
    return nextDueDate;
  }

  public void deactivate() {
    if (active) {
      active = false;
      interrupt();
    }
  }
}
