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
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jbpm.JbpmException;
import org.jbpm.ejb.impl.JobListenerBean;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

/**
 * The JMS message service leverages the reliable communication infrastructure
 * accessed through JMS interfaces to deliver asynchronous continuation
 * messages to the {@link JobListenerBean}.
 * 
 * <h3>Configuration</h3>
 * 
 * The JMS message service factory exposes the following configurable fields.
 * 
 * <ul>
 * <li><code>connectionFactoryJndiName</code></li>
 * <li><code>destinationJndiName</code></li>
 * <li><code>isCommitEnabled</code></li>
 * </ul>
 * 
 * Refer to the jBPM manual for details.
 * 
 * @author Tom Baeyens
 * @author Alejandro Guizar
 */
public class JmsMessageServiceFactory implements ServiceFactory
{
  private static final long serialVersionUID = 1L;

  String connectionFactoryJndiName = "java:comp/env/jms/JbpmConnectionFactory";
  String destinationJndiName = "java:comp/env/jms/JobQueue";
  String commandDestinationJndiName = "java:comp/env/jms/CommandQueue";
  boolean isCommitEnabled = false;

  private ConnectionFactory connectionFactory;
  private Destination destination;
  private Destination commandDestination;

  public ConnectionFactory getConnectionFactory()
  {
    if (connectionFactory == null)
    {
      try
      {
        connectionFactory = (ConnectionFactory)lookup(connectionFactoryJndiName);
      }
      catch (NamingException e)
      {
        throw new JbpmException("could not retrieve message connection factory", e);
      }
    }
    return connectionFactory;
  }

  public Destination getDestination()
  {
    if (destination == null)
    {
      try
      {
        destination = (Destination)lookup(destinationJndiName);
      }
      catch (NamingException e)
      {
        throw new JbpmException("could not retrieve job destination", e);
      }
    }
    return destination;
  }

  public Destination getCommandDestination()
  {
    if (commandDestination == null)
    {
      try
      {
        commandDestination = (Destination)lookup(commandDestinationJndiName);
      }
      catch (NamingException e)
      {
        throw new JbpmException("could not retrieve command destination", e);
      }
    }
    return commandDestination;
  }

  public boolean isCommitEnabled()
  {
    return isCommitEnabled;
  }

  private static Object lookup(String name) throws NamingException
  {
    Context initial = new InitialContext();
    try
    {
      return initial.lookup(name);
    }
    finally
    {
      initial.close();
    }
  }

  public Service openService()
  {
    try
    {
      Connection connection = getConnectionFactory().createConnection();
      return new JmsMessageService(connection, getDestination(), isCommitEnabled);
    }
    catch (JMSException e)
    {
      throw new JbpmException("couldn't open message session", e);
    }
  }

  public void close()
  {
    connectionFactory = null;
    destination = null;
    commandDestination = null;
  }

}
