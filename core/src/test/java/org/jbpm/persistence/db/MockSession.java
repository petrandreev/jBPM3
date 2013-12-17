package org.jbpm.persistence.db;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

public class MockSession implements org.hibernate.classic.Session {
  
  private static final long serialVersionUID = 1L;

  final Connection connection;
  final SessionFactory sessionFactory;

  MockTransaction transaction;
  boolean isFlushed;
  boolean isClosed;

  boolean failOnFlush;
  boolean failOnClose;

  public MockSession(SessionFactory sessionFactory) {
    this(sessionFactory, null);
  }

  public MockSession(SessionFactory sessionFactory, Connection connection) {
    this.connection = connection;
    this.sessionFactory = sessionFactory;
  }

  public void setFailOnFlush(boolean fail) {
    failOnFlush = fail;
  }

  public void setFailOnClose(boolean fail) {
    failOnClose = fail;
  }

  public Transaction beginTransaction() throws HibernateException {
    transaction = new MockTransaction();
    return transaction;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public Connection connection() throws HibernateException {
    return connection;
  }

  public Connection close() throws HibernateException {
    if (failOnClose)
      throw new HibernateException("simulated close exception");

    isClosed = true;
    return connection;
  }

  public void flush() throws HibernateException {
    if (failOnFlush) 
      throw new HibernateException("simulated flush exception");

    isFlushed = true;
  }

  public boolean isOpen() {
    return ! isClosed;
  }

  public EntityMode getEntityMode() {
    throw new UnsupportedOperationException();
  }

  public org.hibernate.Session getSession(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public void setFlushMode(FlushMode flushMode) {
    throw new UnsupportedOperationException();
  }

  public FlushMode getFlushMode() {
    throw new UnsupportedOperationException();
  }

  public void setCacheMode(CacheMode cacheMode) {
    throw new UnsupportedOperationException();
  }

  public CacheMode getCacheMode() {
    throw new UnsupportedOperationException();
  }

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public void cancelQuery() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isConnected() {
    throw new UnsupportedOperationException();
  }

  public boolean isDirty() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Serializable getIdentifier(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean contains(Object object) {
    throw new UnsupportedOperationException();
  }

  public void evict(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object load(Class theClass, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object load(String entityName, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void load(Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Serializable save(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Serializable save(String entityName, Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void saveOrUpdate(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void saveOrUpdate(String entityName, Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void update(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void update(String entityName, Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object merge(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object merge(String entityName, Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void persist(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void persist(String entityName, Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void delete(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void delete(String entityName, Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void lock(Object object, LockMode lockMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void refresh(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void refresh(Object object, LockMode lockMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public LockMode getCurrentLockMode(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(Class persistentClass) {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(Class persistentClass, String alias) {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(String entityName) {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(String entityName, String alias) {
    throw new UnsupportedOperationException();
  }

  public Query createQuery(String queryString) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public SQLQuery createSQLQuery(String queryString) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query createFilter(Object collection, String queryString) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query getNamedQuery(String queryName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public Object get(Class clazz, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object get(String entityName, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public String getEntityName(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Filter enableFilter(String filterName) {
    throw new UnsupportedOperationException();
  }

  public Filter getEnabledFilter(String filterName) {
    throw new UnsupportedOperationException();
  }

  public void disableFilter(String filterName) {
    throw new UnsupportedOperationException();
  }

  public SessionStatistics getStatistics() {
    throw new UnsupportedOperationException();
  }

  public void setReadOnly(Object entity, boolean readOnly) {
    throw new UnsupportedOperationException();
  }

  public Connection disconnect() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void reconnect() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void reconnect(Connection connection) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object saveOrUpdateCopy(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object saveOrUpdateCopy(Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object saveOrUpdateCopy(String entityName, Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object saveOrUpdateCopy(String entityName, Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public List find(String query) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public List find(String query, Object value, Type type) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public List find(String query, Object[] values, Type[] types) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Iterator iterate(String query) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Iterator iterate(String query, Object value, Type type) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Iterator iterate(String query, Object[] values, Type[] types) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Collection filter(Object collection, String filter) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Collection filter(Object collection, String filter, Object value, Type type) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Collection filter(Object collection, String filter, Object[] values, Type[] types) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int delete(String query) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int delete(String query, Object value, Type type) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int delete(String query, Object[] values, Type[] types) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Query createSQLQuery(String sql, String returnAlias, Class returnClass) {
    throw new UnsupportedOperationException();
  }

  public Query createSQLQuery(String sql, String[] returnAliases, Class[] returnClasses) {
    throw new UnsupportedOperationException();
  }

  public void save(Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void save(String entityName, Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void update(Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void update(String entityName, Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }
}
