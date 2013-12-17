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
package org.jbpm.db.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.jbpm.util.ClassLoaderUtil;

public class HibernateHelper {

  private HibernateHelper() {
    // prevent instantiation
  }

  /** maps SessionFactory's to Configurations.
   * by default, configuration lookup will be enabled */
  static Map configurations = new HashMap();

  public static void clearConfigurationsCache() {
    configurations.clear();
  }

  public static SessionFactory createSessionFactory() {
    return createSessionFactory(null, null, true);
  }

  public static SessionFactory createSessionFactory(String cfgXmlResource) {
    return createSessionFactory(cfgXmlResource, null, true);
  }

  public static SessionFactory createSessionFactory(String cfgXmlResource, String propertiesResource) {
    return createSessionFactory(cfgXmlResource, propertiesResource, true);
  }

  public static SessionFactory createSessionFactory(String cfgXmlResource,
      String propertiesResource, boolean isConfigLookupEnabled) {
    Configuration configuration = createConfiguration(cfgXmlResource, propertiesResource);
    return createSessionFactory(configuration, isConfigLookupEnabled);
  }

  public static SessionFactory createSessionFactory(Configuration configuration,
      boolean isConfigLookupEnabled) {
    SessionFactory sessionFactory = configuration.buildSessionFactory();
    if (isConfigLookupEnabled) {
      configurations.put(sessionFactory, configuration);
    }
    return sessionFactory;
  }

  public static Configuration createConfiguration(String cfgXmlResource, String propertiesResource) {
    Configuration configuration = new Configuration();

    // if a special hibernate configuration xml file is specified,
    if (cfgXmlResource != null) {
      // use the configured file name
      URL cfgURL = Thread.currentThread().getContextClassLoader().getResource(cfgXmlResource);
      log.debug("creating hibernate configuration resource '" + cfgURL + "'");
      configuration.configure(cfgXmlResource);
    } else {
      log.debug("using default hibernate configuration resource (hibernate.cfg.xml)");
      configuration.configure();
    }

    // if the properties are specified in a separate file
    if (propertiesResource != null) {
      log.debug("using hibernate properties from resource '" + propertiesResource + "'");
      // load the properties
      Properties properties = loadPropertiesFromResource(propertiesResource);
      if (!properties.isEmpty()) {
        // and overwrite the properties with the specified properties
        configuration.setProperties(properties);
      }
    }

    return configuration;
  }

  public static Configuration getConfiguration(SessionFactory sessionFactory) {
    return (Configuration) configurations.get(sessionFactory);
  }

  public static SchemaExport createSchemaExport(SessionFactory sessionFactory) {
    return new SchemaExport(getConfiguration(sessionFactory));
  }

  public static boolean createSchemaExportScript(SessionFactory sessionFactory) {
    boolean script = false;
    String showSql = getConfiguration(sessionFactory).getProperty("hibernate.show_sql");
    if ("true".equalsIgnoreCase(showSql)) {
      script = true;
    }
    return script;
  }

  public static void clearHibernateCache(SessionFactory sessionFactory) {
    sessionFactory.evictQueries();

    Map classMetadata = sessionFactory.getAllClassMetadata();
    Iterator iter = classMetadata.keySet().iterator();
    while (iter.hasNext()) {
      String entityName = (String) iter.next();
      sessionFactory.evictEntity(entityName);
    }

    Map collectionMetadata = sessionFactory.getAllCollectionMetadata();
    iter = collectionMetadata.keySet().iterator();
    while (iter.hasNext()) {
      String collectionName = (String) iter.next();
      sessionFactory.evictCollection(collectionName);
    }
  }

  static Properties loadPropertiesFromResource(String resource) {
    Properties properties = new Properties();
    InputStream inputStream = ClassLoaderUtil.getStream(resource);
    if (inputStream != null) {
      try {
        properties.load(inputStream);
      } catch (IOException e) {
        log.warn("couldn't load hibernate properties from resource '" + resource + "'", e);
      }
    } else {
      log.warn("hibernate properties resource '" + resource + "' not found");
    }
    return properties;
  }

  private static Log log = LogFactory.getLog(HibernateHelper.class);
}
