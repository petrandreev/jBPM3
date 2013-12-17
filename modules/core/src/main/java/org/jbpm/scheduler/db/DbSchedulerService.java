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
package org.jbpm.scheduler.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.scheduler.SchedulerService;

public class DbSchedulerService implements SchedulerService {

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(DbSchedulerService.class);
  
  JobSession jobSession = null;
  JobExecutor jobExecutor = null;
  boolean hasProducedJobs = false;

  public DbSchedulerService() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext==null) {
      throw new JbpmException("instantiation of the DbSchedulerService requires a current JbpmContext");
    }
    this.jobSession = jbpmContext.getJobSession();
    this.jobExecutor = jbpmContext.getJbpmConfiguration().getJobExecutor();
  }
  
  public void createTimer(Timer timerJob) {
    jobSession.saveJob(timerJob);
    hasProducedJobs = true;
  }

  public void deleteTimer(Timer timer) {
    jobSession.deleteJob(timer);
  }

  public void deleteTimersByName(String timerName, Token token) {
    jobSession.deleteTimersByName(timerName, token);
  }

  public void deleteTimersByProcessInstance(ProcessInstance processInstance) {
    jobSession.deleteJobsForProcessInstance(processInstance);
  }

  public void close() {
    if (hasProducedJobs && jobExecutor != null) {
      log.debug("timers were produced, job executor will be signalled");
      synchronized (jobExecutor) {
        jobExecutor.notify();
      }
    }
  }
}
