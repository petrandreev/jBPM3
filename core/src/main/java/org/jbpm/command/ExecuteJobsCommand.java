package org.jbpm.command;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.db.JobSession;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;

/**
 * Execute all overdue Jobs (may be enhanced with more attributes in future if needed)
 * 
 * @author ruecker
 */
public class ExecuteJobsCommand extends AbstractBaseCommand {

  private static final long serialVersionUID = -2457066688404533959L;

  private static final Log log = LogFactory.getLog(ExecuteJobsCommand.class);

  private transient JbpmContext jbpmContext;

  public Object execute(JbpmContext jbpmContext) throws Exception {
    this.jbpmContext = jbpmContext;
    try {
      // acquire jobs; on exception, call returns empty collection
      Collection acquiredJobs = acquireJobs();
      // execute jobs
      for (Iterator i = acquiredJobs.iterator(); i.hasNext();) {
        Job job = (Job) i.next();
        executeJob(job);
      }
    }
    finally {
      this.jbpmContext = null;
    }
    return null;
  }

  private String getName() {
    return this.toString();
  }

  protected Collection acquireJobs() {
    boolean debug = log.isDebugEnabled();
    Collection jobs;
    try {
      // search for acquirable job
      String lockOwner = getName();
      JobSession jobSession = jbpmContext.getJobSession();
      Job firstJob = jobSession.getFirstAcquirableJob(lockOwner);
      // is there a job?
      if (firstJob != null) {
        // is job exclusive?
        if (firstJob.isExclusive()) {
          // find other exclusive jobs
          ProcessInstance processInstance = firstJob.getProcessInstance();
          jobs = jobSession.findExclusiveJobs(lockOwner, processInstance);
          if (debug) log.debug("acquiring exclusive " + jobs + " for " + processInstance);
        }
        else {
          jobs = Collections.singletonList(firstJob);
          if (debug) log.debug("acquiring " + firstJob);
        }

        // acquire jobs
        Date lockTime = new Date();
        for (Iterator i = jobs.iterator(); i.hasNext();) {
          // lock job
          Job job = (Job) i.next();
          job.setLockOwner(lockOwner);
          job.setLockTime(lockTime);
          // has job failed previously?
          if (job.getException() != null) {
            // decrease retry count
            int retries = job.getRetries() - 1;
            job.setRetries(retries);
            if (debug) log.debug(job + " has " + retries + " retries remaining");
          }
        }
        if (debug) log.debug("acquired " + jobs);
      }
      else {
        jobs = Collections.EMPTY_LIST;
        if (debug) log.debug("no acquirable job found");
      }
    }
    catch (RuntimeException e) {
      jobs = Collections.EMPTY_LIST;
      if (debug) log.debug("failed to acquire jobs", e);
    }
    return jobs;
  }

  protected void executeJob(Job job) throws Exception {
    JobSession jobSession = jbpmContext.getJobSession();
    jobSession.reattachJob(job);

    // register process instance for automatic save
    // see https://jira.jboss.org/jira/browse/JBPM-1015
    jbpmContext.addAutoSaveProcessInstance(job.getProcessInstance());

    if (log.isDebugEnabled()) log.debug("executing " + job);
    if (job.execute(jbpmContext)) jobSession.deleteJob(job);
  }


}
