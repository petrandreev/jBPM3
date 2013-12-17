package org.jbpm.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.Test;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.Delegation;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

public class TaskMailTest extends AbstractJbpmTestCase {

  private JbpmContext jbpmContext;

  private static JbpmConfiguration getJbpmConfiguration(int smtpPort) { 
   return JbpmConfiguration.parseXmlString(XML_DECL
    + "<jbpm-configuration>"
    + "  <jbpm-context>"
    + "    <service name='scheduler' factory='"
    + TestSchedulerService.class.getName()
    + "' />"
    + "  </jbpm-context>"
    + "  <int name='jbpm.mail.smtp.port' value='" + smtpPort + "' />"
    + "  <bean name='jbpm.mail.address.resolver' class='"
    + MailTest.TestAddressResolver.class.getName()
    + "' singleton='true' />"
    + "</jbpm-configuration>");
  }

  private static Wiser wiser;

  protected void setUp() throws Exception {
    super.setUp();
    
    wiser = MailTestSetup.getWiser();
    int smtpPort = wiser.getServer().getPort();
    jbpmContext = getJbpmConfiguration(smtpPort).createJbpmContext();
  }

  protected void tearDown() throws Exception {
    wiser.getMessages().clear();
    wiser.stop();
    wiser = null;

    TestSchedulerService testSchedulerService = (TestSchedulerService) jbpmContext.getServiceFactory(Services.SERVICENAME_SCHEDULER);
    testSchedulerService.reset();
    jbpmContext.close();

    super.tearDown();
  }

  public void testTaskInstanceNotification() throws IOException, MessagingException {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='laundry' notify='yes'>"
      + "      <assignment actor-id='you' />"
      + "    </task>"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List messages = wiser.getMessages();
    assertEquals(1, messages.size());

    WiserMessage message = (WiserMessage) messages.get(0);
    MimeMessage email = message.getMimeMessage();

    assert Arrays.equals(InternetAddress.parse("you@example.domain"),
      email.getRecipients(RecipientType.TO));
    assertEquals("Task notification: laundry", email.getSubject());
    // just to make sure that all expressions were resolved
    String content = (String) email.getContent();
    assertEquals(-1, content.indexOf("${"));
    assert content.startsWith("Hi you,") : content;
  }

  public static class TestSchedulerService implements SchedulerService, ServiceFactory {
    private static final long serialVersionUID = 1L;

    List createdTimers = new ArrayList();
    List cancelledTimers = new ArrayList();

    public void reset() {
      createdTimers.clear();
      cancelledTimers.clear();
    }

    public void createTimer(Timer timer) {
      createdTimers.add(timer);
    }

    public void deleteTimer(Timer timer) {
      cancelledTimers.add(timer.getName());
    }

    public void deleteTimersByName(String timerName, Token token) {
      cancelledTimers.add(timerName);
    }

    public void deleteTimersByProcessInstance(ProcessInstance processInstance) {
    }

    public Service openService() {
      return this;
    }

    public void close() {
    }
  }

  public void testTaskInstanceReminder() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='laundry'>"
      + "      <assignment actor-id='you' />"
      + "      <reminder duedate='0 seconds' repeat='60 seconds' />"
      + "    </task>"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertEquals(0, wiser.getMessages().size());

    TestSchedulerService testSchedulerService = (TestSchedulerService) jbpmContext.getServices()
      .getService("scheduler");
    assertEquals(1, testSchedulerService.createdTimers.size());
    Timer createdTimer = (Timer) testSchedulerService.createdTimers.get(0);
    Delegation delegation = createdTimer.getAction().getActionDelegation();
    assertEquals("org.jbpm.mail.Mail", delegation.getClassName());
    assertEquals("<template>task-reminder</template>", delegation.getConfiguration());

    createdTimer.execute(jbpmContext);

    List messages = wiser.getMessages();
    assertEquals(1, messages.size());

    WiserMessage message = (WiserMessage) messages.get(0);
    MimeMessage email = message.getMimeMessage();

    assert Arrays.equals(InternetAddress.parse("you@example.domain"),
      email.getRecipients(RecipientType.TO));
    assertEquals("Task reminder: laundry", email.getSubject());
    // just to make sure that all expressions were resolved
    String content = (String) email.getContent();
    assertEquals(-1, content.indexOf("${"));
    assert content.startsWith("Hey you,") : content;

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertEquals(0, testSchedulerService.cancelledTimers.size());
    taskInstance.end();
    assertEquals(1, testSchedulerService.cancelledTimers.size());
  }

  public void testUnexistingUser() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='laundry' notify='true'>"
      + "      <assignment actor-id='ghost' />"
      + "    </task>"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertEquals(0, wiser.getMessages().size());

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();

    // ghost will get null as an email address in the
    // org.jbpm.mail.MailTest$TestAddressResolver
    assertEquals("ghost", taskInstance.getActorId());

    taskInstance.end();
    assertEquals(0, wiser.getMessages().size());
  }
}
