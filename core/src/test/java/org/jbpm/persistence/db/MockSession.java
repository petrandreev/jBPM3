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
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.jdbc.Work;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
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

    @Override
    public Transaction beginTransaction() throws HibernateException {
        transaction = new MockTransaction();
        return transaction;
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public Connection connection() throws HibernateException {
        return connection;
    }

    @Override
    public Connection close() throws HibernateException {
        if (failOnClose)
            throw new HibernateException("simulated close exception");

        isClosed = true;
        return connection;
    }

    @Override
    public void flush() throws HibernateException {
        if (failOnFlush)
            throw new HibernateException("simulated flush exception");

        isFlushed = true;
    }

    @Override
    public boolean isOpen() {
        return !isClosed;
    }

    @Override
    public EntityMode getEntityMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public org.hibernate.Session getSession(EntityMode entityMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFlushMode(FlushMode flushMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlushMode getFlushMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheMode getCacheMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public void cancelQuery() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirty() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getIdentifier(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evict(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(Class theClass, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(String entityName, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void load(Object object, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable save(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable save(String entityName, Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveOrUpdate(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveOrUpdate(String entityName, Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(String entityName, Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object merge(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object merge(String entityName, Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(String entityName, Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String entityName, Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lock(Object object, LockMode lockMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(Object object, LockMode lockMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockMode getCurrentLockMode(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(Class persistentClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(Class persistentClass, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String entityName, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query createQuery(String queryString) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery createSQLQuery(String queryString) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query createFilter(Object collection, String queryString) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query getNamedQuery(String queryName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Class clazz, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String entityName, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEntityName(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Filter enableFilter(String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableFilter(String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionStatistics getStatistics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(Object entity, boolean readOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection disconnect() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reconnect() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reconnect(Connection connection) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object saveOrUpdateCopy(Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object saveOrUpdateCopy(Object object, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object saveOrUpdateCopy(String entityName, Object object) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object saveOrUpdateCopy(String entityName, Object object, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List find(String query) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List find(String query, Object value, Type type) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List find(String query, Object[] values, Type[] types) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator iterate(String query) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator iterate(String query, Object value, Type type) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator iterate(String query, Object[] values, Type[] types) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection filter(Object collection, String filter) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection filter(Object collection, String filter, Object value, Type type) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection filter(Object collection, String filter, Object[] values, Type[] types) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(String query) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(String query, Object value, Type type) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(String query, Object[] values, Type[] types) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query createSQLQuery(String sql, String returnAlias, Class returnClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query createSQLQuery(String sql, String[] returnAliases, Class[] returnClasses) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(Object object, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(String entityName, Object object, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(Object object, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(String entityName, Object object, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    // <<-------- Hibernate 3.6.10 methods --------//

    @Override
    public boolean isDefaultReadOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultReadOnly(boolean readOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(Class theClass, Serializable id, LockOptions lockOptions) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockRequest buildLockRequest(LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(Object object, LockOptions lockOptions) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Class clazz, Serializable id, LockOptions lockOptions) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly(Object entityOrProxy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableFetchProfile(String name) throws UnknownProfileException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableFetchProfile(String name) throws UnknownProfileException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeHelper getTypeHelper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LobHelper getLobHelper() {
        throw new UnsupportedOperationException();
    }
    // -------- Hibernate 3.6.10 methods -------->>//
}
