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
package org.jbpm.msg.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.msg.MessageService;

public class DbMessageService implements MessageService {
  
  private static final long serialVersionUID = 1L;

  JobSession jobSession = null;
  JobExecutor jobExecutor = null;
  boolean hasProducedJobs = false;
  
  public DbMessageService() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext==null) {
      throw new JbpmException("instantiation of the DbMessageService requires a current JbpmContext");
    }
    jobSession = jbpmContext.getJobSession();
    jobExecutor = jbpmContext.getJbpmConfiguration().getJobExecutor();
  }

  public void send(Job job) {
    jobSession.saveJob(job);
    log.debug("saved "+job);
    hasProducedJobs = true;
  }

  public void close() {
    if ( (hasProducedJobs)
         && (jobExecutor!=null)
       ) {
      log.debug("messages were produced, job executor will be signalled");
      synchronized(jobExecutor) {
        jobExecutor.notify();
      }
    }
  }
  
  private static Log log = LogFactory.getLog(DbMessageService.class);
}
