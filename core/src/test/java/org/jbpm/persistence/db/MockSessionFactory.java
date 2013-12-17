package org.jbpm.persistence.db;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.TransactionManager;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.StatelessSession;
import org.hibernate.cache.Cache;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.classic.Session;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.query.QueryPlanCache;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.StatisticsImplementor;
import org.hibernate.type.Type;

public class MockSessionFactory implements SessionFactoryImplementor {

  private Settings settings;
  private boolean failOnFlush;
  private boolean failOnClose;
  private boolean isClosed;

  private static final long serialVersionUID = 1L;

  public MockSessionFactory() {
    Configuration configuration = new Configuration();
    configuration.configure();
    settings = configuration.buildSettings();
  }

  public void setFailOnFlush(boolean fail) {
    failOnFlush = fail;
  }

  public void setFailOnClose(boolean fail) {
    failOnClose = fail;
  }

  public Session openSession(Connection connection) {
    MockSession session = new MockSession(this, connection);
    session.setFailOnFlush(failOnFlush);
    session.setFailOnClose(failOnClose);
    return session;
  }

  public Session openSession() throws HibernateException {
    MockSession session = new MockSession(this);
    session.setFailOnFlush(failOnFlush);
    session.setFailOnClose(failOnClose);
    return session;
  }

  public Session getCurrentSession() throws HibernateException {
    return null;
  }

  ////////////////////////////

  public void close() throws HibernateException {
    isClosed = true;
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
    return isClosed;
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

  public Map getAllSecondLevelCacheRegions() {
    throw new UnsupportedOperationException();
  }

  public CollectionPersister getCollectionPersister(String role) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public Set getCollectionRolesByEntityParticipant(String entityName) {
    throw new UnsupportedOperationException();
  }

  public ConnectionProvider getConnectionProvider() {
    throw new UnsupportedOperationException();
  }

  public Dialect getDialect() {
    throw new UnsupportedOperationException();
  }

  public EntityNotFoundDelegate getEntityNotFoundDelegate() {
    throw new UnsupportedOperationException();
  }

  public EntityPersister getEntityPersister(String entityName) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
    throw new UnsupportedOperationException();
  }

  public String[] getImplementors(String className) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public String getImportedClassName(String name) {
    throw new UnsupportedOperationException();
  }

  public Interceptor getInterceptor() {
    throw new UnsupportedOperationException();
  }

  public NamedQueryDefinition getNamedQuery(String queryName) {
    throw new UnsupportedOperationException();
  }

  public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
    throw new UnsupportedOperationException();
  }

  public QueryCache getQueryCache() {
    throw new UnsupportedOperationException();
  }

  public QueryCache getQueryCache(String regionName) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public QueryPlanCache getQueryPlanCache() {
    throw new UnsupportedOperationException();
  }

  public ResultSetMappingDefinition getResultSetMapping(String name) {
    throw new UnsupportedOperationException();
  }

  public String[] getReturnAliases(String queryString) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Type[] getReturnTypes(String queryString) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public SQLExceptionConverter getSQLExceptionConverter() {
    throw new UnsupportedOperationException();
  }

  public Cache getSecondLevelCacheRegion(String regionName) {
    throw new UnsupportedOperationException();
  }

  public Settings getSettings() {
    return settings;
  }

  public SQLFunctionRegistry getSqlFunctionRegistry() {
    throw new UnsupportedOperationException();
  }

  public StatisticsImplementor getStatisticsImplementor() {
    throw new UnsupportedOperationException();
  }

  public TransactionManager getTransactionManager() {
    throw new UnsupportedOperationException();
  }

  public UpdateTimestampsCache getUpdateTimestampsCache() {
    throw new UnsupportedOperationException();
  }

  public Session openSession(Connection connection, boolean flushBeforeCompletionEnabled,
    boolean autoCloseSessionEnabled, ConnectionReleaseMode connectionReleaseMode)
    throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Session openTemporarySession() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public String getIdentifierPropertyName(String className) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public Type getIdentifierType(String className) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public Type getReferencedPropertyType(String className, String propertyName)
    throws MappingException {
    throw new UnsupportedOperationException();
  }
}
