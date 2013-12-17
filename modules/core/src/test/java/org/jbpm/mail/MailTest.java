package org.jbpm.mail;

import java.util.Arrays;
import java.util.Iterator;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

public class MailTest extends AbstractJbpmTestCase
{
  private static final int SMTP_PORT = 23583;

  private static SimpleSmtpServer server;
  private JbpmContext jbpmContext;
  
  static JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
    "<jbpm-configuration>" +
    "  <jbpm-context />" +
    "  <string name='resource.mail.properties' value='org/jbpm/mail/test.mail.properties' />" +
    "  <bean name='jbpm.mail.address.resolver' class='" + TestAddressResolver.class.getName() + "' singleton='true' />" +
    "</jbpm-configuration>"
  );

  public static Test suite()
  {
    return new TestSetup(new TestSuite(MailTest.class))
    {
      protected void setUp() throws Exception
      {
        server = startSmtpServer(SMTP_PORT);
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
    jbpmContext = jbpmConfiguration.createJbpmContext();
  }

  protected void tearDown() throws Exception
  {
    jbpmContext.close();
    super.tearDown();
  }

  static SimpleSmtpServer startSmtpServer(int port)
  {
    /*
     * SimpleSmtpServer.start(int) blocks the calling thread until the server socket is created. If the socket is
     * created too quickly (seems to happen on Linux and Mac) then the notification is sent too early and the calling
     * thread blocks forever.
     * 
     * The code below corresponds to SimpleSmtpServer.start(int) except that the thread start has been moved inside of
     * the synchronized block.
     */
    SimpleSmtpServer server = new SimpleSmtpServer(port);
    Thread serverThread = new Thread(server);

    // Block until the server socket is created
    synchronized (server)
    {
      serverThread.start();
      try
      {
        server.wait(10 * 1000);
      }
      catch (InterruptedException e)
      {
        // Ignore don't care.
      }
    }
    return server;
  }

  public void testWithoutAddressResolving()
  {
    String to = "sample.shipper@example.domain";
    String subject = "latest news";
    String text = "roy is assurancetourix";

    Mail mail = new Mail(null, null, to, subject, text);
    mail.send();

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("latest news", email.getHeaderValue("Subject"));
    assertEquals("roy is assurancetourix", email.getBody());
    assertEquals("sample.shipper@example.domain", email.getHeaderValue("To"));
  }

  public void testMailWithAddressResolving()
  {
    String actors = "manager";
    String subject = "latest news";
    String text = "roy is assurancetourix";

    Mail mail = new Mail(null, actors, null, subject, text);
    mail.send();

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("latest news", email.getHeaderValue("Subject"));
    assertEquals("roy is assurancetourix", email.getBody());
    assertEquals("manager@example.domain", email.getHeaderValue("To"));
  }

  public void testMailWithBccAddress()
  {
    String bcc = "bcc@example.domain";
    String subject = "latest news";
    String text = "roy is assurancetourix";

    Mail mail = new Mail(null, null, null, null, bcc, subject, text);
    mail.send();

    assertEquals(1, server.getReceivedEmailSize());
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("latest news", email.getHeaderValue("Subject"));
    assertEquals("roy is assurancetourix", email.getBody());
    assertNull(email.getHeaderValue("To"));
  }  
  
  public void testMailNodeAttributes() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='send email' />" +
      "  </start-state>" +
      "  <mail-node name='send email' actors='george' subject='readmylips' text='nomoretaxes'>" +
      "    <transition to='end' />" +
      "  </mail-node>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("readmylips", email.getHeaderValue("Subject"));
    assertEquals("nomoretaxes", email.getBody());
    assertEquals("george@example.domain", email.getHeaderValue("To"));
  }
  
  public void testMailNodeElements() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='send email' />" +
      "  </start-state>" +
      "  <mail-node name='send email' actors='george'>" +
      "    <subject>readmylips</subject>" +
      "    <text>nomoretaxes</text>" +
      "    <transition to='end' />" +
      "  </mail-node>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("readmylips", email.getHeaderValue("Subject"));
    assertEquals("nomoretaxes", email.getBody());
    assertEquals("george@example.domain", email.getHeaderValue("To"));
  }

  public void testMailActionAttributes() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail name='send email' actors='george' subject='readmylips' text='nomoretaxes' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("readmylips", email.getHeaderValue("Subject"));
    assertEquals("nomoretaxes", email.getBody());
    assertEquals("george@example.domain", email.getHeaderValue("To"));
  }

  public void testMailActionElements() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail actors='george'>" +
      "        <subject>readmylips</subject>" +
      "        <text>nomoretaxes</text>" +
      "      </mail>" +
      "    <transition to='end' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertTrue(server.getReceivedEmailSize() == 1);
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("readmylips", email.getHeaderValue("Subject"));
    assertEquals("nomoretaxes", email.getBody());
    assertEquals("george@example.domain", email.getHeaderValue("To"));
  }

  public void testMultipleRecipients() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail name='send email' actors='george; barbara; suzy' subject='readmylips' text='nomoretaxes' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertEquals(1, server.getReceivedEmailSize());
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("readmylips", email.getHeaderValue("Subject"));
    assertEquals("nomoretaxes", email.getBody());
    assertEquals(Arrays.asList(new String[] { "george@example.domain", "barbara@example.domain", "suzy@example.domain" }), Arrays.asList(email
        .getHeaderValues("To")));
  }

  public void testMailWithoutAddressResolving() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail name='send email' to='george@humpydumpy.gov; spiderman@hollywood.ca.us' subject='readmylips' text='nomoretaxes' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertEquals(1, server.getReceivedEmailSize());
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals(Arrays.asList(new String[] { "george@humpydumpy.gov", "spiderman@hollywood.ca.us" }), Arrays.asList(email.getHeaderValues("To")));
  }

  public void testToVariableExpression() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail name='send email' to='#{user.email}' subject='s' text='t' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    User mrNobody = new User("hucklebuck@example.domain");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("user", mrNobody);
    processInstance.signal();

    assertEquals(1, server.getReceivedEmailSize());
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("hucklebuck@example.domain", email.getHeaderValue("To"));
  }

  public void testToSwimlaneExpression() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail name='send email' actors='#{initiator}' subject='s' text='t' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Swimlane initiator = new Swimlane("initiator");
    SwimlaneInstance initiatorInstance = new SwimlaneInstance(initiator);
    initiatorInstance.setActorId("huckelberry");
    processInstance.getTaskMgmtInstance().addSwimlaneInstance(initiatorInstance);
    processInstance.signal();

    assertEquals(1, server.getReceivedEmailSize());
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("huckelberry@example.domain", email.getHeaderValue("To"));
  }

  public void testSubjectExpression() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail name='send email' actors='me' subject='your ${item} order' text='t' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("item", "cookies");
    processInstance.signal();

    assertEquals(1, server.getReceivedEmailSize());
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("your cookies order", email.getHeaderValue("Subject"));
  }

  public void testTextExpression() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end'>" +
      "      <mail name='send email' actors='me' text='your ${item} order' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("item", "cookies");
    processInstance.signal();

    assertEquals(1, server.getReceivedEmailSize());
    Iterator emailIter = server.getReceivedEmail();
    SmtpMessage email = (SmtpMessage)emailIter.next();
    emailIter.remove();
    
    assertEquals("your cookies order", email.getBody());
  }

  public static class User
  {
    String email;

    public User(String email)
    {
      this.email = email;
    }

    public String getEmail()
    {
      return email;
    }
  }

  public static class TestAddressResolver implements AddressResolver
  {
    private static final long serialVersionUID = 1L;

    public Object resolveAddress(String actorId)
    {
      if ("ghost".equals(actorId))
      {
        return null;
      }
      return actorId + "@example.domain";
    }
  }
}
