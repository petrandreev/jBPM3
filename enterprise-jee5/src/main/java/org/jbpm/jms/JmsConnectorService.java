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

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;
import org.jbpm.msg.MessageService;
import org.jbpm.scheduler.SchedulerService;

public class JmsConnectorService implements MessageService, SchedulerService {

  private final JmsConnectorServiceFactory serviceFactory;
  private final Connection connection;

  private static final String SCHEDULED_DELIVERY_PROP = "JMS_JBOSS_SCHEDULED_DELIVERY";
  private static final String GROUP_ID_PROP = "JMSXGroupID";
  private static final String GROUP_PREFIX = "jBPMPID";
  private static final long serialVersionUID = 2L;

  private static final Log log = LogFactory.getLog(JmsConnectorService.class);

  JmsConnectorService(JmsConnectorServiceFactory serviceFactory) throws JMSException {
    connection = serviceFactory.getConnectionFactory().createConnection();
    this.serviceFactory = serviceFactory;
  }

  private JobSession getJobSession() {
    return serviceFactory.getJbpmConfiguration().getCurrentJbpmContext().getJobSession();
  }

  public void send(Job job) {
    getJobSession().saveJob(job);
    try {
      sendMessage(job);
    }
    catch (JMSException e) {
      throw new JbpmException("failed to send job message", e);
    }
  }

  final void sendMessage(Job job) throws JMSException {
    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    try {
      Message message = session.createMessage();
      populateMessage(message, job);

      MessageProducer messageProducer = session.createProducer(serviceFactory.getDestination());
      messageProducer.send(message);
    }
    finally {
      // there is no need to close the producers of a closed session
      session.close();
    }
  }

  protected void populateMessage(Message message, Job job) throws JMSException {
    message.setLongProperty("jobId", job.getId());

    if (job instanceof Timer) {
      Timer timer = (Timer) job;

      if (log.isDebugEnabled()) {
        log.debug("scheduling " + timer + " to execute at " + timer.getDueDate());
      }
      message.setLongProperty(SCHEDULED_DELIVERY_PROP, timer.getDueDate().getTime());
      // raise timer priority
      message.setJMSPriority(9);
    }

    if (job.isExclusive()) {
      message.setStringProperty(GROUP_ID_PROP, GROUP_PREFIX + job.getProcessInstance().getId());
    }
  }

  public void createTimer(Timer timer) {
    send(timer);
  }

  public void deleteTimer(Timer timer) {
    getJobSession().deleteJob(timer);
  }

  public void deleteTimersByName(String timerName, Token token) {
    getJobSession().deleteTimersByName(timerName, token);
  }

  public void deleteTimersByProcessInstance(ProcessInstance processInstance) {
    getJobSession().deleteJobsForProcessInstance(processInstance);
  }

  public void close() {
    try {
      connection.close();
    }
    catch (JMSException e) {
      throw new JbpmException("failed to close jms connection", e);
    }
  }
}
