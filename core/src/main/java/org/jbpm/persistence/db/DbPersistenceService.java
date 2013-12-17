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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.LockAcquisitionException;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.ContextSession;
import org.jbpm.db.GraphSession;
import org.jbpm.db.JobSession;
import org.jbpm.db.LoggingSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.svc.Services;
import org.jbpm.tx.TxService;

public class DbPersistenceService implements PersistenceService {

  private static final long serialVersionUID = 1L;

  protected final DbPersistenceServiceFactory persistenceServiceFactory;

  protected Connection connection;
  protected boolean mustConnectionBeClosed;

  protected Transaction transaction;
  protected boolean isTransactionEnabled;
  protected boolean isCurrentSessionEnabled;

  protected Session session;
  protected boolean mustSessionBeFlushed;
  protected boolean mustSessionBeClosed;

  protected GraphSession graphSession;
  protected TaskMgmtSession taskMgmtSession;
  protected JobSession jobSession;
  protected ContextSession contextSession;
  protected LoggingSession loggingSession;

  private Map customSessions;

  /**
   * @deprecated for access to other services, invoke {@link JbpmContext#getServices()}
   */
  protected Services services;

  public DbPersistenceService(DbPersistenceServiceFactory persistenceServiceFactory) {
    this.persistenceServiceFactory = persistenceServiceFactory;
    this.isTransactionEnabled = persistenceServiceFactory.isTransactionEnabled();
    this.isCurrentSessionEnabled = persistenceServiceFactory.isCurrentSessionEnabled();
  }

  public SessionFactory getSessionFactory() {
    return session != null ? session.getSessionFactory()
      : persistenceServiceFactory.getSessionFactory();
  }

  public Session getSession() {
    if (session == null) {
      SessionFactory sessionFactory = persistenceServiceFactory.getSessionFactory();

      if (isCurrentSessionEnabled) {
        session = sessionFactory.getCurrentSession();
        mustSessionBeFlushed = false;
        mustSessionBeClosed = false;
      }
      else {
        Connection connection = getConnection(false);
        session = connection != null ? sessionFactory.openSession(connection)
          : sessionFactory.openSession();
        mustSessionBeFlushed = true;
        mustSessionBeClosed = true;
      }

      if (isTransactionEnabled) beginTransaction();
    }
    return session;
  }

  public void beginTransaction() {
    transaction = session.beginTransaction();
    // commit does a flush anyway
    mustSessionBeFlushed = false;
  }

  public void endTransaction() {
    if (!isTransactionRollbackOnly()) {
      Exception commitException = commit();
      if (commitException != null) {
        rollback();
        closeSession();
        closeConnection();
        throw new JbpmPersistenceException("transaction commit failed", commitException);
      }
    }
    else {
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
      DataSource dataSource = persistenceServiceFactory.getDataSource();
      if (dataSource != null) {
        try {
          connection = dataSource.getConnection();
          mustConnectionBeClosed = true;
        }
        catch (SQLException e) {
          // NOTE that Errors are not caught because that might halt the JVM
          // and mask the original Error
          throw new JbpmPersistenceException("connection to data source failed", e);
        }
      }
      else {
        // resolve session or return
        Session session = this.session;
        if (session != null || resolveSession && (session = getSession()) != null) {
          // get connection from session
          connection = session.connection();
          /*
           * If the session is using aggressive connection release (as in a CMT environment),
           * the application is responsible for closing the returned connection. Otherwise, the
           * application should not close the connection.
           */
          SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
          ConnectionReleaseMode releaseMode = sessionFactory.getSettings()
            .getConnectionReleaseMode();
          mustConnectionBeClosed = releaseMode == ConnectionReleaseMode.AFTER_STATEMENT;
        }
      }
    }
    return connection;
  }

  public boolean isTransactionActive() {
    return transaction != null && transaction.isActive();
  }

  protected boolean isTransactionManagedExternally() {
    return !isTransactionEnabled;
  }

  protected boolean isTransactionRollbackOnly() {
    TxService txService = getTxService();
    return txService != null ? txService.isRollbackOnly() : false;
  }

  public void close() {
    // flush session
    Exception flushException = flushSession();
    if (flushException != null) {
      // JBPM-1465: if transaction is enabled, flush is disabled, see beginTransaction()
      // otherwise, transaction is managed externally
      // in either case, rolling back is unnecessary and possibly disastrous
      closeSession();
      closeConnection();
      throw new JbpmPersistenceException("failed to flush hibernate session", flushException);
    }

    // commit or roll back transaction
    endTransaction();

    // close session
    Exception closeSessionException = closeSession();
    if (closeSessionException != null) {
      closeConnection();
      throw new JbpmPersistenceException("failed to close hibernate session",
        closeSessionException);
    }

    // close jdbc connection
    Exception closeConnectionException = closeConnection();
    if (closeConnectionException != null) {
      throw new JbpmPersistenceException("failed to close hibernate connection",
        closeConnectionException);
    }
  }

  protected Exception commit() {
    if (!isTransactionManagedExternally() && transaction != null) {
      try {
        transaction.commit();
      }
      catch (RuntimeException e) {
        return e;
      }
    }
    return null;
  }

  protected Exception rollback() {
    // if there is an external transaction manager, throw an exception at it
    if (isTransactionManagedExternally()) {
      throw new JbpmPersistenceException("cannot honor rollback request under external transaction manager");
    }
    if (transaction != null) {
      try {
        transaction.rollback();
      }
      catch (RuntimeException e) {
        return e;
      }
    }
    return null;
  }

  private Exception flushSession() {
    if (mustSessionBeFlushed) {
      if (session != null) {
        try {
          session.flush();
        }
        catch (RuntimeException e) {
          return e;
        }
      }
      else {
        log.warn("no hibernate session to flush");
      }
    }
    return null;
  }

  private Exception closeSession() {
    if (mustSessionBeClosed) {
      if (session != null) {
        try {
          session.close();
        }
        catch (RuntimeException e) {
          return e;
        }
      }
      else {
        log.warn("no hibernate session to close");
      }
    }
    return null;
  }

  private Exception closeConnection() {
    if (mustConnectionBeClosed) {
      if (connection != null) {
        try {
          connection.close();
        }
        catch (RuntimeException e) {
          return e;
        }
        catch (SQLException e) {
          return e;
        }
      }
      else {
        log.warn("no jdbc connection to close");
      }
    }
    return null;
  }

  public void assignId(Object object) {
    Session session = getSession();
    if (session != null) {
      try {
        session.save(object);
      }
      catch (HibernateException e) {
        // NOTE that Errors are not caught because that might halt the JVM
        // and mask the original Error.
        throw new JbpmPersistenceException("could not assign id to " + object, e);
      }
    }
  }

  // getters and setters //////////////////////////////////////////////////////

  public GraphSession getGraphSession() {
    if (graphSession == null) {
      Session session = getSession();
      if (session != null) graphSession = new GraphSession(session);
    }
    return graphSession;
  }

  public LoggingSession getLoggingSession() {
    if (loggingSession == null) {
      Session session = getSession();
      if (session != null) loggingSession = new LoggingSession(session);
    }
    return loggingSession;
  }

  public JobSession getJobSession() {
    if (jobSession == null) {
      Session session = getSession();
      if (session != null) jobSession = new JobSession(session);
    }
    return jobSession;
  }

  public ContextSession getContextSession() {
    if (contextSession == null) {
      Session session = getSession();
      if (session != null) contextSession = new ContextSession(session);
    }
    return contextSession;
  }

  public TaskMgmtSession getTaskMgmtSession() {
    if (taskMgmtSession == null) {
      Session session = getSession();
      if (session != null) taskMgmtSession = new TaskMgmtSession(session);
    }
    return taskMgmtSession;
  }

  public Object getCustomSession(Class sessionClass) {
    if (customSessions == null) {
      customSessions = new IdentityHashMap();
    }
    else {
      Object customSession = customSessions.get(sessionClass);
      if (customSession != null) return customSession;
    }
    try {
      Constructor constructor = sessionClass.getConstructor(new Class[] {
        Session.class
      });
      try {
        Object customSession = constructor.newInstance(new Object[] {
          session
        });
        customSessions.put(sessionClass, customSession);
        return customSession;
      }
      catch (InstantiationException e) {
        throw new JbpmException(sessionClass + " is not instantiable", e);
      }
      catch (IllegalAccessException e) {
        throw new JbpmException(getClass() + " has no access to " + constructor, e);
      }
      catch (InvocationTargetException e) {
        throw new JbpmException(constructor + " threw exception", e.getCause());
      }
    }
    catch (NoSuchMethodException e) {
      throw new JbpmException("constructor not found: " + sessionClass.getName() + '('
        + Session.class.getName() + ')', e);
    }
  }

  public DataSource getDataSource() {
    return persistenceServiceFactory.getDataSource();
  }

  /**
   * @deprecated use {@link TxService#isRollbackOnly()} instead
   */
  public boolean isRollbackOnly() {
    return isTransactionRollbackOnly();
  }

  /**
   * @deprecated use {@link TxService#setRollbackOnly()} instead
   */
  public void setRollbackOnly() {
    TxService txService = getTxService();
    if (txService != null) txService.setRollbackOnly();
  }

  /**
   * @deprecated use {@link TxService#setRollbackOnly()} instead
   * @throws IllegalArgumentException if <code>rollbackOnly</code> is <code>false</code>
   */
  public void setRollbackOnly(boolean rollbackOnly) {
    if (!rollbackOnly) {
      throw new IllegalArgumentException("cannot unmark transaction for rollback");
    }
    setRollbackOnly();
  }

  private static TxService getTxService() {
    return (TxService) Services.getCurrentService(Services.SERVICENAME_TX, false);
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
   * @deprecated call {@link #setSession(Session, boolean) setSession(session, true)} instead
   */
  public void setSessionWithoutDisablingTx(Session session) {
    setSession(session, true);
  }

  /**
   * Injects an external Hibernate session. Injecting a session normally disables transaction
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
   * <td>any</td>
   * <td>false (no change)</td>
   * </tr>
   * </table>
   */
  public void setSession(Session session, boolean keepTransactionEnabled) {
    this.session = session;
    if (isTransactionEnabled && !keepTransactionEnabled) isTransactionEnabled = false;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public void setContextSession(ContextSession contextSession) {
    this.contextSession = contextSession;
  }

  public void setDataSource(DataSource dataSource) {
    persistenceServiceFactory.setDataSource(dataSource);
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
    persistenceServiceFactory.setSessionFactory(sessionFactory);
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

  /**
   * Tells whether the given exception or any of its causes is a persistence exception.
   * Currently a <em>persistence exception</em> is an instance of {@link HibernateException}.
   */
  public static boolean isPersistenceException(Exception exception) {
    for (Throwable t = exception; t != null; t = t.getCause()) {
      if (t instanceof HibernateException) return true;
    }
    return false;
  }

  /**
   * Tells whether the given exception or any of its causes is a locking exception. Currently a
   * <em>locking exception</em> is an instance of {@link StaleStateException} or
   * {@link LockAcquisitionException}. Note that Hibernate dialects have limited ability to map
   * native locking error codes to {@link LockAcquisitionException}s.
   */
  public static boolean isLockingException(Exception exception) {
    for (Throwable t = exception; t != null; t = t.getCause()) {
      if (t instanceof StaleStateException || t instanceof LockAcquisitionException) {
        return true;
      }
    }
    return false;
  }

  private static final Log log = LogFactory.getLog(DbPersistenceService.class);
}
