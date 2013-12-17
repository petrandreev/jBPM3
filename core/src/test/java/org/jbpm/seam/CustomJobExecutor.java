package org.jbpm.seam;

import org.jbpm.job.executor.JobExecutor;

public class CustomJobExecutor extends JobExecutor {
  private static final long serialVersionUID = 1L;

  protected Thread createThread(String threadName) {
    return new CustomJobExecutorThread(threadName, this);
  }
}
