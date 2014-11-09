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
package org.jbpm.db;

import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmException;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.JndiUtil;

/**
 * creates JbpmSessions. Obtain a JbpmSessionFactory with
 * 
 * <pre>
 * static JbpmSessionFactory jbpmSessionFactory = JbpmSessionFactory.buildJbpmSessionFactory();
 * </pre>
 * 
 * and store it somewhere static. It takes quite some time to create a JbpmSessionFactory, but
 * you only have to do it once. After that, creating JbpmSession's is really fast.
 * 
 * @deprecated use {@link org.jbpm.JbpmContext} and {@link org.jbpm.JbpmConfiguration} instead.
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JbpmSessionFactory {

  private static String jndiName = getJndiName();

  private static String getJndiName() {
    if (Configs.hasObject("jbpm.session.factory.jndi.name")) {
      return Configs.getString("jbpm.session.factory.jndi.name");
    }
    return null;
  }

  private Configuration configuration;
  private SessionFactory sessionFactory;
  private Collection hibernatableLongIdClasses;
  private Collection hibernatableStringIdClasses;
  private JbpmSchema jbpmSchema;

  private static JbpmSessionFactory instance;

  /**
   * a singleton is kept in JbpmSessionFactory as a convenient central location.
   */
  public static JbpmSessionFactory getInstance() {
    if (instance == null) {
      // if there is a JNDI name configured
      if (jndiName != null) {
        // fetch the JbpmSessionFactory from JNDI
        if (log.isDebugEnabled()) {
          log.debug("fetching JbpmSessionFactory from " + jndiName);
        }
        instance = (JbpmSessionFactory) JndiUtil.lookup(jndiName, JbpmSessionFactory.class);
      }
      // else there is no JNDI name configured
      else {
        // create a new default instance
        if (log.isDebugEnabled()) log.debug("building singleton JbpmSessionFactory");
        instance = buildJbpmSessionFactory();
      }
    }
    return instance;
  }

  public JbpmSessionFactory(Configuration configuration) {
    this(configuration, buildSessionFactory(configuration));
  }

  public JbpmSessionFactory(Configuration configuration, SessionFactory sessionFactory) {
    this.configuration = configuration;
    this.sessionFactory = sessionFactory;
  }

  public static JbpmSessionFactory buildJbpmSessionFactory() {
    return buildJbpmSessionFactory(getConfigResource());
  }

  public static JbpmSessionFactory buildJbpmSessionFactory(String configResource) {
    return buildJbpmSessionFactory(createConfiguration(configResource));
  }

  public static JbpmSessionFactory buildJbpmSessionFactory(Configuration configuration) {
    return new JbpmSessionFactory(configuration);
  }

  private static String getConfigResource() {
    return Configs.getString("resource.hibernate.cfg.xml");
  }

  public static Configuration createConfiguration() {
    return createConfiguration(getConfigResource());
  }

  public static Configuration createConfiguration(String configResource) {
    // create the hibernate configuration
    Configuration configuration = new Configuration();
    if (configResource != null) {
      configuration.configure(configResource);
    }
    else {
      configuration.configure();
    }

    // check if the properties in the hibernate.cfg.xml need to be overwritten by a separate
    // properties file.
    if (Configs.hasObject("resource.hibernate.properties")) {
      String propertiesResource = Configs.getString("resource.hibernate.properties");
      if (log.isDebugEnabled()) {
        log.debug("loading hibernate properties from resource: " + propertiesResource);
      }
      Properties properties = new Properties();
      try {
        properties.load(ClassLoaderUtil.getStream(propertiesResource));
      }
      catch (IOException e) {
        throw new JbpmException("could not load hibernate properties from resource: "
          + propertiesResource, e);
      }
      // add the properties to the configuration, replacing any existing values
      configuration.addProperties(properties);
    }

    return configuration;
  }

  public static SessionFactory buildSessionFactory(Configuration configuration) {
    return configuration.buildSessionFactory();
  }

  /**
   * obtains a jdbc connection as specified in the hibernate configurations and creates a
   * JbpmSession with it.
   */
  public JbpmSession openJbpmSession() {
    return openJbpmSession((Connection) null);
  }

  /**
   * creates a JbpmSession around the given connection. Note that you are responsible for
   * closing the connection so closing the JbpmSession will not close the jdbc connection.
   */
  public JbpmSession openJbpmSession(Connection jdbcConnection) {
    JbpmSession dbSession;
    try {
      Session session;
      if (jdbcConnection == null) {
        // use the hibernate properties in the nwsp.properties file to
        // create a jdbc connection for the created hibernate session.
        session = getSessionFactory().openSession();
      }
      else {
        // use the client provided jdbc connection in
        // the created hibernate session.
        session = getSessionFactory().openSession(jdbcConnection);
      }

      dbSession = new JbpmSession(this, session);
    }
    catch (HibernateException e) {
      log.error(e);
      throw new JbpmException("couldn't create a hibernate persistence session", e);
    }
    return dbSession;
  }

  public JbpmSession openJbpmSession(Session session) {
    return new JbpmSession(null, session);
  }

  public JbpmSession openJbpmSessionAndBeginTransaction() {
    JbpmSession dbSession = openJbpmSession((Connection) null);
    dbSession.beginTransaction();
    return dbSession;
  }

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * checks if the given class is persistable with hibernate and has an id of type long.
   */
  public boolean isHibernatableWithLongId(Class clazz) {
    if (hibernatableLongIdClasses == null) initHibernatableClasses();
    return hibernatableLongIdClasses.contains(clazz);
  }

  /**
   * checks if the given class is persistable with hibernate and has an id of type string.
   */
  public boolean isHibernatableWithStringId(Class clazz) {
    if (hibernatableStringIdClasses == null) initHibernatableClasses();
    return hibernatableStringIdClasses.contains(clazz);
  }

  public JbpmSchema getJbpmSchema() {
    if (jbpmSchema == null) jbpmSchema = new JbpmSchema(configuration);
    return jbpmSchema;
  }

  private void initHibernatableClasses() {
    hibernatableLongIdClasses = new HashSet();
    hibernatableStringIdClasses = new HashSet();
    for (Iterator iter = configuration.getClassMappings(); iter.hasNext();) {
      PersistentClass persistentClass = (PersistentClass) iter.next();
      if (LongType.class == persistentClass.getIdentifier().getType().getClass()) {
        hibernatableLongIdClasses.add(persistentClass.getMappedClass());
      }
      else if (StringType.class == persistentClass.getIdentifier().getType().getClass()) {
        hibernatableStringIdClasses.add(persistentClass.getMappedClass());
      }
    }
  }

  private static final Log log = LogFactory.getLog(JbpmSessionFactory.class);
}
