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
package org.jbpm.persistence.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.util.JTAHelper;
import org.jbpm.JbpmException;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;

public class JtaDbPersistenceService extends DbPersistenceService {

  private UserTransaction transaction;

  private static final long serialVersionUID = 1L;

  private static Log log = LogFactory.getLog(JtaDbPersistenceService.class);

  public JtaDbPersistenceService(JtaDbPersistenceServiceFactory persistenceServiceFactory) {
    super(persistenceServiceFactory);

    if (!isTransactionActive()) {
      beginTransaction();
    }
  }

  public boolean isTransactionActive() {
    SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) getSessionFactory();
    return JTAHelper.isTransactionInProgress(sessionFactory);
  }

  protected boolean isTransactionManagedExternally() {
    return transaction == null;
  }

  protected boolean isTransactionRollbackOnly() {
    return super.isTransactionRollbackOnly()
        || JTAHelper.isMarkedForRollback(getTransactionStatus());
  }

  public void beginTransaction() {
    try {
      log.debug("beginning " + transaction);
      JtaDbPersistenceServiceFactory jtaFactory = (JtaDbPersistenceServiceFactory) persistenceServiceFactory;
      transaction = jtaFactory.getUserTransaction();
      transaction.begin();
    }
    catch (Exception e) {
      throw new JbpmException("transaction begin failed", e);
    }
  }

  private int getTransactionStatus() {
    try {
      return transaction.getStatus();
    }
    catch (SystemException e) {
      log.error("could not get transaction status", e);
      return Status.STATUS_UNKNOWN;
    }
  }

  protected Exception commit() {
    log.debug("committing " + transaction);
    try {
      transaction.commit();
      return null;
    }
    catch (Exception e) {
      if (isStaleStateException(e)) {
        log.debug("optimistic locking failed, could not commit " + transaction);
        StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error(
            "optimistic locking failed, could not commit " + transaction, e);
      }
      else {
        // Switched to debug because either handle OR log 
        // an exception is best practice, not both!
        // Example: Clustered JobExecutor may have
        // Exceptions which should be kept quiet
        log.debug("transaction commit failed", e);
      }
      return e;
    }
  }

  protected Exception rollback() {
    log.debug("rolling back " + transaction);
    try {
      transaction.rollback();
      return null;
    }
    catch (Exception e) {
      // Switched to debug because either handle OR log 
      // an exception is best practice, not both!
      // Example: Clustered JobExecutor may have
      // Exceptions which should be kept quiet
      log.debug("transaction rollback failed", e);
      return e;
    }
  }

  public boolean isJtaTxCreated() {
    return !isTransactionManagedExternally();
  }
}
