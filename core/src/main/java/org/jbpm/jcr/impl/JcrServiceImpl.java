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
package org.jbpm.jcr.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jbpm.JbpmException;
import org.jbpm.jcr.JcrService;
import org.jbpm.svc.Services;
import org.jbpm.tx.TxService;

public class JcrServiceImpl implements JcrService {

  private static final long serialVersionUID = 1L;

  protected Session session;

  public JcrServiceImpl(Session session) {
    this.session = session;
  }

  public Session getSession() {
    return session;
  }

  public void close() {
    TxService txService = getTxService();
    if (txService != null && txService.isRollbackOnly()) {
      try {
        session.refresh(false);
      }
      catch (RepositoryException e) {
        // NOTE that Errors are not caught because that might halt the JVM
        // and mask the original Error
        throw new JbpmException("could not refresh(rollback) jcr session", e);
      }
    }
    else {
      try {
        session.save();
      }
      catch (RepositoryException e) {
        // NOTE that Errors are not caught because that might halt the JVM
        // and mask the original Error
        throw new JbpmException("could not save jcr session", e);
      }
    }
    session.logout();
  }

  private static TxService getTxService() {
    return (TxService) Services.getCurrentService(Services.SERVICENAME_TX);
  }
}
