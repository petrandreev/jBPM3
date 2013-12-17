package org.jbpm.persistence.db;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.classic.Session;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

public class MockSessionFactory implements SessionFactory {

  private boolean failOnFlush;
  private boolean failOnClose;

  private static final long serialVersionUID = 1L;

  public void setFailOnFlush(boolean fail) {
    failOnFlush = fail;
  }

  public void setFailOnClose(boolean fail) {
    failOnClose = fail;
  }

  public Session openSession(Connection connection) {
    MockSession session = new MockSession(connection);
    session.setFailOnFlush(failOnFlush);
    session.setFailOnClose(failOnClose);
    return session;
  }

  public Session openSession() throws HibernateException {
    MockSession session = new MockSession();
    session.setFailOnFlush(failOnFlush);
    session.setFailOnClose(failOnClose);
    return session;
  }

  public Session getCurrentSession() throws HibernateException {
    return null;
  }
  
  ////////////////////////////

  public void close() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Session openSession(Interceptor interceptor) throws HibernateException {
    throw new UnsupportedOperationException();
  }
  public Session openSession(Connection connection, Interceptor interceptor) {
    throw new UnsupportedOperationException();
  }

  public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Map getAllClassMetadata() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Map getAllCollectionMetadata() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Statistics getStatistics() {
    throw new UnsupportedOperationException();
  }

  public boolean isClosed() {
    throw new UnsupportedOperationException();
  }

  public void evict(Class persistentClass) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evict(Class persistentClass, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictEntity(String entityName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictEntity(String entityName, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictCollection(String roleName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictCollection(String roleName, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictQueries() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void evictQueries(String cacheRegion) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public StatelessSession openStatelessSession() {
    throw new UnsupportedOperationException();
  }

  public StatelessSession openStatelessSession(Connection connection) {
    throw new UnsupportedOperationException();
  }

  public Set getDefinedFilterNames() {
    throw new UnsupportedOperationException();
  }

  public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Reference getReference() throws NamingException {
    throw new UnsupportedOperationException();
  }
}
