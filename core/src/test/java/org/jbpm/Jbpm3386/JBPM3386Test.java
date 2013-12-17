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
package org.jbpm.Jbpm3386;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.mail.MailTestSetup;
import org.subethamail.smtp.AuthenticationHandler;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.auth.LoginAuthenticationHandler;
import org.subethamail.smtp.auth.LoginFailedException;
import org.subethamail.smtp.auth.PlainAuthenticationHandler;
import org.subethamail.smtp.auth.PluginAuthenticationHandler;
import org.subethamail.smtp.auth.UsernamePasswordValidator;
import org.subethamail.smtp.server.MessageListenerAdapter;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * Auth/relay support in mail nodes.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-3386">JBPM-3386</a>
 */
public class JBPM3386Test extends AbstractJbpmTestCase {

  protected static Logger log = Logger.getLogger(JBPM3386Test.class);

  private JbpmContext jbpmContext;
  private ProcessInstance processInstance;

  private JbpmConfiguration jbpmConfiguration = null;
  private Wiser wiser;

  private static String authUsername = "doug@newcastle.uk";
  private static String authPassword = "newcastlePoolHustler#1!";

  private static final String toAddress = "tester@jboss.org";

  private String getJbpmConfigurationXmlString(int smtpPort ) { 
    String JBPM_CFG_XML = 
      "<jbpm-configuration>"
        + "  <jbpm-context />"
        + "  <string name='resource.mail.templates' value='org/jbpm/jbpm3386/mail.templates.xml' />"
        + "  <int name='jbpm.mail.smtp.port' value='" + smtpPort + "' />"
        + "  <string name='jbpm.mail.user' value='doug@newcastle.uk' />"
        + "  <string name='jbpm.mail.password' value='newcastlePoolHustler#1!' />"
        + "</jbpm-configuration>";
    return JBPM_CFG_XML;
  }

  private int SMTP_PORT;
  
  protected void setUp() throws Exception {
    SMTP_PORT = MailTestSetup.getOpenPort();
    
    wiser = new Wiser();
    MessageListenerAdapter serverMessageListenerAdapter = (MessageListenerAdapter) wiser.getServer()
      .getMessageHandlerFactory();
    serverMessageListenerAdapter.setAuthenticationHandlerFactory(new JBPM3386AuthHandlerFactory());
    wiser.setPort(SMTP_PORT);
    wiser.start();
    
    jbpmConfiguration = JbpmConfiguration.parseXmlString(getJbpmConfigurationXmlString(SMTP_PORT));

    // jbpm test setup
    super.setUp();
    log.info("Using port " + SMTP_PORT);

    jbpmContext = jbpmConfiguration.createJbpmContext();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm3386/processdefinition.xml");
    processInstance = new ProcessInstance(processDefinition);

    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("to", toAddress);

    // start process
    processInstance.signal();
  }

  protected void tearDown() throws Exception {
    if (wiser != null) {
      wiser.getMessages().clear();
      wiser.stop();
      wiser = null;
    }
    jbpmContext.close();
    super.tearDown();
  }

  public void testMailWithAuthentication() throws MessagingException {
    processInstance.signal();
    assertEquals("end", processInstance.getRootToken().getNode().getName());

    List messages = wiser.getMessages();
    assertEquals(1, messages.size());

    for (Iterator iter = messages.iterator(); iter.hasNext();) {
      WiserMessage wiserMessage = (WiserMessage) iter.next();
      MimeMessage message = wiserMessage.getMimeMessage();
      assertEquals("Test message for jbpm 3386", message.getSubject());
      assertTrue(Arrays.equals(InternetAddress.parse(toAddress), message.getRecipients(RecipientType.TO)));
    }
  }

  public void testMailWithAuthenticationFailPassword() throws MessagingException {
    authPassword = "bad password";
    try {
      processInstance.signal();
      fail("An exception should have been thrown here.");
    }
    catch (Exception e) {
      if (!(e instanceof JbpmException)) {
        e.printStackTrace();
        fail("Expected an JbpmException, not an exception of type " + e.getClass().getName() + ": " + e.getMessage());
      }
      
      Throwable cause = e.getCause();
      if(cause == null || !(cause instanceof AuthenticationFailedException)) {
        e.printStackTrace();
        fail("Expected an AuthenticationFailedException, not an exception of type "
          + e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  public void testMailWithAuthenticationFailUser() throws MessagingException {
    authUsername = "trevor";
    try {
      processInstance.signal();
      fail("An exception should have been thrown here.");
    }
    catch (Exception e) {
      if (!(e instanceof JbpmException)) {
        e.printStackTrace();
        fail("Expected an JbpmException, not an exception of type " + e.getClass().getName() + ": " + e.getMessage());
      }
      
      Throwable cause = e.getCause();
      if(cause == null || !(cause instanceof AuthenticationFailedException)) {
        e.printStackTrace();
        fail("Expected an AuthenticationFailedException, not an exception of type "
          + e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }
  
  public static class JBPM3386AuthHandlerFactory implements AuthenticationHandlerFactory {
    public AuthenticationHandler create() {
      PluginAuthenticationHandler ret = new PluginAuthenticationHandler();
      UsernamePasswordValidator validator = new UsernamePasswordValidator() {
        public void login(String username, String password) throws LoginFailedException {
          if (!authUsername.equals(username) || !authPassword.equals(password)) {
            String message = "Tried to login with user/password [" + authUsername + "/" + authPassword
              + "]";
            log.info(message);
            System.out.println(message);
            throw new LoginFailedException("Incorrect password for user " + authUsername);
          }
        }
      };
      ret.addPlugin(new PlainAuthenticationHandler(validator));
      ret.addPlugin(new LoginAuthenticationHandler(validator));
      return ret;
    }
  }
}
