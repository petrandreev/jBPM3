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

import java.security.AccessControlException;
import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.db.ContextSession;
import org.jbpm.db.GraphSession;
import org.jbpm.db.JobSession;
import org.jbpm.db.LoggingSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;
import org.jbpm.logging.LoggingService;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.msg.MessageService;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.jbpm.security.authentication.DefaultAuthenticationService;

public class ServicesTest extends AbstractJbpmTestCase {

  public void testUnavailableServiceFactory() {
    Map serviceFactories = new HashMap();
    Services services = new Services(serviceFactories);
    assertNull(services.getService("unexisting-service"));
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
  
  public void testGetService() {
    Map serviceFactories = new HashMap();
    serviceFactories.put("testservice", new TestServiceFactory());
    Services services = new Services(serviceFactories);
    TestService testService = (TestService) services.getService("testservice");
    assertNotNull(testService);
    assertSame(testService, services.getService("testservice"));
  }
  
  public static class TestGivenServiceFactory implements ServiceFactory {
    private static final long serialVersionUID = 1L;
    Service service = null;
    public TestGivenServiceFactory(Service service) {
      this.service = service;
    }
    public Service openService() {
      return service;
    }
    public void close() {
    }
  }
  
  public void testAuthenticationService() {
    Map serviceFactories = new HashMap();
    AuthenticationService authenticationService = new DefaultAuthenticationService();
    serviceFactories.put("authentication", new TestGivenServiceFactory(
            authenticationService
          ));
    Services services = new Services(serviceFactories);
    assertSame(authenticationService, services.getAuthenticationService());
  }

  public void testAuthorizationService() {
    Map serviceFactories = new HashMap();
    AuthorizationService authorizationService = new AuthorizationService() {
      private static final long serialVersionUID = 1L;
      public void checkPermission(Permission permission) throws AccessControlException {
      }
      public void close() {
      }
    };
    serviceFactories.put("authorization", new TestGivenServiceFactory(
      authorizationService
    ));
    Services services = new Services(serviceFactories);
    assertSame(authorizationService, services.getAuthorizationService());
  }

  public void testLoggingService() {
    Map serviceFactories = new HashMap();
    LoggingService loggingService = new LoggingService(){
      private static final long serialVersionUID = 1L;
      public void log(ProcessLog processLog) {}
      public void close() {}
    };
    serviceFactories.put("logging", new TestGivenServiceFactory(
            loggingService
    ));
    Services services = new Services(serviceFactories);
    assertSame(loggingService, services.getLoggingService());
  }
  public void testMessageService() {
    Map serviceFactories = new HashMap();
    MessageService messageService = new MessageService(){
      private static final long serialVersionUID = 1L;
      public void send(Job job) {}
      public void close() {}
    };
    serviceFactories.put(Services.SERVICENAME_MESSAGE, new TestGivenServiceFactory(
            messageService
    ));
    Services services = new Services(serviceFactories);
    assertSame(messageService, services.getMessageService());
  }
  public void testPersistenceService() {
    Map serviceFactories = new HashMap();
    PersistenceService service = new PersistenceService() {
      private static final long serialVersionUID = 1L;
      public void close() {}
      public void assignId(Object object) {}
      public GraphSession getGraphSession() {return null;}
      public LoggingSession getLoggingSession() {return null;}
      public JobSession getJobSession() {return null;}
      public ContextSession getContextSession() {return null;}
      public TaskMgmtSession getTaskMgmtSession() {return null;}
      public Object getCustomSession(Class sessionClass) {return null;}
      public boolean isRollbackOnly() {return false;}
      public void setRollbackOnly(boolean isRollbackOnly) {}
      public void setRollbackOnly() {}
      public void setGraphSession(GraphSession graphSession) {}
      public void setLoggingSession(LoggingSession loggingSession) {}
      public void setJobSession(JobSession jobSession) {}
      public void setTaskMgmtSession(TaskMgmtSession taskMgmtSession) {}
    };
    serviceFactories.put("persistence", new TestGivenServiceFactory(service));
    Services services = new Services(serviceFactories);
    assertSame(service, services.getPersistenceService());
  }
  public void testSchedulerService() {
    Map serviceFactories = new HashMap();
    SchedulerService schedulerService = new SchedulerService() {
      private static final long serialVersionUID = 1L;
      public void createTimer(Timer timer) {}
      public void deleteTimer(Timer timer) {}
      public void deleteTimersByName(String timerName, Token token) {}
      public void deleteTimersByProcessInstance(ProcessInstance processInstance) {}
      public void close() {}
    };
    serviceFactories.put("scheduler", new TestGivenServiceFactory(
            schedulerService
    ));
    Services services = new Services(serviceFactories);
    assertSame(schedulerService, services.getSchedulerService());
  }
}
