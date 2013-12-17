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
package org.jbpm.examples.mail;

import java.util.Arrays;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.mail.AddressResolver;

public class MailTest extends AbstractJbpmTestCase {

  private JbpmContext jbpmContext;
  private Wiser wiser = new Wiser();

  private static JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseResource("mail/jbpm.cfg.xml");

  protected void setUp() throws Exception {
    super.setUp();
    jbpmContext = jbpmConfiguration.createJbpmContext();
    wiser.setPort(2525);
    wiser.start();
  }

  protected void tearDown() throws Exception {
    wiser.stop();
    jbpmContext.close();
    super.tearDown();
  }

  public void testSimpleProcess() throws MessagingException {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("mail/processdefinition.xml");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List messages = wiser.getMessages();
    assertEquals(1, messages.size());

    WiserMessage message = (WiserMessage) messages.get(0);
    MimeMessage email = message.getMimeMessage();
    assert Arrays.equals(InternetAddress.parse("grandma@dalton.com"), email.getRecipients(RecipientType.TO));

    Address[] from = email.getFrom();
    assertEquals(1, from.length);
    InternetAddress fromAddress = (InternetAddress) from[0];
    assertEquals("no-reply@jbpm.org", fromAddress.getAddress());
  }

  public static class MyAddressResolver implements AddressResolver {
    private static final long serialVersionUID = 1L;

    public Object resolveAddress(String actorId) {
      return actorId + "@dalton.com";
    }
  }
}
