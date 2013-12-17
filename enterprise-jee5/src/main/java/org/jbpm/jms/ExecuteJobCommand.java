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
package org.jbpm.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;

/**
 * Individual job processing command.
 * 
 * @author Alejandro Guizar
 */
public class ExecuteJobCommand implements Command {

  private final long jobId;

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(ExecuteJobCommand.class);

  public ExecuteJobCommand(long jobId) {
    this.jobId = jobId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    Job job = acquireJob(jobId, jbpmContext);
    if (job != null) executeJob(job, jbpmContext);
    return job;
  }

  private static Job acquireJob(long jobId, JbpmContext jbpmContext) {
    boolean debug = log.isDebugEnabled();
    if (debug) log.debug("acquiring job: " + jobId);
    Job job = jbpmContext.getJobSession().getJob(jobId);

    // job could have been deleted manually
    // or by ending the process instance
    if (job != null) {
      // register process instance for automatic save
      // see https://jira.jboss.org/jira/browse/JBPM-1015
      ProcessInstance processInstance = job.getProcessInstance();
      jbpmContext.addAutoSaveProcessInstance(processInstance);

      // if job is exclusive, lock process instance
      if (job.isExclusive()) {
        jbpmContext.getGraphSession().lockProcessInstance(processInstance);
      }

      // mark job as locked to prevent it from being deleted
      job.setLockOwner(Thread.currentThread().getName());
      if (debug) log.debug("acquired " + job);
    }
    else {
      log.warn("job not found: " + jobId);
    }

    return job;
  }

  private static void executeJob(Job job, JbpmContext jbpmContext) throws Exception {
    if (log.isDebugEnabled()) log.debug("executing " + job);
    if (job.execute(jbpmContext)) {
      // clear job
      jbpmContext.getJobSession().deleteJob(job);
    }
    else {
      // job is a repetitive timer
      Timer timer = jbpmContext.getJobSession().loadTimer(job.getId());
      JmsConnectorService schedulerService = (JmsConnectorService) jbpmContext.getServices()
        .getSchedulerService();
      schedulerService.sendMessage(timer);
    }
  }

  @Override
  public String toString() {
    return "ExecuteJobCommand(" + jobId + ")";
  }
}
