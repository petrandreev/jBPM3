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
package org.jbpm.db;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;
import org.jbpm.util.CollectionUtil;

public class JobSession {

  private final Session session;

  public JobSession(Session session) {
    this.session = session;
  }

  public Job getFirstAcquirableJob(String lockOwner) {
    try {
      return (Job) session.getNamedQuery("JobSession.getFirstAcquirableJob")
          .setString("lockOwner", lockOwner)
          .setTimestamp("now", new Date())
          .setMaxResults(1)
          .uniqueResult();
    }
    catch (HibernateException e) {
      throw new JbpmException("could not get first acquirable job", e);
    }
  }

  public List<Job> findExclusiveJobs(String lockOwner, ProcessInstance processInstance) {
    try {
      List<?> jobs = session.getNamedQuery("JobSession.findExclusiveJobs")
          .setString("lockOwner", lockOwner)
          .setTimestamp("now", new Date())
          .setParameter("processInstance", processInstance)
          .list();
      return CollectionUtil.checkList(jobs, Job.class);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not find exclusive jobs owned by '"
          + lockOwner
          + "' for "
          + processInstance, e);
    }
  }

  public List<Job> findJobsByToken(Token token) {
    try {
      List<?> jobs = session.getNamedQuery("JobSession.findJobsByToken")
          .setParameter("token", token)
          .list();
      return CollectionUtil.checkList(jobs, Job.class);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not find jobs for " + token, e);
    }
  }

  public Job getFirstDueJob(String lockOwner, Collection<Long> monitoredJobs) {
    try {
      Query query;
      if (monitoredJobs == null || monitoredJobs.isEmpty()) {
        query = session.getNamedQuery("JobSession.getFirstDueJob");
      }
      else {
        query = session.getNamedQuery("JobSession.getFirstDueJobExcludingMonitoredJobs");
        query.setParameterList("monitoredJobIds", monitoredJobs);
      }
      return (Job) query.setString("lockOwner", lockOwner)
          .setMaxResults(1)
          .uniqueResult();
    }
    catch (HibernateException e) {
      throw new JbpmException("could not get first due job owned by '"
          + lockOwner
          + "' ignoring jobs "
          + monitoredJobs, e);
    }
  }
  
  public void saveJob(Job job) {
    try {
      session.save(job);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not save " + job, e);
    }
  }

  public void deleteJob(Job job) {
    try {
      session.delete(job);
      log.debug("deleted " + job);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not delete " + job, e);
    }
  }

  public Job loadJob(long jobId) {
    try {
      return (Job) session.load(Job.class, new Long(jobId));
    }
    catch (HibernateException e) {
      throw new JbpmException("could not load job " + jobId, e);
    }
  }

  public Timer loadTimer(long timerId) {
    try {
      return (Timer) session.load(Timer.class, new Long(timerId));
    }
    catch (HibernateException e) {
      throw new JbpmException("could not load timer " + timerId, e);
    }
  }

  public List<Job> loadJobs(long... jobIds) {
    try {
      List<?> jobs = session.createCriteria(Job.class)
          .add(Restrictions.in("id", toObjectArray(jobIds)))
          .list();
      return CollectionUtil.checkList(jobs, Job.class);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not load jobs " + Arrays.toString(jobIds), e);
    }
  }

  private static Long[] toObjectArray(long[] primitives) {
    final int length = primitives.length;
    Long[] objects = new Long[length];
    for (int i = 0; i < length; i++) {
      objects[i] = primitives[i];
    }
    return objects;
  }

  public Job getJob(long jobId) {
    try {
      return (Job) session.get(Job.class, new Long(jobId));
    }
    catch (HibernateException e) {
      throw new JbpmException("could not get job " + jobId, e);
    }
  }

  public void suspendJobs(Token token) {
    try {
      session.getNamedQuery("JobSession.suspendJobs").setParameter("token", token).executeUpdate();
    }
    catch (HibernateException e) {
      throw new JbpmException("could not suspend jobs for " + token, e);
    }
  }

  public void resumeJobs(Token token) {
    try {
      session.getNamedQuery("JobSession.resumeJobs").setParameter("token", token).executeUpdate();
    }
    catch (HibernateException e) {
      throw new JbpmException("could not resume jobs for " + token, e);
    }
  }

  public void deleteTimersByName(String name, Token token) {
    try {
      // delete unowned timers
      int entityCount = session.getNamedQuery("JobSession.deleteTimersByName")
          .setString("name", name)
          .setParameter("token", token)
          .executeUpdate();
      log.debug("deleted " + entityCount + " timers by name '" + name + "' for " + token);

      // prevent further repetitions
      List<?> timers = session.getNamedQuery("JobSession.findRepeatingTimersByName")
          .setString("name", name)
          .setParameter("token", token)
          .list();
      preventFurtherRepetitions(timers);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not delete timers by name '" + name + "' for " + token, e);
    }
  }

  public int countDeletableJobsForProcessInstance(ProcessInstance processInstance) {
    Number jobCount = (Number) session.getNamedQuery("JobSession.countDeletableJobsForProcessInstance")
        .setParameter("processInstance", processInstance)
        .uniqueResult();
    return jobCount.intValue();
  }

  public void deleteJobsForProcessInstance(ProcessInstance processInstance) {
    try {
      // delete unowned node-execute-jobs and timers
      int entityCount = session.getNamedQuery("JobSession.deleteJobsForProcessInstance")
          .setParameter("processInstance", processInstance)
          .executeUpdate();
      log.debug("deleted " + entityCount + " jobs for " + processInstance);

      // prevent further repetitions
      List<?> timers = session.getNamedQuery("JobSession.findRepeatingTimersForProcessInstance")
          .setParameter("processInstance", processInstance)
          .list();
      preventFurtherRepetitions(timers);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not delete jobs for " + processInstance, e);
    }
  }

  private static void preventFurtherRepetitions(List<?> timers) {
    if (!timers.isEmpty()) {
      for (Timer timer : CollectionUtil.checkList(timers, Timer.class)) {
        timer.setRepeat(null);
      }
      log.debug("prevented further repetitions of " + timers);
    }
  }

  public List<Job> findJobsWithOverdueLockTime(Date threshold) {
    try {
      List<?> jobs = session.getNamedQuery("JobSession.findJobsWithOverdueLockTime")
          .setDate("threshold", threshold)
          .list();
      return CollectionUtil.checkList(jobs, Job.class);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not find jobs with lock time over " + threshold, e);
    }
  }

  /**
   * get all failed jobs. Failed job have a retry count
   * of 0 and the occured exception set.
   */
  public List<Job> findFailedJobs() {
    try {
      List<?> jobs = session.getNamedQuery("JobSession.findFailedJobs").list();
      return CollectionUtil.checkList(jobs, Job.class);
    }
    catch (HibernateException e) {
      throw new JbpmException("could not find failed jobs", e);
    }
  }

  private static Log log = LogFactory.getLog(JobSession.class);
}
