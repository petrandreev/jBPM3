package org.jbpm.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.StaleStateException;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;

/**
 * Execute all overdue Jobs (may be enhanced with more attributes in future if needed)
 * 
 * @author ruecker
 */
public class ExecuteJobsCommand extends AbstractBaseCommand
{

  private static final long serialVersionUID = -2457066688404533959L;

  private static final Log log = LogFactory.getLog(ExecuteJobsCommand.class);

  private static final int maxLockTime = 60000;

  private transient JbpmContext jbpmContext;

  public Object execute(JbpmContext jbpmContext) throws Exception
  {
    this.jbpmContext = jbpmContext;
    try
    {
      Collection acquiredJobs = acquireJobs();

      if (!acquiredJobs.isEmpty())
      {
        Iterator iter = acquiredJobs.iterator();
        while (iter.hasNext())
        {
          Job job = (Job)iter.next();
          executeJob(job);
        }
      }

      // Job job = jbpmContext.getJobSession().getFirstAcquirableJob("");
      // if (job != null) {
      // log.info("execution job: " + job);
      // job.execute(jbpmContext);
      // }
    }
    catch (JbpmException ex)
    {
      log.warn("exception while executing job", ex);
    }
    this.jbpmContext = null;
    return null;
  }

  private String getName()
  {
    return this.toString();
  }

  protected Collection acquireJobs()
  {
    Collection acquiredJobs = null;
    Collection jobsToLock = new ArrayList();
    log.debug("acquiring jobs for execution...");

    try
    {
      JobSession jobSession = jbpmContext.getJobSession();
      log.debug("querying for acquirable job...");
      Job job = jobSession.getFirstAcquirableJob(getName());
      if (job != null)
      {
        if (job.isExclusive())
        {
          log.debug("exclusive acquirable job found (" + job + "). querying for other exclusive jobs to lock them all in one tx...");
          List otherExclusiveJobs = jobSession.findExclusiveJobs(getName(), job.getProcessInstance());
          jobsToLock.addAll(otherExclusiveJobs);
          log.debug("trying to obtain a process-instance exclusive locks for '" + otherExclusiveJobs + "'");
        }
        else
        {
          log.debug("trying to obtain a lock for '" + job + "'");
          jobsToLock.add(job);
        }

        Iterator iter = jobsToLock.iterator();
        while (iter.hasNext())
        {
          job = (Job)iter.next();
          job.setLockOwner(getName());
          job.setLockTime(new Date());
          // jbpmContext.getSession().update(job);
        }

        // HACKY HACK : this is a workaround for a hibernate problem that is fixed in hibernate 3.2.1
        if (job instanceof Timer)
        {
          Hibernate.initialize(((Timer)job).getGraphElement());
        }

      }
      else
      {
        log.debug("no acquirable jobs in job table");
      }

      acquiredJobs = jobsToLock;
      log.debug("obtained locks on following jobs: " + acquiredJobs);

    }
    catch (StaleStateException e)
    {
      log.debug("couldn't acquire lock on job(s): " + jobsToLock);
    }
    return acquiredJobs;
  }

  protected void executeJob(Job job)
  {
    JobSession jobSession = jbpmContext.getJobSession();
    job = jobSession.loadJob(job.getId());

    try
    {
      log.debug("executing job " + job);
      if (job.execute(jbpmContext))
      {
        jobSession.deleteJob(job);
      }

    }
    catch (Exception e)
    {
      log.debug("exception while executing '" + job + "'", e);
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      job.setException(sw.toString());
      job.setRetries(job.getRetries() - 1);
    }

    // if this job is locked too long
    long totalLockTimeInMillis = System.currentTimeMillis() - job.getLockTime().getTime();
    if (totalLockTimeInMillis > maxLockTime)
    {
      jbpmContext.setRollbackOnly();
    }

  }

  protected Date getNextDueDate()
  {
    Date nextDueDate = null;
    JobSession jobSession = jbpmContext.getJobSession();
    Job job = jobSession.getFirstDueJob(getName(), new ArrayList());
    if (job != null)
    {
      nextDueDate = job.getDueDate();
    }
    return nextDueDate;
  }
  
}
