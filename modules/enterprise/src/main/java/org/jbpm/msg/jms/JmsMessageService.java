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
package org.jbpm.msg.jms;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.job.Job;
import org.jbpm.msg.MessageService;

public class JmsMessageService implements MessageService {

  private static final long serialVersionUID = 1L;

  JobSession jobSession = null;
  Connection connection = null;
  Session session = null;
  Destination destination = null;
  MessageProducer messageProducer = null;
  boolean isCommitEnabled = false;

  public JmsMessageService(Connection connection, Destination destination, boolean isCommitEnabled) throws JMSException {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext==null) {
      throw new JbpmException("jms message service must be created inside a jbpm context");
    }
    this.jobSession = jbpmContext.getJobSession();

    this.connection = connection;
    this.destination = destination;
    this.isCommitEnabled = isCommitEnabled;
    /* 
     * If the connection supports XA, the session will always take part in the global transaction.
     * Otherwise the first parameter specifies whether message productions and consumptions 
     * are part of a single transaction (TRUE) or performed immediately (FALSE).
     * Messages are never meant to be received before the database transaction commits,
     * hence the transacted is preferable.
     */
    session = connection.createSession(true, Session.SESSION_TRANSACTED);
  }

  public void send(Job job) {
    try {
      jobSession.saveJob(job);
      
      Message message = session.createMessage();
      message.setLongProperty("jobId", job.getId());
      if (job.getToken()!=null) {
        message.setLongProperty("tokenId", job.getToken().getId());
      }
      if (job.getProcessInstance()!=null) {
        message.setLongProperty("processInstanceId", job.getProcessInstance().getId());
      }
      if (job.getTaskInstance()!=null) {
        message.setLongProperty("taskInstanceId", job.getTaskInstance().getId());
      }
      modifyMessage(message, job);
      getMessageProducer().send(message);
    } catch (JMSException e) {
      throw new JbpmException("couldn't send jms message", e);
    }
  }

  /**
   * Hook to modify the message, e.g. adding additional properties
   * to the header required by the own application. One possible 
   * use case is to rescue the actor id over the "JMS" intermezzo
   * of asynchronous continuations.
   */
  public void modifyMessage(Message message, Job job) throws JMSException {
  }

  public void close() {
    JbpmException exception = null;

    if (messageProducer!=null) {
      try {
        messageProducer.close();
      } catch (Exception e) {
        // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
        exception = new JbpmException("couldn't close message producer", e);
      }
    }

    if (session!=null) {
      if (isCommitEnabled) {
        try {
          session.commit();
        } catch (Exception e) {
          if (exception==null) {
            exception = new JbpmException("couldn't commit JMS session", e);
          }
        }
      }
      
      try {
        session.close();
      } catch (Exception e) {
        if (exception==null) {
          exception = new JbpmException("couldn't close JMS session", e);
        }
      }
    }

    if (connection!=null) {
      try {
        connection.close();
      } catch (Exception e) {
        if (exception==null) {
          exception = new JbpmException("couldn't close JMS connection", e);
        }
      }
    }

    if (exception!=null) {
      throw exception;
    }
  }

  public Session getSession() {
    return session;
  }

  protected MessageProducer getMessageProducer() throws JMSException {
    if (messageProducer==null) {
      messageProducer = session.createProducer(destination);
    }
    return messageProducer;
  }
}
