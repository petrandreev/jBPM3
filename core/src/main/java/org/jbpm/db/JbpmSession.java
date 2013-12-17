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
package org.jbpm.db;

import java.sql.Connection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jbpm.JbpmException;

/**
 * represents the connection to the jbpm database.
 * 
 * You can obtain a JbpmSession with
 * <pre>
 * JbpmSession jbpmSession = jbpmSessionFactory.openJbpmSession();
 * </pre>
 * or  
 * <pre>
 * Connection jdbcConnection = ...;
 * JbpmSession jbpmSession = jbpmSessionFactory.openJbpmSession(jdbcConnection);
 * </pre>  
 * The actual database operations are defined in the module sessions :
 * <ul>
 *   <li>{@link org.jbpm.db.GraphSession}</li>
 *   <li>{@link org.jbpm.db.TaskMgmtSession}</li>
 *   <li>{@link org.jbpm.db.LoggingSession}</li>
 *   <li>{@link org.jbpm.db.ContextSession}</li>
 * </ul>  
 * The easiest way to obtain the operations is like this :
 * <ul>
 *   <li><pre>jbpmSession.getGraphSession().someGraphDbMethod(...)</pre></li>
 *   <li><pre>jbpmSession.getTaskMgmtSession().someTaskDbMethod(...)</pre></li>
 *   <li><pre>jbpmSession.getLoggingSession().someLoggingDbMethod(...)</pre></li>
 *   <li><pre>jbpmSession.getContextSession().someContextDbMethod(...)</pre></li>
 * </ul>
 * 
 * @deprecated use {@link org.jbpm.JbpmContext} and {@link org.jbpm.JbpmConfiguration} instead.
 */
public class JbpmSession {
  
  static ThreadLocal currentJbpmSessionStack = new ThreadLocal();
  
  JbpmSessionFactory jbpmSessionFactory = null;
  Session session = null;
  Transaction transaction = null;
  
  GraphSession graphSession = null;
  ContextSession contextSession = null;
  TaskMgmtSession taskMgmtSession = null;
  LoggingSession loggingSession = null;
  JobSession jobSession = null;
  
  public JbpmSession( JbpmSessionFactory jbpmSessionFactory, Session session ) {
    this.jbpmSessionFactory = jbpmSessionFactory;
    this.session = session;
    this.graphSession = new GraphSession(this);
    this.contextSession = new ContextSession(this);
    this.taskMgmtSession = new TaskMgmtSession(this);
    this.loggingSession = new LoggingSession(this);
    
    pushCurrentSession();
  }
  
  public JbpmSession(Session session) {
    this.session = session;
    this.graphSession = new GraphSession(this);
    this.contextSession = new ContextSession(this);
    this.taskMgmtSession = new TaskMgmtSession(this);
    this.loggingSession = new LoggingSession(this);
  }

  public JbpmSessionFactory getJbpmSessionFactory() {
    return jbpmSessionFactory;
  }

  public Connection getConnection() {
    try {
      return session.connection();
    } catch (Exception e) {
      log.error(e);
      handleException();
      throw new JbpmException( "couldn't get the jdbc connection from hibernate", e );
    }
  }

  public Session getSession() {
    return session;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public void beginTransaction() {
    try {
      transaction = session.beginTransaction();
    } catch (Exception e) {
      log.error(e);
      handleException();
      throw new JbpmException( "couldn't begin a transaction", e );
    }
  }

  public void commitTransaction() {
    if ( transaction == null ) {
      throw new JbpmException("can't commit : no transaction started" );
    }
    try {
      session.flush();
      transaction.commit();
    } catch (Exception e) {
      log.error(e);
      handleException();
      throw new JbpmException( "couldn't commit transaction", e );
    } finally {
      transaction = null;
    }
  }

  public void rollbackTransaction() {
    if ( transaction == null ) {
      throw new JbpmException("can't rollback : no transaction started" );
    }
    try {
      transaction.rollback();
    } catch (Exception e) {
      log.error(e);
      handleException();
      throw new JbpmException( "couldn't rollback transaction", e );
    } finally {
      transaction = null;
    }
  }
  
  public void commitTransactionAndClose() {
    commitTransaction();
    close();
  }
  public void rollbackTransactionAndClose() {
    rollbackTransaction();
    close();
  }
  
  public GraphSession getGraphSession() {
    return graphSession;
  }
  public ContextSession getContextSession() {
    return contextSession;
  }
  public TaskMgmtSession getTaskMgmtSession() {
    return taskMgmtSession;
  }
  public LoggingSession getLoggingSession() {
    return loggingSession;
  }
  public JobSession getJobSession() {
    return jobSession;
  }

  public void close() {
    try {
      if ( (session!=null)
           && (session.isOpen())
         ) {
        session.close();
      }
    } catch (Exception e) {
      log.error(e);
      throw new JbpmException( "couldn't close the hibernate connection", e );
    } finally {
      popCurrentSession();
      session = null;
    }
  }

  /**
   * handles an exception that is thrown by hibernate.
   */
  void handleException() {
    // if hibernate throws an exception,  
    if (transaction!=null) {
      try {
        // the transaction should be rolled back
        transaction.rollback();
      } catch (HibernateException e) {
        log.error("couldn't rollback hibernate transaction", e);
      }
      // and the hibernate session should be closed.
      close();
    }
  }

  public void pushCurrentSession() {
    LinkedList stack = (LinkedList) currentJbpmSessionStack.get();
    if (stack==null) {
      stack = new LinkedList();
      currentJbpmSessionStack.set(stack);
    }
    stack.addFirst(this);
  }
  
  /**
   * @deprecated use {@link org.jbpm.JbpmConfiguration#getCurrentJbpmContext()} instead.
   */
  public static JbpmSession getCurrentJbpmSession() {
    JbpmSession jbpmSession = null;
    LinkedList stack = (LinkedList) currentJbpmSessionStack.get();
    if ( (stack!=null)
         && (! stack.isEmpty()) 
       ) {
      jbpmSession = (JbpmSession) stack.getFirst();
    }
    return jbpmSession;
  }

  public void popCurrentSession() {
    LinkedList stack = (LinkedList) currentJbpmSessionStack.get();
    if ( (stack==null)
         || (stack.isEmpty())
         || (stack.getFirst()!=this)
       ) {
      log.warn("can't pop current session: are you calling JbpmSession.close() multiple times ?");
    } else {
      stack.removeFirst();
    }
  }

  private static final Log log = LogFactory.getLog(JbpmSession.class);
}
