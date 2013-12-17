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
package org.jbpm.persistence.db;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.ContextSession;
import org.jbpm.db.GraphSession;
import org.jbpm.db.JobSession;
import org.jbpm.db.LoggingSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.svc.Service;
import org.jbpm.svc.Services;
import org.jbpm.tx.TxService;

public class DbPersistenceService implements Service, PersistenceService {

  private static final long serialVersionUID = 1L;

  protected final DbPersistenceServiceFactory persistenceServiceFactory;

  protected Connection connection;
  protected boolean mustConnectionBeClosed;

  protected Transaction transaction;
  protected boolean isTransactionEnabled = true;
  protected boolean isCurrentSessionEnabled;

  protected Session session;
  protected boolean mustSessionBeFlushed;
  protected boolean mustSessionBeClosed;

  protected GraphSession graphSession;
  protected TaskMgmtSession taskMgmtSession;
  protected JobSession jobSession;
  protected ContextSession contextSession;
  protected LoggingSession loggingSession;

  /** @deprecated for access to other services, invoke {@link JbpmContext#getServices()} */
  protected Services services;

  public DbPersistenceService(DbPersistenceServiceFactory persistenceServiceFactory) {
    this.persistenceServiceFactory = persistenceServiceFactory;
    this.isTransactionEnabled = persistenceServiceFactory.isTransactionEnabled();
    this.isCurrentSessionEnabled = persistenceServiceFactory.isCurrentSessionEnabled();
  }

  public SessionFactory getSessionFactory() {
    return session != null ? session.getSessionFactory() : persistenceServiceFactory
        .getSessionFactory();
  }

  public Session getSession() {
    if (session == null && getSessionFactory() != null) {
      Connection connection = getConnection(false);
      if (isCurrentSessionEnabled) {
        log.debug("using current hibernate session");
        session = getSessionFactory().getCurrentSession();
        mustSessionBeClosed = false;
        mustSessionBeFlushed = false;
        mustConnectionBeClosed = false;
      }
      else if (connection != null) {
        log.debug("creating hibernate session on " + connection);
        session = getSessionFactory().openSession(connection);
        mustSessionBeClosed = true;
        mustSessionBeFlushed = true;
        mustConnectionBeClosed = false;
      }
      else {
        log.debug("creating hibernate session");
        session = getSessionFactory().openSession();
        mustSessionBeClosed = true;
        mustSessionBeFlushed = true;
        mustConnectionBeClosed = false;
      }

      if (isTransactionEnabled) {
        beginTransaction();
      }
    }
    return session;
  }

  public void beginTransaction() {
    log.debug("beginning hibernate transaction");
    transaction = session.beginTransaction();
    log.debug("begun " + transaction);
  }

  public void endTransaction() {
    if (isTransactionManagedExternally()) {
      if (session != null && getTxService().isRollbackOnly()) {
        throw new JbpmException("cannot mark externally managed transaction for rollback");
      }
      return;
    }

    if (!isTransactionRollbackOnly()) {
      Exception commitException = commit();
      if (commitException != null) {
        rollback();
        closeSession();
        closeConnection();
        throw new JbpmPersistenceException("transaction commit failed", commitException);
      }
    }
    else { // isRollbackOnly==true
      Exception rollbackException = rollback();
      if (rollbackException != null) {
        closeSession();
        closeConnection();
        throw new JbpmPersistenceException("transaction rollback failed", rollbackException);
      }
    }
  }

  public Connection getConnection() {
    return getConnection(true);
  }

  public Connection getConnection(boolean resolveSession) {
    if (connection == null) {
      if (persistenceServiceFactory.getDataSource() != null) {
        try {
          log.debug("fetching jdbc connection from datasource");
          connection = persistenceServiceFactory.getDataSource().getConnection();
          mustConnectionBeClosed = true;
        }
        catch (Exception e) {
          // NOTE that Errors are not caught because that might halt the JVM
          // and mask the original Error.
          throw new JbpmException("could not obtain connection from datasource", e);
        }
      }
      else {
        if (resolveSession) {
          // initializes the session member
          getSession();
        }
        if (session != null) {
          connection = session.connection();
          log.debug("fetched "
              + connection
              + " from hibernate session, client is responsible for closing it!");
          mustConnectionBeClosed = false;
        }
      }
    }
    return connection;
  }

  public boolean isTransactionActive() {
    return transaction != null && transaction.isActive();
  }

  protected boolean isTransactionManagedExternally() {
    return !isTransactionEnabled || transaction == null;
  }

  protected boolean isTransactionRollbackOnly() {
    return getTxService().isRollbackOnly();
  }

  public void close() {
    endTransaction();

    Exception flushException = flushSession();
    if (flushException != null) {
      // JBPM-1465: at this point, the transaction is already committed or rolled back
      // alternatively, the transaction is being managed externally
      // hence rolling back here is redundant and possibly dangerous
      closeSession();
      closeConnection();
      throw new JbpmPersistenceException("hibernate flush session failed", flushException);
    }

    Exception closeSessionException = closeSession();
    if (closeSessionException != null) {
      closeConnection();
      throw new JbpmPersistenceException("hibernate close session failed", closeSessionException);
    }

    Exception closeConnectionException = closeConnection();
    if (closeConnectionException != null) {
      throw new JbpmPersistenceException("hibernate close connection failed",
          closeConnectionException);
    }
  }

  protected Exception commit() {
    try {
      log.debug("committing " + transaction);
      mustSessionBeFlushed = false; // commit does a flush anyway
      transaction.commit();
    }
    catch (Exception e) {
      if (isStaleStateException(e)) {
        log.info("problem committing transaction: optimistic locking failed");
        StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error(
            "optimistic locking failed while committing " + transaction, e);
      }
      else {
        log.error("transaction commit failed", e);
      }
      return e;
    }
    return null;
  }

  protected Exception rollback() {
    try {
      log.debug("rolling back " + transaction);
      // flushing updates that will be rolled back is not very clever :-)
      mustSessionBeFlushed = false;
      transaction.rollback();
    }
    catch (Exception e) {
      log.error("transaction rollback failed", e);
      return e;
    }
    return null;
  }

  private Exception flushSession() {
    if (mustSessionBeFlushed) {
      try {
        log.debug("flushing " + session);
        session.flush();
      }
      catch (Exception e) {
        if (isStaleStateException(e)) {
          log.info("problem flushing session: optimistic locking failed");
          StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error(
              "optimistic locking failed while flushing " + session, e);
        }
        else {
          log.error("hibernate flush failed", e);
        }
        return e;
      }
    }
    return null;
  }

  private Exception closeSession() {
    if (mustSessionBeClosed) {
      try {
        if (session.isOpen()) {
          log.debug("closing hibernate session");
          session.close();
        }
        else {
          log.warn("hibernate session was already closed");
        }
      }
      catch (Exception e) {
        return e;
      }
    }
    return null;
  }

  private Exception closeConnection() {
    if (mustConnectionBeClosed) {
      try {
        if (connection != null) {
          log.debug("closing jdbc connection");
          connection.close();
        }
        else {
          log.warn("jdbc connection was already closed");
        }
      }
      catch (Exception e) {
        log.error("hibernate session close failed", e);
        return e;
      }
    }
    return null;
  }

  public void assignId(Object object) {
    try {
      getSession().save(object);
    }
    catch (Exception e) {
      // NOTE that Errors are not caught because that might halt the JVM
      // and mask the original Error.
      throw new JbpmPersistenceException("couldn't assign id to " + object, e);
    }
  }

  // getters and setters //////////////////////////////////////////////////////

  public GraphSession getGraphSession() {
    if (graphSession == null) {
      Session session = getSession();
      if (session != null) {
        graphSession = new GraphSession(session);
      }
    }
    return graphSession;
  }

  public LoggingSession getLoggingSession() {
    if (loggingSession == null) {
      Session session = getSession();
      if (session != null) {
        loggingSession = new LoggingSession(session);
      }
    }
    return loggingSession;
  }

  public JobSession getJobSession() {
    if (jobSession == null) {
      Session session = getSession();
      if (session != null) {
        jobSession = new JobSession(session);
      }
    }
    return jobSession;
  }

  public ContextSession getContextSession() {
    if (contextSession == null) {
      Session session = getSession();
      if (session != null) {
        contextSession = new ContextSession(session);
      }
    }
    return contextSession;
  }

  public TaskMgmtSession getTaskMgmtSession() {
    if (taskMgmtSession == null) {
      Session session = getSession();
      if (session != null) {
        taskMgmtSession = new TaskMgmtSession(session);
      }
    }
    return taskMgmtSession;
  }

  public DataSource getDataSource() {
    return persistenceServiceFactory.dataSource;
  }

  /**
   * @deprecated use {@link TxService#isRollbackOnly()} instead
   */
  public boolean isRollbackOnly() {
    return getTxService().isRollbackOnly();
  }

  /**
   * @deprecated use {@link TxService#setRollbackOnly()} instead
   */
  public void setRollbackOnly() {
    getTxService().setRollbackOnly();
  }

  /**
   * @throws UnsupportedOperationException if <code>rollbackOnly</code> is <code>false</code>
   * @deprecated use {@link TxService#setRollbackOnly()} instead
   */
  public void setRollbackOnly(boolean rollbackOnly) {
    if (!rollbackOnly) {
      throw new UnsupportedOperationException();
    }
    setRollbackOnly();
  }

  private TxService getTxService() {
    return (TxService) Services.getCurrentService(Services.SERVICENAME_TX);
  }

  /**
   * Injects an external Hibernate session, disabling transaction management.
   */
  public void setSession(Session session) {
    setSession(session, false);
  }

  /**
   * Injects an external Hibernate session without affecting transaction management.
   * 
   * @deprecated use {@link #setSession(Session, boolean) setSession(session, true)} instead
   */
  public void setSessionWithoutDisablingTx(Session session) {
    setSession(session, true);
  }

  /**
   * Injects an external Hibernate session. Injecting a session would normally disable transaction
   * management. The <code>keepTransactionEnabled</code> parameter can be used to prevent
   * transaction management from being disabled, according to the following table.
   * <table border="1">
   * <tr>
   * <th>is currently enabled?</th>
   * <th>keep enabled?</th>
   * <th>enabled onward</th>
   * </tr>
   * <tr>
   * <td>true</td>
   * <td>true</td>
   * <td>true (no change)</td>
   * </tr>
   * <tr>
   * <td>true</td>
   * <td>false</td>
   * <td>false</td>
   * </tr>
   * <tr>
   * <td>false</td>
   * <td>n/a</td>
   * <td>false (no change)</td>
   * </tr>
   * </table>
   */
  public void setSession(Session session, boolean keepTransactionEnabled) {
    this.session = session;
    if (isTransactionEnabled && !keepTransactionEnabled) {
      log.debug("disabling transaction due to session injection");
      isTransactionEnabled = false;
    }
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public void setContextSession(ContextSession contextSession) {
    this.contextSession = contextSession;
  }

  public void setDataSource(DataSource dataSource) {
    this.persistenceServiceFactory.dataSource = dataSource;
  }

  public void setGraphSession(GraphSession graphSession) {
    this.graphSession = graphSession;
  }

  public void setLoggingSession(LoggingSession loggingSession) {
    this.loggingSession = loggingSession;
  }

  public void setJobSession(JobSession jobSession) {
    this.jobSession = jobSession;
  }

  public void setTaskMgmtSession(TaskMgmtSession taskMgmtSession) {
    this.taskMgmtSession = taskMgmtSession;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.persistenceServiceFactory.sessionFactory = sessionFactory;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public void setTransaction(Transaction transaction) {
    this.transaction = transaction;
  }

  public boolean isTransactionEnabled() {
    return isTransactionEnabled;
  }

  public void setTransactionEnabled(boolean isTransactionEnabled) {
    this.isTransactionEnabled = isTransactionEnabled;
  }

  public static boolean isPersistenceException(Exception exception) {
    for (Throwable t = exception; t != null; t = t.getCause()) {
      if (t instanceof HibernateException) return true;
    }
    return false;
  }

  public static boolean isStaleStateException(Exception exception) {
    for (Throwable t = exception; t != null; t = t.getCause()) {
      if (t instanceof StaleStateException) return true;
    }
    return false;
  }

  private static Log log = LogFactory.getLog(DbPersistenceService.class);
}
