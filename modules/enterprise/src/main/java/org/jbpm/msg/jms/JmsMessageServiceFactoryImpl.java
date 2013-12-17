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
import javax.jms.JMSException;

import org.jbpm.JbpmException;
import org.jbpm.svc.Service;

/**
 * Alias for {@link JmsMessageServiceFactory}, supplied for compatibility
 * with the jBPM 3.2 series.
 * @deprecated replaced by {@link JmsMessageServiceFactory}
 */
public class JmsMessageServiceFactoryImpl extends JmsMessageServiceFactory {

  private static final long serialVersionUID = 1L;

  public Service openService() {
    try {
      Connection connection = getConnectionFactory().createConnection();
      return new JmsMessageServiceImpl(connection, getDestination(), isCommitEnabled);
    } 
    catch (JMSException e) {
      throw new JbpmException("couldn't open message session", e);
    } 
  }

}
