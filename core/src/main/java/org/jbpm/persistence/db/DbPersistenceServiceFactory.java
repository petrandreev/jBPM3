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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.db.JbpmSchema;
import org.jbpm.db.hibernate.HibernateHelper;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.util.JndiUtil;

public class DbPersistenceServiceFactory implements ServiceFactory {

  private static final long serialVersionUID = 1L;

  private Configuration configuration;

  String sessionFactoryJndiName;
  private SessionFactory sessionFactory;

  String dataSourceJndiName;
  private DataSource dataSource;

  boolean isTransactionEnabled = true;
  boolean isCurrentSessionEnabled;

  boolean mustSessionFactoryBeClosed;

  /** @deprecated replaced by {@link #jbpmSchema} */
  private SchemaExport schemaExport;
  private JbpmSchema jbpmSchema;

  public Service openService() {
    return new DbPersistenceService(this);
  }

  public synchronized Configuration getConfiguration() {
    if (configuration == null) {
      String hibernateCfgXmlResource = null;
      if (Configs.hasObject("resource.hibernate.cfg.xml")) {
        hibernateCfgXmlResource = Configs.getString("resource.hibernate.cfg.xml");
      }
      String hibernatePropertiesResource = null;
      if (Configs.hasObject("resource.hibernate.properties")) {
        hibernatePropertiesResource = Configs.getString("resource.hibernate.properties");
      }
      configuration = HibernateHelper.createConfiguration(hibernateCfgXmlResource, hibernatePropertiesResource);
    }
    return configuration;
  }

  /**
   * @deprecated use {@link #getJbpmSchema()} instead
   */
  public synchronized SchemaExport getSchemaExport() {
    if (schemaExport == null) {
      schemaExport = new SchemaExport(getConfiguration());
    }
    return schemaExport;
  }

  public synchronized JbpmSchema getJbpmSchema() {
    if (jbpmSchema == null) {
      jbpmSchema = new JbpmSchema(getConfiguration());
    }
    return jbpmSchema;
  }

  public synchronized SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      if (sessionFactoryJndiName != null) {
        sessionFactory = (SessionFactory) JndiUtil.lookup(sessionFactoryJndiName, SessionFactory.class);
        mustSessionFactoryBeClosed = false;
      }
      else {
        sessionFactory = getConfiguration().buildSessionFactory();
        mustSessionFactoryBeClosed = true;
      }
    }
    return sessionFactory;
  }

  public DataSource getDataSource() {
    if (dataSource == null && dataSourceJndiName != null) {
      dataSource = (DataSource) JndiUtil.lookup(dataSourceJndiName, DataSource.class);
    }
    return dataSource;
  }

  public void cleanSchema() {
    getJbpmSchema().cleanSchema();
    HibernateHelper.clearHibernateCache(getSessionFactory());
  }

  public void createSchema() {
    getJbpmSchema().createSchema();
    HibernateHelper.clearHibernateCache(getSessionFactory());
  }

  public void dropSchema() {
    getJbpmSchema().dropSchema();
    HibernateHelper.clearHibernateCache(getSessionFactory());
  }

  boolean getScript() {
    boolean script = false;
    String showSql = getConfiguration().getProperty(Environment.SHOW_SQL);
    if ("true".equalsIgnoreCase(showSql)) {
      script = true;
    }
    return script;
  }

  public void close() {
    if (mustSessionFactoryBeClosed) {
      if (sessionFactory != null) {
        sessionFactory.close();
      }
      else {
        Log log = LogFactory.getLog(DbPersistenceServiceFactory.class);
        log.warn("no session factory to close");
      }
    }
  }

  public String getDataSourceJndiName() {
    return dataSourceJndiName;
  }

  public void setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
  }

  public String getSessionFactoryJndiName() {
    return sessionFactoryJndiName;
  }

  public void setSessionFactoryJndiName(String sessionFactoryJndiName) {
    this.sessionFactoryJndiName = sessionFactoryJndiName;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void setSchemaExport(SchemaExport schemaExport) {
    this.schemaExport = schemaExport;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
    mustSessionFactoryBeClosed = false;
  }

  public boolean isTransactionEnabled() {
    return isTransactionEnabled;
  }

  public void setTransactionEnabled(boolean isTransactionEnabled) {
    this.isTransactionEnabled = isTransactionEnabled;
  }

  public boolean isCurrentSessionEnabled() {
    return isCurrentSessionEnabled;
  }

  public void setCurrentSessionEnabled(boolean isCurrentSessionEnabled) {
    this.isCurrentSessionEnabled = isCurrentSessionEnabled;
  }

}
