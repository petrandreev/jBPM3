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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.jbpm.JbpmException;
import org.jbpm.persistence.db.DbPersistenceService;

public class JtaDbPersistenceService extends DbPersistenceService {

  private UserTransaction userTransaction;

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(JtaDbPersistenceService.class);

  public JtaDbPersistenceService(JtaDbPersistenceServiceFactory persistenceServiceFactory) {
    super(persistenceServiceFactory);
    // if no transaction is underway, begin one
    if (getTransactionStatus() == Status.STATUS_NO_TRANSACTION) beginTransaction();
  }

  public boolean isTransactionActive() {
    return getTransactionStatus() == Status.STATUS_ACTIVE;
  }

  protected boolean isTransactionManagedExternally() {
    return userTransaction == null;
  }

  protected boolean isTransactionRollbackOnly() {
    return super.isTransactionRollbackOnly()
      || getTransactionStatus() == Status.STATUS_MARKED_ROLLBACK;
  }

  public void beginTransaction() {
    try {
      if (log.isDebugEnabled()) log.debug("beginning user transaction");
      JtaDbPersistenceServiceFactory jtaFactory =
        (JtaDbPersistenceServiceFactory) persistenceServiceFactory;
      userTransaction = jtaFactory.getUserTransaction();
      userTransaction.begin();
    }
    catch (NotSupportedException e) {
      throw new JbpmException("transaction already in course", e);
    }
    catch (SystemException e) {
      throw new JbpmException("failed to begin transaction", e);
    }
  }

  private int getTransactionStatus() {
    try {
      if (userTransaction != null) {
        return userTransaction.getStatus();
      }
      else {
        TransactionManager transactionManager = getTransactionManager();
        return transactionManager != null ? transactionManager.getStatus()
          : Status.STATUS_NO_TRANSACTION;
      }
    }
    catch (SystemException e) {
      log.error("could not get transaction status", e);
      return Status.STATUS_UNKNOWN;
    }
  }

  private TransactionManager getTransactionManager() {
    final JtaPlatform jtaPlatform = ((SessionFactoryImplementor) getSessionFactory()).getServiceRegistry().getService( JtaPlatform.class );
    return jtaPlatform.retrieveTransactionManager();
  }

  protected Exception commit() {
    if (userTransaction != null) {
      if (log.isDebugEnabled()) log.debug("committing user transaction");
      try {
        userTransaction.commit();
      }
      catch (RollbackException e) {
        return e;
      }
      catch (HeuristicMixedException e) {
        return e;
      }
      catch (HeuristicRollbackException e) {
        return e;
      }
      catch (SystemException e) {
        return e;
      }
    }
    return null;
  }

  protected Exception rollback() {
    if (userTransaction != null) {
      if (log.isDebugEnabled()) log.debug("rolling back user transaction");
      try {
        userTransaction.rollback();
      }
      catch (SystemException e) {
        return e;
      }
      catch (RuntimeException e) {
        return e;
      }
    }
    else {
      TransactionManager transactionManager = getTransactionManager();
      if (transactionManager == null) {
        throw new JbpmException("cannot honor rollback request without transaction manager");
      }
      if (log.isDebugEnabled()) log.debug("marking external transaction for rollback");
      try {
        transactionManager.setRollbackOnly();
      }
      catch (SystemException e) {
        return e;
      }
    }
    return null;
  }

  public boolean isJtaTxCreated() {
    return !isTransactionManagedExternally();
  }
}
