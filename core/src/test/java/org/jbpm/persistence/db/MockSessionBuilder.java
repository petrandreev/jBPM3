package org.jbpm.persistence.db;

import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionOwner;

/**
 * Mocks the session builder.
 * @author pan
 */
public class MockSessionBuilder implements SessionBuilderImplementor {

  private SessionFactory sessionFactory;
  
  private Connection connection;

  MockSessionBuilder() {
  }

  MockSessionBuilder(SessionFactory sessionFactory) {

  }

  public Session openSession() {
    return connection != null ? new MockSession(sessionFactory, connection):new MockSession(sessionFactory);
  }

  public SessionBuilder interceptor(Interceptor interceptor) {
    return this;
  }

  public SessionBuilder noInterceptor() {
    return this;
  }

  public SessionBuilder connection(Connection connection) {
    this.connection = connection;
    return this;
  }

  public SessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
    return this;
  }

  public SessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
    return this;
  }

  public SessionBuilder autoClose(boolean autoClose) {
    return this;
  }

  public SessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
    return this;
  }

  public SessionBuilder tenantIdentifier(String tenantIdentifier) {
    return this;
  }

  public SessionBuilder eventListeners(SessionEventListener... listeners) {
    return this;
  }

  public SessionBuilder clearEventListeners() {
    return this;
  }

  public SessionBuilder owner(SessionOwner sessionOwner) {
    return this;
  }
}
