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
package org.jbpm.logging.db;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.logging.LoggingService;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.persistence.db.DbPersistenceService;

public class DbLoggingService implements LoggingService {

  private static final long serialVersionUID = 2L;

  private final PersistenceService persistenceService;

  public DbLoggingService() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext == null) throw new JbpmException("no active jbpm context");
    persistenceService = jbpmContext.getServices().getPersistenceService();
  }

  public void log(ProcessLog processLog) {
    // check if transaction is active before saving log
    // https://jira.jboss.org/browse/JBPM-2983
    if (persistenceService instanceof DbPersistenceService) {
      DbPersistenceService dbPersistenceService = (DbPersistenceService) persistenceService;
      if (dbPersistenceService.isTransactionActive()) {
        // Improvement suggestions:
        // db-level: use hi-lo id strategy to avoid repetitive insert (dependent on db-lock)
        // session: use stateless session or at least different session
        // can we borrow connection safely? (open on top of another session)
        dbPersistenceService.getSession().save(processLog);
      }
    }
  }

  public void close() {
  }
}
