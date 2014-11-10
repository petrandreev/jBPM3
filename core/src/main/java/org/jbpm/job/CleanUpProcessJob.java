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
package org.jbpm.job;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;

/**
 * Cancels jobs associated to a process instance.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-1709">JBPM-1709</a>
 * @author Alejandro Guizar
 */
public class CleanUpProcessJob extends Job {

  private static final long serialVersionUID = 1L;

  public CleanUpProcessJob() {
    // default constructor
  }

  public CleanUpProcessJob(Token token) {
    super(token);
  }

  public boolean execute(JbpmContext jbpmContext) throws Exception {
    // is scheduler service available?
    SchedulerService schedulerService = jbpmContext.getServices().getSchedulerService();
    if (schedulerService != null) {
      // give scheduler a chance to cancel timers
      schedulerService.deleteTimersByProcessInstance(getProcessInstance());
    }
    else {
      // just delete jobs straight from the database
      jbpmContext.getJobSession().deleteJobsForProcessInstance(getProcessInstance());
    }
    return true;
  }

  public String toString() {
    return "CleanUpProcessJob(" + getId() + ',' + getProcessInstance() + ')';
  }

}
