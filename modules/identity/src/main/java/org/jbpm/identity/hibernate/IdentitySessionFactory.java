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
package org.jbpm.identity.hibernate;

import java.sql.*;

import org.apache.commons.logging.*;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.jbpm.identity.*;

public class IdentitySessionFactory {
  
  protected Configuration configuration = null;
  protected SessionFactory sessionFactory = null;
  
  public IdentitySessionFactory() {
    this(createConfiguration());
  }

  public IdentitySessionFactory(Configuration configuration) {
    this(configuration, configuration.buildSessionFactory());
  }

  public IdentitySessionFactory(Configuration configuration, SessionFactory sessionFactory) {
    this.configuration = configuration;
    this.sessionFactory = sessionFactory;
  }

  public static Configuration createConfiguration() {
    return createConfiguration(null);
  }

  public static Configuration createConfiguration(String resource) {
    Configuration configuration = null;
    // create the hibernate configuration
    configuration = new Configuration();
    
    if (resource!=null) {
      log.debug("using '"+resource+"' as hibernate configuration for jbpm");
      configuration.configure(resource);
    } else {
      log.debug("using the default hibernate configuration file: hibernate.cfg.xml");
      configuration.configure();
    }
    return configuration;
  }

  public IdentitySession openIdentitySession() {
    return new IdentitySession(sessionFactory.openSession());
  }

  public IdentitySession openIdentitySession(Connection connection) {
    return new IdentitySession(sessionFactory.openSession(connection));
  }

  public void evictCachedIdentities() {
    sessionFactory.evict(User.class);
    sessionFactory.evict(Membership.class);
    sessionFactory.evict(Group.class);
  }

  public Configuration getConfiguration() {
    return configuration;
  }
  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  private static final Log log = LogFactory.getLog(IdentitySessionFactory.class);
}
