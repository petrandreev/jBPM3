package org.jbpm.seam;

import org.jbpm.JbpmContext;
import org.jbpm.db.JobSession;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.job.executor.JobExecutorThread;

public class CustomJobExecutorThread extends JobExecutorThread {

  private JobExecutor jobExecutor;

  public CustomJobExecutorThread(String name, JobExecutor jobExecutor) {
    super(name, jobExecutor);
    this.jobExecutor = jobExecutor;
  }

  protected void executeJob(Job job) {
    JbpmContext jbpmContext = jobExecutor.getJbpmConfiguration().createJbpmContext();
    try {
      JobSession jobSession = jbpmContext.getJobSession();
      jobSession.reattachJob(job);

      // custom stuff
      job.getProcessInstance().getContextInstance().setVariable("custom", Boolean.TRUE);

      if (job.execute(jbpmContext)) jobSession.deleteJob(job);
    }
    catch (Exception e) {
      e.printStackTrace();
      jbpmContext.setRollbackOnly();
    }
    finally {
      jbpmContext.close();
    }
  }
}
