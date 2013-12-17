package org.jbpm.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.Delegation;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

public class TaskMailTest extends AbstractJbpmTestCase 
{
  private static final int SMTP_PORT = 23583;

  static JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
    "<jbpm-configuration>" +
    "  <jbpm-context>" +
    "    <service name='scheduler' factory='org.jbpm.mail.TaskMailTest$TestSchedulerService' />" +
    "  </jbpm-context>" +
    "  <string name='resource.mail.properties' value='org/jbpm/mail/test.mail.properties' />" +
    "  <bean name='jbpm.mail.address.resolver' class='org.jbpm.mail.MailTest$TestAddressResolver' singleton='true' />" +
    "</jbpm-configuration>"
  );

  private static SimpleSmtpServer server;
  private JbpmContext jbpmContext;

  public static Test suite()
  {
    return new TestSetup(new TestSuite(TaskMailTest.class))
    {
      protected void setUp() throws Exception
      {
        server = MailTest.startSmtpServer(SMTP_PORT);
      }

      protected void tearDown() throws Exception
      {
        server.stop();
      }
    };
  }
  
  protected void setUp() throws Exception
  {
    super.setUp();
    TestSchedulerService testSchedulerService = (TestSchedulerService) jbpmConfiguration.getServiceFactory("scheduler");
    testSchedulerService.reset();
    jbpmContext = jbpmConfiguration.createJbpmContext();
  }

  protected void tearDown() throws Exception
  {
    jbpmContext.close();
    super.tearDown();
  }

  public static class RoundRobinAssigner implements AssignmentHandler
  {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception
    {
      assignable.setActorId("you");
    }
  }

  public void testTaskInstanceNotification() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='laundry' notify='yes'>" +
      "      <assignment class='org.jbpm.mail.TaskMailTest$RoundRobinAssigner' />" +
      "    </task>" +
      "    <transition to='b' />" +
      "  </task-node>" +
      "  <state name='b' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage) emailIter.next();
    emailIter.remove();
    
    assertEquals("you@example.domain", email.getHeaderValue("To"));
    assertEquals("Task 'laundry'", email.getHeaderValue("Subject"));
    assertEquals(-1, email.getBody().indexOf("#{"));  // just to make sure that all expressions were resolved
    assertTrue(-1!=email.getBody().indexOf("http://localhost:8080/jbpm/home?taskId=0")); 
  }
  
  public static class TestSchedulerService implements SchedulerService, ServiceFactory
  {
    private static final long serialVersionUID = 1L;
    List createdTimers = null;
    List cancelledTimers = null;

    public TestSchedulerService()
    {
      reset();
    }

    public void reset()
    {
      createdTimers = new ArrayList();
      cancelledTimers = new ArrayList();
    }

    public void createTimer(Timer timer)
    {
      createdTimers.add(timer);
    }

    public void deleteTimer(Timer timer)
    {
      cancelledTimers.add(timer.getName());
    }

    public void deleteTimersByName(String timerName, Token token)
    {
      cancelledTimers.add(timerName);
    }

    public void deleteTimersByProcessInstance(ProcessInstance processInstance)
    {
    }

    public Service openService()
    {
      return this;
    }

    public void close()
    {
    }
  }

  public void testTaskInstanceReminder() throws Exception 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='laundry'>" +
      "      <assignment class='org.jbpm.mail.TaskMailTest$RoundRobinAssigner' />" +
      "      <reminder duedate='0 seconds' repeat='60 seconds' />" +
      "    </task>" +
      "    <transition to='b' />" +
      "  </task-node>" +
      "  <state name='b' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertTrue(server.getReceivedEmailSize() == 0);

    TestSchedulerService testSchedulerService = (TestSchedulerService) jbpmContext.getServices().getService("scheduler");
    assertEquals(1, testSchedulerService.createdTimers.size());
    Timer createdTimer = (Timer) testSchedulerService.createdTimers.get(0);
    Delegation delegation = createdTimer.getAction().getActionDelegation();
    assertEquals("org.jbpm.mail.Mail", delegation.getClassName());
    assertEquals("<template>task-reminder</template>", delegation.getConfiguration());
    
    createdTimer.execute(jbpmContext);

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage) emailIter.next();
    emailIter.remove();
    
    assertEquals("you@example.domain", email.getHeaderValue("To"));
    assertEquals("Task 'laundry' !", email.getHeaderValue("Subject"));
    assertEquals(-1, email.getBody().indexOf("#{"));  // just to make sure that all expressions were resolved
    assertTrue(-1!=email.getBody().indexOf("http://localhost:8080/jbpm/home?taskId=0"));
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertEquals(0, testSchedulerService.cancelledTimers.size());
    taskInstance.end();
    assertEquals(1, testSchedulerService.cancelledTimers.size());
  }
  
  public static class GhostAssigner implements AssignmentHandler
  {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception
    {
      assignable.setActorId("ghost");
    }
  }

  public void testUnexistingUser() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='laundry' notify='true'>" +
      "      <assignment class='org.jbpm.mail.TaskMailTest$GhostAssigner' />" +
      "    </task>" +
      "    <transition to='b' />" +
      "  </task-node>" +
      "  <state name='b' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertEquals(0, server.getReceivedEmailSize());

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    
    // ghost will get null as an email address in the org.jbpm.mail.MailTest$TestAddressResolver
    assertEquals("ghost", taskInstance.getActorId());
    
    taskInstance.end();
    assertEquals(0, server.getReceivedEmailSize());
  }
}
