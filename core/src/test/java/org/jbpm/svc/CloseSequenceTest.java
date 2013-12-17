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
package org.jbpm.svc;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * this test verifies that services are closed in the 
 * order as they are specified in the jbpmConfiguration (=jbpm.cfg.xml)
 */
public class CloseSequenceTest extends AbstractJbpmTestCase {
  
  static List closedServices = null; 

  public static class TestServiceFactory implements ServiceFactory {
    private static final long serialVersionUID = 1L;
    String id;
    public Service openService() {
      return new TestService(id);
    }
    public void close() {
    }
  }

  public static class TestService implements Service {
    private static final long serialVersionUID = 1L;
    String id;
    public TestService(String id) {
      this.id = id;
    }
    public void close() {
      closedServices.add(id);
    }
  }
  
  JbpmConfiguration jbpmConfiguration = null;
  
  protected void setUp() {
    closedServices = new ArrayList();
    jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='default.jbpm.context'>" +
      "    <service name='one'>" +
      "      <factory>" +
      "        <bean class='org.jbpm.svc.CloseSequenceTest$TestServiceFactory'>" +
      "          <field name='id'><string value='one'/></field>" +
      "        </bean>" +
      "      </factory>" +
      "    </service>" +
      "    <service name='two'>" +
      "      <factory>" +
      "        <bean class='org.jbpm.svc.CloseSequenceTest$TestServiceFactory'>" +
      "          <field name='id'><string value='two'/></field>" +
      "        </bean>" +
      "      </factory>" +
      "    </service>" +
      "    <service name='three'>" +
      "      <factory>" +
      "        <bean class='org.jbpm.svc.CloseSequenceTest$TestServiceFactory'>" +
      "          <field name='id'><string value='three'/></field>" +
      "        </bean>" +
      "      </factory>" +
      "    </service>" +
      "    <service name='four'>" +
      "      <factory>" +
      "        <bean class='org.jbpm.svc.CloseSequenceTest$TestServiceFactory'>" +
      "          <field name='id'><string value='four'/></field>" +
      "        </bean>" +
      "      </factory>" +
      "    </service>" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
  }
  
  public void testAllServices() {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      jbpmContext.getServices().getService("two");
      jbpmContext.getServices().getService("four");
      jbpmContext.getServices().getService("one");
      jbpmContext.getServices().getService("three");
    } finally {
      jbpmContext.close();
    }
    
    assertEquals("one", closedServices.get(0));
    assertEquals("two", closedServices.get(1));
    assertEquals("three", closedServices.get(2));
    assertEquals("four", closedServices.get(3));
  }

  public void testTwoOutOfFour() {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      jbpmContext.getServices().getService("three");
      jbpmContext.getServices().getService("two");
    } finally {
      jbpmContext.close();
    }
    
    assertEquals("two", closedServices.get(0));
    assertEquals("three", closedServices.get(1));
  }
}
