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
package org.jbpm.jbpm2608;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JobSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;

/**
 * {@link JobSession#findJobsWithOverdueLockTime(Date)} returns incorrect list
 * of overdue jobs.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2608">JBPM-2608</a>
 * @author Alejandro Guizar
 */
public class JBPM2608Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    deployProcessDefinition(processDefinition);
  }

  public void testFindJobsWithOverdueLockTime() {
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(getName());

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR, -1);
    // databases such as mysql do not have millisecond precision
    calendar.set(Calendar.MILLISECOND, 0);
    Date oneHourAgo = calendar.getTime();

    calendar.add(Calendar.SECOND, -1);
    Date oneHourAgoMinusOneSec = calendar.getTime();

    calendar.add(Calendar.SECOND, 2);
    Date oneHourAgoPlusOneSec = calendar.getTime();

    Timer timer = new Timer(processInstance.getRootToken());
    timer.setLockTime(oneHourAgo);
    jbpmContext.getServices().getSchedulerService().createTimer(timer);

    newTransaction();
    timer = jobSession.loadTimer(timer.getId());
    assertEquals(oneHourAgo, timer.getLockTime());

    List jobs = jobSession.findJobsWithOverdueLockTime(oneHourAgoMinusOneSec);
    assert jobs.isEmpty() : jobs;

    jobs = jobSession.findJobsWithOverdueLockTime(oneHourAgo);
    assert jobs.isEmpty() : jobs;

    jobs = jobSession.findJobsWithOverdueLockTime(oneHourAgoPlusOneSec);
    assertEquals(1, jobs.size());

    Job job = (Job) jobs.get(0);
    assertSame(job, timer);
  }

}
