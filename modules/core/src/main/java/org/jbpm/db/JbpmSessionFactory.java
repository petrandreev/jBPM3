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
import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.util.ClassLoaderUtil;

/**
 * creates JbpmSessions.
 * Obtain a JbpmSessionFactory with
 * <pre>
 * static JbpmSessionFactory jbpmSessionFactory = JbpmSessionFactory.buildJbpmSessionFactory();
 * </pre>
 * and store it somewhere static.  It takes quite some time to create a DbSessionFactory,
 * but you only have to do it once.  After that, creating DbSession's is really fast.
 * 
 * @deprecated use {@link org.jbpm.JbpmContext} and {@link org.jbpm.JbpmConfiguration} instead.
 */
public class JbpmSessionFactory implements Serializable {
  
  private static final long serialVersionUID = 1L;

  static String jndiName = getJndiName();
  private static String getJndiName() {
    String jndiName = null;
    if (JbpmConfiguration.Configs.hasObject("jbpm.session.factory.jndi.name")) {
      jndiName = JbpmConfiguration.Configs.getString("jbpm.session.factory.jndi.name");
    }
    return jndiName;
  }

  Configuration configuration = null;
  SessionFactory sessionFactory = null;
  Collection hibernatableLongIdClasses = null;
  Collection hibernatableStringIdClasses = null;
  JbpmSchema jbpmSchema = null;
  
  static JbpmSessionFactory instance = null;
  /**
   * a singleton is kept in JbpmSessionFactory as a convenient central location.
   */
  public static JbpmSessionFactory getInstance() {
    if (instance==null) {
      
      // if there is a JNDI name configured
      if (jndiName!=null) {
        try {
          // fetch the JbpmSessionFactory from JNDI
          log.debug("fetching JbpmSessionFactory from '"+jndiName+"'");
          InitialContext initialContext = new InitialContext();
          Object o = initialContext.lookup(jndiName);
          instance = (JbpmSessionFactory) PortableRemoteObject.narrow(o, JbpmSessionFactory.class);
        } catch (Exception e) {
          throw new JbpmException("couldn't fetch JbpmSessionFactory from jndi '"+jndiName+"'");
        }
        
      } else { // else there is no JNDI name configured
        // create a new default instance.
        log.debug("building singleton JbpmSessionFactory");
        instance = buildJbpmSessionFactory();
      }
    }
    return instance;
  }
  
  public JbpmSessionFactory(Configuration configuration) {
    this( configuration, buildSessionFactory(configuration) );
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
    return JbpmConfiguration.Configs.getString("resource.hibernate.cfg.xml");
  }

  public static Configuration createConfiguration() {
    return createConfiguration(getConfigResource());
  }

  public static Configuration createConfiguration(String configResource) {
    Configuration configuration = null;
    // create the hibernate configuration
    configuration = new Configuration();
    if (configResource!=null) {
      log.debug("using '"+configResource+"' as hibernate configuration for jbpm");
      configuration.configure(configResource);
    } else {
      log.debug("using the default hibernate configuration file: hibernate.cfg.xml");
      configuration.configure();
    }
    
    // check if the properties in the hibernate.cfg.xml need to be overwritten by a separate properties file.
    if (JbpmConfiguration.Configs.hasObject("resource.hibernate.properties")) {
      String hibernatePropertiesResource = JbpmConfiguration.Configs.getString("resource.hibernate.properties");
      Properties hibernateProperties = new Properties();
      try {
        hibernateProperties.load( ClassLoaderUtil.getStream(hibernatePropertiesResource) );
      } catch (IOException e) {
        throw new JbpmException("couldn't load the hibernate properties from resource '"+hibernatePropertiesResource+"'", e);
      }
      log.debug("overriding hibernate properties with "+ hibernateProperties);
      configuration.setProperties(hibernateProperties);
    }
    
    return configuration;
  }

  public static SessionFactory buildSessionFactory(Configuration configuration) {
    SessionFactory sessionFactory = null;
    // create the hibernate session factory
    log.debug("building hibernate session factory");
    sessionFactory = configuration.buildSessionFactory();
    return sessionFactory;
  }

  /**
   * obtains a jdbc connection as specified in the hibernate configurations and 
   * creates a DbSession with it.
   */
  public JbpmSession openJbpmSession() {
    return openJbpmSession((Connection)null);
  }

  /**
   * creates a DbSession around the given connection.  Note that you are 
   * responsible for closing the connection so closing the DbSession will 
   * not close the jdbc connection.
   */
  public JbpmSession openJbpmSession(Connection jdbcConnection) {
    JbpmSession dbSession = null;
    
    try {
      Session session = null;
      
      if ( jdbcConnection == null ) {
        // use the hibernate properties in the nwsp.properties file to 
        // create a jdbc connection for the created hibernate session.
        session = getSessionFactory().openSession();
      } else {
        // use the client provided jdbc connection in  
        // the created hibernate session.
        session = getSessionFactory().openSession(jdbcConnection);
      }
      
      dbSession = new JbpmSession( this, session );
      
    } catch (HibernateException e) {
      log.error( e );
      throw new JbpmException( "couldn't create a hibernate persistence session", e );
    }
    return dbSession;
  }

  public JbpmSession openJbpmSession(Session session) {
    return new JbpmSession(null, session);
  }

  public JbpmSession openJbpmSessionAndBeginTransaction() {
    JbpmSession dbSession = openJbpmSession((Connection)null);
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
   * clears the process definitions from hibernate's second level cache.
  public void evictCachedProcessDefinitions() {
    sessionFactory.evict(ProcessDefinition.class);
  }
   */

  /**
   * checks if the given class is persistable with hibernate and has an id of type long.
   */
  public boolean isHibernatableWithLongId(Class clazz) {
    if (hibernatableLongIdClasses==null) {
      initHibernatableClasses();
    }
    return hibernatableLongIdClasses.contains(clazz);
  }

  /**
   * checks if the given class is persistable with hibernate and has an id of type string.
   */
  public boolean isHibernatableWithStringId(Class clazz) {
    if (hibernatableStringIdClasses==null) {
      initHibernatableClasses();
    }
    return hibernatableStringIdClasses.contains(clazz);
  }
  
  public JbpmSchema getJbpmSchema() {
    if (jbpmSchema==null) {
      jbpmSchema = new JbpmSchema(configuration);
    }
    return jbpmSchema;
  }

  void initHibernatableClasses() {
    hibernatableLongIdClasses = new HashSet();
    hibernatableStringIdClasses = new HashSet();
    Iterator iter = configuration.getClassMappings();
    while (iter.hasNext()) {
      PersistentClass persistentClass = (PersistentClass) iter.next();
      if (LongType.class==persistentClass.getIdentifier().getType().getClass()) {
        hibernatableLongIdClasses.add( persistentClass.getMappedClass() );
      } else if (StringType.class==persistentClass.getIdentifier().getType().getClass()) {
        hibernatableStringIdClasses.add( persistentClass.getMappedClass() );
      }
    }
  }

  private static final Log log = LogFactory.getLog(JbpmSessionFactory.class);
}
