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

import java.util.Iterator;
import java.util.Map;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmContext;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

public class JbpmContextTest extends AbstractJbpmTestCase {

  private static ObjectFactory parse(String xml) {
    return ObjectFactoryParser.parseXmlString(xml);
  }

  public void testEmptyJbpmContext() {
    ObjectFactory objectFactory = parse("<jbpm-configuration>"
      + "  <jbpm-context name='mycontext' />"
      + "</jbpm-configuration>");

    JbpmContext jbpmContext = (JbpmContext) objectFactory.createObject("mycontext");
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
    ObjectFactory objectFactory = parse("<jbpm-configuration>"
      + "  <jbpm-context name='mycontext'>"
      + "    <service name='myservice' factory='"
      + MyServiceFactory.class.getName()
      + "' />"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");

    JbpmContext jbpmContext = (JbpmContext) objectFactory.createObject("mycontext");
    Map serviceFactories = jbpmContext.getServices().getServiceFactories();
    assertEquals(1, serviceFactories.size());
    assertEquals("myservice", serviceFactories.keySet().iterator().next());
  }

  public void testJbpmContextWithTwoServiceFactories() {
    ObjectFactory objectFactory = parse("<jbpm-configuration>"
      + "  <bean name='myservicebean' class='"
      + MyServiceFactory.class.getName()
      + "' />"
      + "  <jbpm-context name='mycontext'>"
      + "    <service name='myservice'>"
      + "      <factory>"
      + "        <ref bean='myservicebean' />"
      + "      </factory>"
      + "    </service>"
      + "    <service name='myservice2'>"
      + "      <factory>"
      + "        <ref bean='myservicebean' />"
      + "      </factory>"
      + "    </service>"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");

    JbpmContext jbpmContext = (JbpmContext) objectFactory.createObject("mycontext");
    Map serviceFactories = jbpmContext.getServices().getServiceFactories();
    assertEquals(2, serviceFactories.size());
    // iteration order is guaranteed!
    Iterator serviceNames = serviceFactories.keySet().iterator();
    assertEquals("myservice", serviceNames.next());
    assertEquals("myservice2", serviceNames.next());
    assertSame(serviceFactories.get("myservice"), serviceFactories.get("myservice2"));
  }
}
