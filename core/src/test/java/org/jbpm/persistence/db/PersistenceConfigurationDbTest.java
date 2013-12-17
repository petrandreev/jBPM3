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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

public class PersistenceConfigurationDbTest extends AbstractJbpmTestCase {

  public void testDisableHibernateTransactions() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context>"
      + "    <service name='persistence'>"
      + "      <factory>"
      + "        <bean class='org.jbpm.persistence.db.DbPersistenceServiceFactory'>"
      + "          <field name='isTransactionEnabled'><false /></field>"
      + "        </bean>"
      + "      </factory>"
      + "    </service>"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");

    DbPersistenceServiceFactory dbPersistenceServiceFactory = (DbPersistenceServiceFactory) jbpmConfiguration.getServiceFactory("persistence");
    assertFalse(dbPersistenceServiceFactory.isTransactionEnabled());
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      DbPersistenceService dbPersistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();
      assertFalse(dbPersistenceService.isTransactionEnabled());
      assertNull(dbPersistenceService.getTransaction());
    }
    finally {
      jbpmContext.close();
    }
  }

  public void testDifferentHibernateCfgFile() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context>"
      + "    <service name='persistence' factory='org.jbpm.persistence.db.DbPersistenceServiceFactory' />"
      + "  </jbpm-context>"
      + "  <string name='resource.hibernate.cfg.xml' value='org/jbpm/persistence/db/custom.hibernate.cfg.xml' />"
      + "  <string name='resource.hibernate.properties' value='org/jbpm/persistence/db/custom.hibernate.properties' />"
      + "</jbpm-configuration>");

    DbPersistenceServiceFactory dbPersistenceServiceFactory = (DbPersistenceServiceFactory) jbpmConfiguration.getServiceFactory("persistence");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      DbPersistenceService dbPersistenceService = (DbPersistenceService) jbpmContext.getServices()
        .getPersistenceService();
      assertEquals(0, dbPersistenceService.getSessionFactory().getAllClassMetadata().size());
      assertEquals("org.hibernate.dialect.PostgreSQLDialect", dbPersistenceServiceFactory.getConfiguration()
        .getProperty("hibernate.dialect"));
    }
    finally {
      jbpmContext.close();
    }
  }
}
