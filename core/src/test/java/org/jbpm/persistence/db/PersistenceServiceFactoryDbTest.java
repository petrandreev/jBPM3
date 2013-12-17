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

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.exe.Token;
import org.jbpm.mock.Jdbc;
import org.jbpm.mock.Jndi;

public class PersistenceServiceFactoryDbTest extends AbstractJbpmTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    Jndi.initialize();
  }

  protected void tearDown() throws Exception {
    Jndi.reset();
    super.tearDown();
  }

  public void testDefaults() {
    DbPersistenceServiceFactory persistenceServiceFactory = new DbPersistenceServiceFactory();
    SessionFactory sessionFactory = persistenceServiceFactory.getSessionFactory();
    assertNotNull(sessionFactory);
    assertNotNull(sessionFactory.getClassMetadata(Token.class));
  }

  public void testJndiDataSource() throws Exception {
    DataSource dataSource = new Jdbc.MockDataSource();
    Jndi.putInJndi("java:/jdbc/testDataSource", dataSource);

    DbPersistenceServiceFactory persistenceServiceFactory = new DbPersistenceServiceFactory();
    persistenceServiceFactory.dataSourceJndiName = "java:/jdbc/testDataSource";
    assertSame(dataSource, persistenceServiceFactory.getDataSource());
  }

  public void testJndiSessionFactory() throws Exception {
    SessionFactory sessionFactory = createSimplestSessionFactory();
    Jndi.putInJndi("java:/hibernate/testSessionFactory", sessionFactory);

    DbPersistenceServiceFactory persistenceServiceFactory = new DbPersistenceServiceFactory();
    persistenceServiceFactory.sessionFactoryJndiName = "java:/hibernate/testSessionFactory";
    assertSame(sessionFactory, persistenceServiceFactory.getSessionFactory());
  }

  private SessionFactory createSimplestSessionFactory() {
    Configuration configuration = new Configuration();
    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    SessionFactory sessionFactory = configuration.buildSessionFactory();
    return sessionFactory;
  }

}
