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
package org.jbpm.enterprise.config;

import junit.framework.Test;

import org.apache.cactus.ServletTestCase;
import org.jbpm.JbpmConfiguration;
import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.msg.jms.JmsMessageServiceFactory;
import org.jbpm.persistence.jta.JtaDbPersistenceServiceFactory;
import org.jbpm.scheduler.ejbtimer.EntitySchedulerServiceFactory;
import org.jbpm.svc.Services;

public class AppServerConfigurationsTest extends ServletTestCase
{
  JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance();

  public static Test suite() throws Exception
  {
     return new IntegrationTestSetup(AppServerConfigurationsTest.class, "enterprise-test.war");
  }
  
  public void testUnavailabilityOfTheJobExecutor()
  {
    assertNull(jbpmConfiguration.getJobExecutor());
  }

  public void testJtaDbPersistenceFactoryConfiguration()
  {
    String expService = JtaDbPersistenceServiceFactory.class.getName();
    String wasService = jbpmConfiguration.getServiceFactory(Services.SERVICENAME_PERSISTENCE).getClass().getName();
    assertEquals(expService, wasService);
    
    JtaDbPersistenceServiceFactory persistenceServiceFactory = (JtaDbPersistenceServiceFactory)jbpmConfiguration.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
    assertFalse(persistenceServiceFactory.isTransactionEnabled());
    assertTrue(persistenceServiceFactory.isCurrentSessionEnabled());
  }

  public void testJmsMessageServiceFactoryConfiguration()
  {
    String expService = JmsMessageServiceFactory.class.getName();
    String wasService = jbpmConfiguration.getServiceFactory(Services.SERVICENAME_MESSAGE).getClass().getName();
    assertEquals(expService, wasService);
  }

  public void testEjbSchedulerServiceFactoryConfiguration()
  {
    String expService = EntitySchedulerServiceFactory.class.getName();
    String wasService = jbpmConfiguration.getServiceFactory(Services.SERVICENAME_SCHEDULER).getClass().getName();
    assertEquals(expService, wasService);
  }
}
