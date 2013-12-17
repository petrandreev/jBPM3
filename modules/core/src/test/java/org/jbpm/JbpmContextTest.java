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
package org.jbpm;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.LoggingService;
import org.jbpm.logging.log.MessageLog;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.svc.save.SaveOperation;

public class JbpmContextTest extends AbstractJbpmTestCase {
  
  protected void setUp() throws Exception
  {
    super.setUp();
    JbpmConfiguration.instances.clear();
    JbpmConfiguration.defaultObjectFactory = null;
  }
  
  public void testServices() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='a' />" +
      "</jbpm-configuration>"
    );
    assertNotNull(jbpmConfiguration);
    Services s = jbpmConfiguration.createJbpmContext("a").getServices();
    assertNotNull(s);
    Services s2 = jbpmConfiguration.createJbpmContext("a").getServices();
    assertNotNull(s2);
    assertNotSame(s, s2);
  }

  public void testJbpmContext() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='a' />" +
      "</jbpm-configuration>"
    );
    JbpmContext one = jbpmConfiguration.createJbpmContext("a");
    assertNotNull(one);
    JbpmContext two = jbpmConfiguration.createJbpmContext("a");
    assertNotNull(two);
    assertNotSame(one, two);
  }

  public static class TestServiceFactory implements ServiceFactory {
    private static final long serialVersionUID = 1L;
    public Service openService() {
      return new TestService();
    }
    public void close() {
    }
  }

  public static class TestService implements Service {
    private static final long serialVersionUID = 1L;
    public void close() {
    }
  }

  public void testCustomService() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='a'>" +
      "    <service name='test' factory='org.jbpm.JbpmContextTest$TestServiceFactory' />" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    Object service = jbpmConfiguration.createJbpmContext("a").getServices().getService("test");
    assertNotNull(service);
    assertEquals(TestService.class, service.getClass());
  }
  
  public void testServiceInFactoryElement() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='a'>" +
      "    <service name='test'>" +
      "      <factory>" +
      "        <bean class='org.jbpm.JbpmContextTest$TestServiceFactory' />" +
      "      </factory>" +
      "    </service>" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    Object service = jbpmConfiguration.createJbpmContext("a").getServices().getService("test");
    assertNotNull(service);
    assertEquals(TestService.class, service.getClass());
  }
  
  public void testServiceCaching() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='a'>" +
      "    <service name='test' factory='org.jbpm.JbpmContextTest$TestServiceFactory' />" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext("a");
    TestService serviceOne = (TestService) jbpmContext.getServices().getService("test");
    assertNotNull(serviceOne);
    TestService serviceTwo = (TestService) jbpmContext.getServices().getService("test");
    assertNotNull(serviceTwo);
    assertSame(serviceOne, serviceTwo);
    jbpmContext = jbpmConfiguration.createJbpmContext("a");
    assertNotSame(serviceOne, jbpmContext.getServices().getService("test"));
  }
  
  public static class CustomLoggingServiceFactory implements ServiceFactory {
    private static final long serialVersionUID = 1L;
    public Service openService() {
      return new CustomLoggingService();
    }
    public void close() {
    }
  }

  public static class CustomLoggingService implements LoggingService {
    private static final long serialVersionUID = 1L;
    ProcessLog processLog;
    int invocationCount = 0;
    public void close() {
    }
    public void log(ProcessLog processLog) {
      this.processLog = processLog;
      this.invocationCount++;
    }
  }

  public void testCustomLoggingService() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='a'>" +
      "    <service name='logging' factory='org.jbpm.JbpmContextTest$CustomLoggingServiceFactory' />" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    
    CustomLoggingService customLoggingService = null;
    MessageLog messageLog = null;
    
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext("a");
    try {
      customLoggingService = (CustomLoggingService) jbpmContext.getServices().getLoggingService();
      messageLog = new MessageLog("blablabla");
      ProcessInstance processInstance = new ProcessInstance();
      processInstance.getLoggingInstance().addLog(messageLog);
      jbpmContext.save(processInstance);

    } finally {
      jbpmContext.close();
    }
    assertEquals(messageLog, customLoggingService.processLog);
    assertEquals(1, customLoggingService.invocationCount);
  }
  
  
  public static class TestSaveOperation implements SaveOperation {
    static int invocationCount = 0;
    private static final long serialVersionUID = 1L;
    public void save(ProcessInstance processInstance, JbpmContext jbpmContext) {
      invocationCount++;
    }
  }

  public void testSaveOperation() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='default.jbpm.context'>" +
      "    <save-operations>" +
      "      <save-operation class='org.jbpm.JbpmContextTest$TestSaveOperation' />" +
      "    </save-operations>" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    ); 
    
    TestSaveOperation.invocationCount = 0;
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    
    try {
      
      assertEquals(0, TestSaveOperation.invocationCount);
      jbpmContext.save(new ProcessInstance());
      assertEquals(1, TestSaveOperation.invocationCount);
      
    } finally {
      jbpmContext.close();
    }
  }

  public void testSaveOperationInBeanElement() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context name='default.jbpm.context'>" +
      "    <save-operations>" +
      "      <save-operation>" +
      "        <bean class='org.jbpm.JbpmContextTest$TestSaveOperation' />" +
      "      </save-operation>" +
      "    </save-operations>" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    ); 
    
    TestSaveOperation.invocationCount = 0;
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    
    try {
      
      assertEquals(0, TestSaveOperation.invocationCount);
      jbpmContext.save(new ProcessInstance());
      assertEquals(1, TestSaveOperation.invocationCount);
      
    } finally {
      jbpmContext.close();
    }
  }
}
