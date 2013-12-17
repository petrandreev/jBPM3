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

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.jcr.JcrService;
import org.jbpm.svc.Services;
import org.jbpm.tx.TxService;

public class JcrServiceImpl implements JcrService {

  private static final long serialVersionUID = 1L;
  
  protected Session session = null;
  Services services = null;

  public JcrServiceImpl(Session session) {
    this.session = session;
    this.services = JbpmContext.getCurrentJbpmContext().getServices();
  }

  public Session getSession() {
    return session;
  }

  public void close() {
    log.debug("closing jcr session");
    
    TxService txService = (services!=null ? services.getTxService() : null);
    if (txService!=null) {
      if (txService.isRollbackOnly()) {
        log.debug("refreshing jcr session because tx service is marked with rollback-only");
        try {
          session.refresh(false);
        } catch (Exception e) {
          // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
          throw new JbpmException("couldn't refresh(rollback) JCR session", e);
        }
      } else {
        log.debug("committing non-JTA JCR session by invoking the session.save()");
        save();
      }
    } else {
      save();
    }

    try {
      session.logout();
    } catch (Exception e) {
      // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
      throw new JbpmException("couldn't save JCR session", e);
    }
  }

  private void save() {
    log.debug("saving jcr session");
    try {
      session.save();
    } catch (Exception e) {
      // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
      throw new JbpmException("couldn't save jackrabbit jcr session", e);
    }
  }

  private static Log log = LogFactory.getLog(JcrServiceImpl.class);
}
