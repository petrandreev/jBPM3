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
package org.jbpm.ejb.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.command.Command;
import org.jbpm.job.Job;
import org.jbpm.persistence.db.DbPersistenceService;

/**
 * Individual job processing command.
 * 
 * @author Alejandro Guizar
 */
public class ExecuteJobCommand implements Command {

  private final long jobId;

  private static final long serialVersionUID = 1L;

  public ExecuteJobCommand(long jobId) {
    this.jobId = jobId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    Job job = acquireJob(jbpmContext);
    executeJob(job, jbpmContext);
    return job;
  }

  private Job acquireJob(JbpmContext jbpmContext) {
    Job job = jbpmContext.getJobSession().loadJob(jobId);

    // if job is exclusive, lock process instance
    if (job.isExclusive()) {
      jbpmContext.getGraphSession().lockProcessInstance(job.getProcessInstance());
    }

    // mark job as locked to prevent other parts of the engine from deleting it
    job.setLockOwner(toString());
    return job;
  }

  static void executeJob(Job job, JbpmContext jbpmContext) {
    log.debug("executing " + job);
    try {
      if (job.execute(jbpmContext)) {
        jbpmContext.getJobSession().deleteJob(job);
      }
    }
    catch (Exception e) {
      log.debug("exception while executing " + job, e);
      if (!DbPersistenceService.isPersistenceException(e)) {
        StringWriter memoryWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(memoryWriter));
        job.setException(memoryWriter.toString());
      }
      else {
        // allowing a transaction to proceed after a persistence exception is unsafe
        throw e instanceof RuntimeException ? (RuntimeException) e :
        	new JbpmException("failed to execute " + job, e);
      }
    }
  }

  private static Log log = LogFactory.getLog(ExecuteJobCommand.class);
}
