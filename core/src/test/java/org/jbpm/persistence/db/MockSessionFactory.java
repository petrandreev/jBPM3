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
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.TypeHelper;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.Region;
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
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.QueryPlanCache;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.StatisticsImplementor;
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
        settings = configuration.buildSettings();
    }

    public void setFailOnFlush(boolean fail) {
        failOnFlush = fail;
    }

    public void setFailOnClose(boolean fail) {
        failOnClose = fail;
    }

    @Override
    public Session openSession(Connection connection) {
        MockSession session = new MockSession(this, connection);
        session.setFailOnFlush(failOnFlush);
        session.setFailOnClose(failOnClose);
        return session;
    }

    @Override
    public Session openSession() throws HibernateException {
        MockSession session = new MockSession(this);
        session.setFailOnFlush(failOnFlush);
        session.setFailOnClose(failOnClose);
        return session;
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
        return null;
    }

    // //////////////////////////

    @Override
    public void close() throws HibernateException {
        isClosed = true;
    }

    @Override
    public Session openSession(Interceptor interceptor) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session openSession(Connection connection, Interceptor interceptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map getAllClassMetadata() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map getAllCollectionMetadata() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Statistics getStatistics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void evict(Class persistentClass) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evict(Class persistentClass, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictEntity(String entityName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictEntity(String entityName, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictCollection(String roleName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictCollection(String roleName, Serializable id) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictQueries() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictQueries(String cacheRegion) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatelessSession openStatelessSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatelessSession openStatelessSession(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set getDefinedFilterNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reference getReference() throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map getAllSecondLevelCacheRegions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CollectionPersister getCollectionPersister(String role) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set getCollectionRolesByEntityParticipant(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionProvider getConnectionProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dialect getDialect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityNotFoundDelegate getEntityNotFoundDelegate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityPersister getEntityPersister(String entityName) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getImplementors(String className) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getImportedClassName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Interceptor getInterceptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamedQueryDefinition getNamedQuery(String queryName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryCache getQueryCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryCache getQueryCache(String regionName) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryPlanCache getQueryPlanCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSetMappingDefinition getResultSetMapping(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getReturnAliases(String queryString) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type[] getReturnTypes(String queryString) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLExceptionConverter getSQLExceptionConverter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public SQLFunctionRegistry getSqlFunctionRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatisticsImplementor getStatisticsImplementor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionManager getTransactionManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UpdateTimestampsCache getUpdateTimestampsCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session openSession(Connection connection, boolean flushBeforeCompletionEnabled, boolean autoCloseSessionEnabled,
            ConnectionReleaseMode connectionReleaseMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session openTemporarySession() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentifierPropertyName(String className) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getIdentifierType(String className) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
        throw new UnsupportedOperationException();
    }

    // <<-------- Hibernate 3.6.10 methods --------//

    @Override
    public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public org.hibernate.Cache getCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsFetchProfileDefinition(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeHelper getTypeHelper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeResolver getTypeResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties getProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Region getSecondLevelCacheRegion(String regionName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FetchProfile getFetchProfile(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionFactoryObserver getFactoryObserver() {
        throw new UnsupportedOperationException();
    }
    // -------- Hibernate 3.6.10 methods -------->>//
}
