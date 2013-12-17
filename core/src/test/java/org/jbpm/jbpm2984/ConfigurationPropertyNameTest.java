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
package org.jbpm.jbpm2984;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.persistence.jta.JtaDbPersistenceServiceFactory;

/**
 * JtaDbPersistenceServiceFactory backward incompatibility.
 * 
 * @see <a href="https://issues.jboss.org/browse/JBPM-2984">JBPM-2984</a>
 * @author Alejandro Guizar
 */
public class ConfigurationPropertyNameTest extends AbstractJbpmTestCase {

  private JbpmConfiguration jbpmConfiguration;

  protected void setUp() throws Exception {
    super.setUp();
    jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + " <jbpm-context>"
      + "  <service name='persistence'>"
      + "   <factory>"
      + "    <bean class='"
      + JtaDbPersistenceServiceFactory.class.getName()
      + "'>"
      + "     <property name='isCurrentSessionEnabled'><false/></property>"
      + "    </bean>"
      + "   </factory>"
      + "  </service>"
      + " </jbpm-context>"
      + "</jbpm-configuration>");
  }

  protected void tearDown() throws Exception {
    jbpmConfiguration.close();
    super.tearDown();
  }

  public void testConfigurationPropertyName() {
    JtaDbPersistenceServiceFactory serviceFactory =
      (JtaDbPersistenceServiceFactory) jbpmConfiguration.getServiceFactory("persistence");
    assertFalse("expected current session to be disabled",
      serviceFactory.isCurrentSessionEnabled());
  }
}
