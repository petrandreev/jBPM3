package org.jbpm.seam;

import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.job.executor.JobExecutorThread;

public class CustomJobExecutorThread extends JobExecutorThread {

  public CustomJobExecutorThread(String name, JobExecutor jobExecutor) {
    super(name, jobExecutor);
  }

  protected void executeJob(Job job) {
    // intercept before
    JobExecutorCustomizationTest.addJobEvent("before");
    try {
      super.executeJob(job);
    }
    finally {
      // intercept after
      JobExecutorCustomizationTest.addJobEvent("after");
    }
  }
}
