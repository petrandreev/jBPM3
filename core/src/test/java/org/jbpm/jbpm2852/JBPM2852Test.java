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
package org.jbpm.jbpm2852;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.mail.MailTestSetup;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * CC support in mail nodes and mail templates.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2852">JBPM-2852</a>
 * @author Alejandro Guizar
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JBPM2852Test extends AbstractJbpmTestCase {

  private JbpmContext jbpmContext;
  private ProcessInstance processInstance;

  private JbpmConfiguration getJbpmConfiguration(int smtpPort) {
    return JbpmConfiguration.parseXmlString(XML_DECL
      + "<jbpm-configuration>"
      + "  <jbpm-context />"
      + "  <string name='resource.mail.templates' value='org/jbpm/jbpm2852/mail.templates.xml' />"
      + "  <int name='jbpm.mail.smtp.port' value='" + smtpPort + "' />"
      + "</jbpm-configuration>");
  }

  private Wiser wiser;

  protected void setUp() throws Exception {
    super.setUp();
    wiser = MailTestSetup.getWiser();
    int smtpPort = wiser.getServer().getPort();
    jbpmContext = getJbpmConfiguration(smtpPort).createJbpmContext();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2852/processdefinition.xml");
    processInstance = new ProcessInstance(processDefinition);

    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("technician", "technician@example.com");
    contextInstance.setVariable("supervisor", "supervisor@example.com");
    contextInstance.setVariable("observer", "observer@example.com");
    processInstance.signal();
  }

  protected void tearDown() throws Exception {
    wiser.getMessages().clear();
    wiser.stop();
    wiser = null;

    jbpmContext.close();
    super.tearDown();
  }

  public void testMailNodeInlineCC() throws MessagingException {
    processInstance.signal("high");
    assertEquals("alert", processInstance.getRootToken().getNode().getName());

    List messages = wiser.getMessages();
    assertEquals(3, messages.size());

    for (Iterator iter = messages.iterator(); iter.hasNext();) {
      WiserMessage wiserMessage = (WiserMessage) iter.next();
      MimeMessage message = wiserMessage.getMimeMessage();
      assertEquals("Reactor temperature exceeded threshold", message.getSubject());
      assert Arrays.equals(InternetAddress.parse("technician@example.com"), message.getRecipients(RecipientType.TO));
      assert Arrays.equals(InternetAddress.parse("supervisor@example.com"), message.getRecipients(RecipientType.CC));
      // bcc recipients undisclosed
      assertNull(message.getRecipients(RecipientType.BCC));
    }
  }

  public void testMailActionTemplateCC() throws MessagingException {
    processInstance.signal("normal");
    assertEquals("ok", processInstance.getRootToken().getNode().getName());

    List messages = wiser.getMessages();
    assertEquals(3, messages.size());

    for (Iterator iter = messages.iterator(); iter.hasNext();) {
      WiserMessage wiserMessage = (WiserMessage) iter.next();
      MimeMessage message = wiserMessage.getMimeMessage();
      assertEquals("Reactor temperature normal", message.getSubject());
      assert Arrays.equals(InternetAddress.parse("technician@example.com"), message.getRecipients(RecipientType.TO));
      assert Arrays.equals(InternetAddress.parse("supervisor@example.com"), message.getRecipients(RecipientType.CC));
      // bcc recipients undisclosed
      assertNull(message.getRecipients(RecipientType.BCC));
    }
  }
}
