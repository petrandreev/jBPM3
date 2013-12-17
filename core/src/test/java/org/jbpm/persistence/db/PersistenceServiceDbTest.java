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
import java.util.List;

import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.mock.Invocation;
import org.jbpm.mock.Jdbc;
import org.jbpm.mock.Recorded;
import org.jbpm.svc.Services;

public class PersistenceServiceDbTest extends AbstractJbpmTestCase {

  private JbpmConfiguration jbpmConfiguration;

  protected void setUp() throws Exception {
    super.setUp();
    // pristine, unshared configuration object
    jbpmConfiguration = JbpmConfiguration.parseResource("jbpm.cfg.xml");
  }

  protected void tearDown() throws Exception {
    jbpmConfiguration.close();
    super.tearDown();
  }

  public void testDefaults() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    DbPersistenceServiceFactory persistenceServiceFactory;
    DbPersistenceService persistenceService;

    try {
      persistenceServiceFactory = (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
      persistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();

      assertNotNull(persistenceService);
      assertSame(persistenceServiceFactory, persistenceService.persistenceServiceFactory);

      assertNull(persistenceServiceFactory.getDataSource());
      assertNull(persistenceServiceFactory.dataSourceJndiName);
      assertNull(persistenceServiceFactory.sessionFactoryJndiName);
      assertNull(persistenceService.session);
      assertNull(persistenceService.transaction);
      assertNull(persistenceService.connection);
      assertNull(persistenceService.contextSession);
      assertNull(persistenceService.graphSession);
      assertNull(persistenceService.loggingSession);
      assertNull(persistenceService.jobSession);
      assertNull(persistenceService.taskMgmtSession);

      Session session = persistenceService.getSession();

      assertSame(session, persistenceService.session);
      assertNull(persistenceService.connection);

      assertNotNull(persistenceServiceFactory.getConfiguration());
      assertNotNull(persistenceServiceFactory.getSessionFactory());
      assertNotNull(persistenceService.transaction);
      assertNull(persistenceService.connection);
      assertNotNull(persistenceService.session);

      assertNull(persistenceServiceFactory.getDataSource());
      assertNull(persistenceServiceFactory.dataSourceJndiName);
      assertNull(persistenceServiceFactory.sessionFactoryJndiName);
      assertNull(persistenceService.contextSession);
      assertNull(persistenceService.graphSession);
      assertNull(persistenceService.loggingSession);
      assertNull(persistenceService.jobSession);
      assertNull(persistenceService.taskMgmtSession);

      assertTrue(persistenceService.transaction.isActive());
      assertTrue(persistenceService.session.isOpen());
      assertFalse(persistenceService.transaction.wasCommitted());
      assertFalse(persistenceService.transaction.wasRolledBack());
    }
    finally {
      jbpmContext.close();
    }

    assertNotNull(persistenceServiceFactory.getConfiguration());
    assertNotNull(persistenceServiceFactory.getSessionFactory());

    assertTrue(persistenceService.transaction.wasCommitted());
    assertFalse(persistenceService.transaction.isActive());
    assertFalse(persistenceService.transaction.wasRolledBack());
    assertFalse(persistenceService.session.isOpen());
    assertFalse(persistenceService.session.isConnected());
  }

  public void testRollbackWithoutSessionCreation() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    DbPersistenceService persistenceService;

    try {
      persistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();

      jbpmContext.setRollbackOnly();
    }
    finally {
      jbpmContext.close();
    }
    assertNull(persistenceService.transaction);
  }

  public void testRollback() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    DbPersistenceService persistenceService;

    try {
      persistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();

      persistenceService.getSession();
      jbpmContext.setRollbackOnly();
    }
    finally {
      jbpmContext.close();
    }

    assertFalse(persistenceService.transaction.wasCommitted());
    assertFalse(persistenceService.transaction.isActive());
    assertTrue(persistenceService.transaction.wasRolledBack());
    assertFalse(persistenceService.session.isOpen());
  }

  public void testUserSuppliedConnection() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    Recorded recordedConnection;

    try {
      DbPersistenceService persistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();

      DataSource dataSource = Jdbc.createRecordedDataSource();
      Connection connection = dataSource.getConnection();
      persistenceService.setConnection(connection);

      Session session = persistenceService.getSession();

      assertNotNull(persistenceService.transaction);
      assertSame(session, persistenceService.session);
      recordedConnection = (Recorded) connection;
      List invocations = recordedConnection.getInvocations();
      assertNull(Invocation.getInvocation(invocations, "commit", 0));
    }
    finally {
      jbpmContext.close();
    }

    List invocations = recordedConnection.getInvocations();
    assertNotNull(Invocation.getInvocation(invocations, "commit", 0));
    assertNull(Invocation.getInvocation(invocations, "close", 0));
  }

  public void testUserSuppliedConnectionWithRollback() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    Recorded recordedConnection;

    try {
      DbPersistenceService persistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();

      DataSource dataSource = Jdbc.createRecordedDataSource();
      Connection connection = dataSource.getConnection();
      persistenceService.setConnection(connection);

      Session session = persistenceService.getSession();
      jbpmContext.setRollbackOnly();

      assertNotNull(persistenceService.transaction);
      assertSame(session, persistenceService.session);
      recordedConnection = (Recorded) connection;
      List invocations = recordedConnection.getInvocations();
      assertNull(Invocation.getInvocation(invocations, "commit", 0));
      assertNull(Invocation.getInvocation(invocations, "rollback", 0));
    }
    finally {
      jbpmContext.close();
    }

    List invocations = recordedConnection.getInvocations();
    assertNull(Invocation.getInvocation(invocations, "commit", 0));
    assertNotNull(Invocation.getInvocation(invocations, "rollback", 0));
  }

  public void testUserSuppliedSession() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    DbPersistenceService persistenceService;

    try {
      persistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();

      DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
      SessionFactory sessionFactory = persistenceServiceFactory.getSessionFactory();
      Session session = sessionFactory.openSession();

      jbpmContext.setSession(session);

      persistenceService.getSession();
      assertNull(persistenceService.transaction);
    }
    finally {
      jbpmContext.close();
    }

    assertNull(persistenceService.transaction);
    assertNotNull(persistenceService.session);
    assertTrue(persistenceService.session.isOpen());
  }

  public void testUserSuppliedSessionWithRollback() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
      SessionFactory sessionFactory = persistenceServiceFactory.getSessionFactory();

      DataSource dataSource = Jdbc.createRecordedDataSource();
      Connection connection = dataSource.getConnection();
      Session session = sessionFactory.openSession(connection);

      jbpmContext.setSession(session);
      jbpmContext.setRollbackOnly();
    }
    finally {
      try {
        jbpmContext.close();
        fail("expected exception");
      }
      catch (JbpmException e) {
        // OK
      }
    }
  }

  public void testTransferResponsibility() throws Exception {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();

    DbPersistenceService dbPersistenceService = null;
    Connection connection;
    try {
      dbPersistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();

      connection = jbpmContext.getConnection();
    }
    finally {
      jbpmContext.close();
    }

    assertFalse(dbPersistenceService.session.isOpen());
    assertFalse(dbPersistenceService.session.isConnected());

    connection.close();
  }
}
