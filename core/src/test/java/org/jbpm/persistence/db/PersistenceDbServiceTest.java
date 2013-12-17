package org.jbpm.persistence.db;

import java.sql.Connection;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

public class PersistenceDbServiceTest extends AbstractJbpmTestCase {

  JbpmConfiguration jbpmConfiguration;
  JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    super.setUp();
    jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration/>");
    jbpmContext = jbpmConfiguration.createJbpmContext();
    jbpmContext.setSessionFactory(new MockSessionFactory());
  }

  protected void tearDown() throws Exception {
    jbpmConfiguration.close();
    super.tearDown();
  }

  // with hibernate transactions
  // hibernate creates connections

  public void testDefaultCommit() {
    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.close();

    assertTrue(mockSession.transaction.wasCommitted);
    assertFalse(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);
  }

  public void testDefaultRollback() {
    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.setRollbackOnly();
    jbpmContext.close();

    assertFalse(mockSession.transaction.wasCommitted);
    assertTrue(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);
  }

  public void testDefaultCommitAfterGetConnection() {
    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.getConnection();
    jbpmContext.close();

    assertTrue(mockSession.transaction.wasCommitted);
    assertFalse(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);
  }

  public void testDefaultFollbackAfterGetConnection() {
    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.setRollbackOnly();
    jbpmContext.getConnection();
    jbpmContext.close();

    assertFalse(mockSession.transaction.wasCommitted);
    assertTrue(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);
  }

  // with hibernate transactions
  // given creates connections

  public void testGivenConnectionCommit() {
    // inject given session
    MockConnectionHelper connectionHelper = new MockConnectionHelper();
    Connection mockConnection = connectionHelper.createMockConnection();
    jbpmContext.setConnection(mockConnection);

    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.close();

    assertTrue(mockSession.transaction.wasCommitted);
    assertFalse(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);

    assertFalse(connectionHelper.wasClosed);
    assertFalse(connectionHelper.wasCommitted);
    assertFalse(connectionHelper.wasRolledBack);
  }

  public void testGivenConnectionRollback() {
    // inject given session
    MockConnectionHelper connectionHelper = new MockConnectionHelper();
    Connection mockConnection = connectionHelper.createMockConnection();
    jbpmContext.setConnection(mockConnection);

    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.setRollbackOnly();
    jbpmContext.close();

    assertFalse(mockSession.transaction.wasCommitted);
    assertTrue(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);

    assertFalse(connectionHelper.wasClosed);
    assertFalse(connectionHelper.wasCommitted);
    assertFalse(connectionHelper.wasRolledBack);
  }

  public void testGivenConnectionCommitAfterGetConnection() {
    // inject given session
    MockConnectionHelper connectionHelper = new MockConnectionHelper();
    Connection mockConnection = connectionHelper.createMockConnection();
    jbpmContext.setConnection(mockConnection);

    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.getConnection();
    jbpmContext.close();

    assertTrue(mockSession.transaction.wasCommitted);
    assertFalse(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);

    assertFalse(connectionHelper.wasClosed);
    assertFalse(connectionHelper.wasCommitted);
    assertFalse(connectionHelper.wasRolledBack);
  }

  public void testGivenConnectionRollbackAfterGetConnection() {
    // inject given session
    MockConnectionHelper connectionHelper = new MockConnectionHelper();
    Connection mockConnection = connectionHelper.createMockConnection();
    jbpmContext.setConnection(mockConnection);

    MockSession mockSession = (MockSession) jbpmContext.getSession();
    assertNotNull(mockSession.transaction);
    jbpmContext.getConnection();
    jbpmContext.setRollbackOnly();
    jbpmContext.close();

    assertFalse(mockSession.transaction.wasCommitted);
    assertTrue(mockSession.transaction.wasRolledBack);
    assertTrue(mockSession.isClosed);
    assertFalse(mockSession.isFlushed);

    assertFalse(connectionHelper.wasClosed);
    assertFalse(connectionHelper.wasCommitted);
    assertFalse(connectionHelper.wasRolledBack);
  }
}
