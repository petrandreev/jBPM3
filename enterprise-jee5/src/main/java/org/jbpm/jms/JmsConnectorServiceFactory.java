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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.ejb.JobListenerBean;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.util.JndiUtil;

/**
 * The JMS connector service leverages the reliable communication infrastructure available through
 * JMS interfaces to deliver asynchronous continuation messages to the {@link JobListenerBean}.
 * 
 * <h3>Configuration</h3>
 * 
 * The JMS connector service factory exposes the following configurable fields.
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
public class JmsConnectorServiceFactory implements ServiceFactory {

  private static final long serialVersionUID = 1L;

  private String connectionFactoryJndiName = "java:comp/env/jms/JbpmConnectionFactory";
  private String destinationJndiName = "java:comp/env/jms/JobQueue";

  public void setConnectionFactoryJndiName(String connectionFactoryJndiName) {
	this.connectionFactoryJndiName = connectionFactoryJndiName;
  }
  
  public void setDestinationJndiName(String destinationJndiName) {
	this.destinationJndiName = destinationJndiName;
  }
  
  private JbpmConfiguration jbpmConfiguration;

  private ConnectionFactory connectionFactory;
  private Destination destination;

  public synchronized ConnectionFactory getConnectionFactory() {
    if (connectionFactory == null) {
      connectionFactory = (ConnectionFactory) JndiUtil.lookup(connectionFactoryJndiName, ConnectionFactory.class);
    }
    return connectionFactory;
  }

  public synchronized Destination getDestination() {
    if (destination == null) {
      destination = (Destination) JndiUtil.lookup(destinationJndiName, Destination.class);
    }
    return destination;
  }

  public JbpmConfiguration getJbpmConfiguration() {
    // in case this field is not initialized in jbpm configuration
    if (jbpmConfiguration == null) {
      JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
      if (jbpmContext == null) throw new JbpmException("no active jbpm context");
      jbpmConfiguration = jbpmContext.getJbpmConfiguration();
    }
    return jbpmConfiguration;
  }

  public Service openService() {
    try {
      return new JmsConnectorService(this);
    }
    catch (JMSException e) {
      throw new JbpmException("could not open message service", e);
    }
  }

  public void close() {
    connectionFactory = null;
    destination = null;
  }

}
