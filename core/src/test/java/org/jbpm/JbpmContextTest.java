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

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.LoggingService;
import org.jbpm.logging.log.MessageLog;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.svc.save.SaveOperation;

public class JbpmContextTest extends AbstractJbpmTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    JbpmConfiguration.clearInstances();
  }

  protected void tearDown() throws Exception {
    JbpmConfiguration.setDefaultObjectFactory(null);
    super.tearDown();
  }

  public void testServices() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='a' />"
      + "</jbpm-configuration>");

    JbpmContext a = jbpmConfiguration.createJbpmContext("a");
    try {
      Services s = a.getServices();

      JbpmContext a2 = jbpmConfiguration.createJbpmContext("a");
      try {
        Services s2 = a2.getServices();
        assertNotSame(s, s2);
      }
      finally {
        a2.close();
      }
    }
    finally {
      a.close();
    }
  }

  public void testJbpmContext() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='a' />"
      + "</jbpm-configuration>");

    JbpmContext one = jbpmConfiguration.createJbpmContext("a");
    try {

      JbpmContext two = jbpmConfiguration.createJbpmContext("a");
      try {
        assertNotSame(one, two);
      }
      finally {
        two.close();
      }
    }
    finally {
      one.close();
    }
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
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='a'>"
      + "    <service name='test' factory='org.jbpm.JbpmContextTest$TestServiceFactory' />"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext("a");
    try {
      Object service = jbpmContext.getServices().getService("test");
      assertSame(TestService.class, service.getClass());
    }
    finally {
      jbpmContext.close();
    }
  }

  public void testServiceInFactoryElement() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='a'>"
      + "    <service name='test'>"
      + "      <factory>"
      + "        <bean class='org.jbpm.JbpmContextTest$TestServiceFactory' />"
      + "      </factory>"
      + "    </service>"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext("a");
    try {
      Object service = jbpmContext.getServices().getService("test");
      assertSame(TestService.class, service.getClass());
    }
    finally {
      jbpmContext.close();
    }
  }

  public void testServiceCaching() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='a'>"
      + "    <service name='test' factory='" + TestServiceFactory.class.getName() + "' />"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");

    JbpmContext a = jbpmConfiguration.createJbpmContext("a");
    try {
      TestService serviceOne = (TestService) a.getServices().getService("test");
      TestService serviceTwo = (TestService) a.getServices().getService("test");
      assertSame(serviceOne, serviceTwo);

      JbpmContext a2 = jbpmConfiguration.createJbpmContext("a");
      try {
        assertNotSame(serviceOne, a2.getServices().getService("test"));
      }
      finally {
        a2.close();
      }
    }
    finally {
      a.close();
    }
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
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='a'>"
      + "    <service name='logging' factory='" + CustomLoggingServiceFactory.class.getName() + "' />"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");

    CustomLoggingService customLoggingService = null;
    MessageLog messageLog = null;

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext("a");
    try {
      customLoggingService = (CustomLoggingService) jbpmContext.getServices().getLoggingService();

      messageLog = new MessageLog("blablabla");
      ProcessInstance processInstance = new ProcessInstance(new ProcessDefinition());
      processInstance.getLoggingInstance().addLog(messageLog);
      jbpmContext.save(processInstance);
    }
    finally {
      jbpmContext.close();
    }
    assertEquals(messageLog, customLoggingService.processLog);
    assertEquals(2, customLoggingService.invocationCount);
  }

  public static class TestSaveOperation implements SaveOperation {
    private static final long serialVersionUID = 1L;

    static int invocationCount = 0;

    public void save(ProcessInstance processInstance, JbpmContext jbpmContext) {
      invocationCount++;
    }
  }

  public void testSaveOperation() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='default.jbpm.context'>"
      + "    <save-operations>"
      + "      <save-operation class='" + TestSaveOperation.class.getName() + "' />"
      + "    </save-operations>"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");
    TestSaveOperation.invocationCount = 0;

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      assertEquals(0, TestSaveOperation.invocationCount);

      jbpmContext.save(new ProcessInstance());
      assertEquals(1, TestSaveOperation.invocationCount);
    }
    finally {
      jbpmContext.close();
    }
  }

  public void testSaveOperationInBeanElement() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context name='default.jbpm.context'>"
      + "    <save-operations>"
      + "      <save-operation>"
      + "        <bean class='" + TestSaveOperation.class.getName() + "' />"
      + "      </save-operation>"
      + "    </save-operations>"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");
    TestSaveOperation.invocationCount = 0;

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      assertEquals(0, TestSaveOperation.invocationCount);

      jbpmContext.save(new ProcessInstance());
      assertEquals(1, TestSaveOperation.invocationCount);
    }
    finally {
      jbpmContext.close();
    }
  }
}
