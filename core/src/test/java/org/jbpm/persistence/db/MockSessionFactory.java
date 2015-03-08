package org.jbpm.persistence.db;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.TransactionManager;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.NamedQueryRepository;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class MockSessionFactory implements SessionFactoryImplementor {

  private Settings settings;
  private boolean failOnFlush;
  private boolean failOnClose;
  private boolean isClosed;

  private static final long serialVersionUID = 1L;

  public MockSessionFactory() {
    Configuration configuration = new Configuration();
    configuration.configure();
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties())
      .build();
    settings = configuration.buildSettings(serviceRegistry);
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

  // //////////////////////////

  
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

  // <<-------- Hibernate 3.6.10 methods --------//

  
  public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
    throw new UnsupportedOperationException();
  }

  
  public org.hibernate.Cache getCache() {
    throw new UnsupportedOperationException();
  }

  
  public boolean containsFetchProfileDefinition(String name) {
    throw new UnsupportedOperationException();
  }

  
  public TypeHelper getTypeHelper() {
    throw new UnsupportedOperationException();
  }

  
  public TypeResolver getTypeResolver() {
    throw new UnsupportedOperationException();
  }

  
  public Properties getProperties() {
    throw new UnsupportedOperationException();
  }

  
  public Region getSecondLevelCacheRegion(String regionName) {
    throw new UnsupportedOperationException();
  }

  public FetchProfile getFetchProfile(String name) {
    throw new UnsupportedOperationException();
  }

  public SessionFactoryObserver getFactoryObserver() {
    throw new UnsupportedOperationException();
  }

  // -------- Hibernate 3.6.10 methods -------->>//

  // <<-------- Hibernate 4.3.8 methods --------//

  public SessionFactoryOptions getSessionFactoryOptions() {
    throw new UnsupportedOperationException();
  }

  public StatelessSessionBuilder withStatelessOptions() {
    throw new UnsupportedOperationException();
  }

  public SessionBuilderImplementor withOptions() {
    return new MockSessionBuilder(this);
  }

  public Map<String, EntityPersister> getEntityPersisters() {
    throw new UnsupportedOperationException();
  }

  public Map<String, CollectionPersister> getCollectionPersisters() {
    throw new UnsupportedOperationException();
  }

  public JdbcServices getJdbcServices() {
    throw new UnsupportedOperationException();
  }

  public void registerNamedQueryDefinition(String name, NamedQueryDefinition definition) {
    throw new UnsupportedOperationException();
  }

  public void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition) {
    throw new UnsupportedOperationException();
  }

  public Region getNaturalIdCacheRegion(String regionName) {
    throw new UnsupportedOperationException();
  }

  public SqlExceptionHelper getSQLExceptionHelper() {
    throw new UnsupportedOperationException();
  }

  public ServiceRegistryImplementor getServiceRegistry() {
    throw new UnsupportedOperationException();
  }

  public void addObserver(SessionFactoryObserver observer) {
    throw new UnsupportedOperationException();
  }

  public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
    throw new UnsupportedOperationException();
  }

  public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
    throw new UnsupportedOperationException();
  }

  public NamedQueryRepository getNamedQueryRepository() {
    throw new UnsupportedOperationException();
  }

  public Iterable<EntityNameResolver> iterateEntityNameResolvers() {
    throw new UnsupportedOperationException();
  }
  // -------- Hibernate 4.3.8 methods -------->>//
}
