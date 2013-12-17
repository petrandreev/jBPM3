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
package org.jbpm.configuration;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmContext;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;

public class JbpmContextTest extends AbstractJbpmTestCase {

  protected ObjectFactoryImpl objectFactory = null;

  protected void parse(String xml) {
    objectFactory = ObjectFactoryParser.parseXmlString(xml);
  }

  public void testEmptyJbpmContext() {
    parse(
      "<jbpm-configuration>" +
      "  <jbpm-context name='mycontext' />" +
      "</jbpm-configuration>"
    );
    
    JbpmContext jbpmContext = (JbpmContext) objectFactory.createObject("mycontext");
    assertNotNull(jbpmContext);
    assertNotNull(jbpmContext.getServices());
    assertNotNull(jbpmContext.getServices());
  }
  
  public static class MyServiceFactory implements ServiceFactory {
    private static final long serialVersionUID = 1L;
    public Service openService() {
      return null;
    }
    public void close() {
    }
  }

  public void testJbpmContextWithBeanServiceFactory() {
    parse(
      "<jbpm-configuration>" +
      "  <jbpm-context name='mycontext'>" +
      "    <service name='myservice' factory='org.jbpm.configuration.JbpmContextTest$MyServiceFactory' />" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    
    JbpmContext jbpmContext = (JbpmContext) objectFactory.createObject("mycontext");
    assertNotNull(jbpmContext);
    assertEquals(1, jbpmContext.getServices().getServiceFactories().size());
    assertTrue(jbpmContext.getServices().getServiceFactories().containsKey("myservice"));
  }

  public void testJbpmContextWithTwoServiceFactories() {
    parse(
      "<jbpm-configuration>" +
      "  <bean name='myservicebean' class='org.jbpm.configuration.JbpmContextTest$MyServiceFactory' />" +
      "  <jbpm-context name='mycontext'>" +
      "    <service name='myservice'>" +
      "      <factory>" +
      "        <ref bean='myservicebean' />" +
      "      </factory>" +
      "    </service>" +
      "    <service name='mysecondservice'>" +
      "      <factory>" +
      "        <ref bean='myservicebean' />" +
      "      </factory>" +
      "    </service>" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    
    JbpmContext jbpmContext = (JbpmContext) objectFactory.createObject("mycontext");
    assertNotNull(jbpmContext);
    Services services = jbpmContext.getServices();
    assertEquals(2, services.getServiceFactories().size());
    assertTrue(services.getServiceFactories().containsKey("myservice"));
    assertTrue(services.getServiceFactories().containsKey("mysecondservice"));
    assertSame(services.getServiceFactory("myservice"), services.getServiceFactory("mysecondservice"));
  }
}
