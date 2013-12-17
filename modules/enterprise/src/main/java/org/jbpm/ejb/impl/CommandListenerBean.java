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
package org.jbpm.ejb.impl;

import java.io.Serializable;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.command.Command;
import org.jbpm.ejb.LocalCommandService;
import org.jbpm.ejb.LocalCommandServiceHome;

/**
 * This message-driven bean listens for {@link ObjectMessage object messages}
 * containing a command instance. The received commands are 
 * executed by the {@link CommandServiceBean command service} bean, using the
 * local interface.
 * 
 * The body of the message must be a Java object that implements the {@link 
 * Command} interface. The message properties, if any, are ignored.
 * 
 * <h3>Environment</h3>
 * 
 * <p>The environment entries and resources available for customization are
 * summarized in the table below.</p>
 * 
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><code>ejb/LocalCommandServiceBean</code></td>
 * <td>EJB Reference</td>
 * <td>Link to the local {@linkplain CommandServiceBean session bean} that
 * executes commands on a separate jBPM context.
 * </td>
 * </tr>
 * <tr>
 * <td><code>jms/JbpmConnectionFactory</code></td>
 * <td>Resource Manager Reference</td>
 * <td>Logical name of the factory that provides JMS connections for producing
 * result messages. Required for command messages that indicate a reply
 * destination.
 * </td>
 * </tr>
 * <tr>
 * <td><code>jms/DeadLetterQueue</code></td>
 * <td>Message Destination Reference</td>
 * <td>Messages which do not contain a command are sent to the queue referenced
 * here. Optional; if absent, such messages are rejected, which may cause the 
 * container to redeliver.
 * </td>
 * </tr>
 * </table>
 * 
 * @author Jim Rigsbee
 * @author Tom Baeyens
 * @author Alejandro Guizar
 */
public class CommandListenerBean implements MessageDrivenBean, MessageListener
{

  private static final long serialVersionUID = 1L;

  MessageDrivenContext messageDrivenContext = null;
  LocalCommandService commandService;
  Connection jmsConnection;
  Destination deadLetterQueue;

  public void onMessage(Message message)
  {
    try
    {
      // extract command from message
      Command command = extractCommand(message);
      if (command == null)
      {
        discard(message);
        return;
      }
      // execute command via local command executor bean
      Object result = commandService.execute(command);
      // send a response back if a "reply to" destination is set
      Destination replyTo = message.getJMSReplyTo();
      if (replyTo != null && (result instanceof Serializable || result == null))
      {
        sendResult((Serializable)result, replyTo, message.getJMSMessageID());
      }
    }
    catch (JMSException e)
    {
      messageDrivenContext.setRollbackOnly();
      log.error("could not process message " + message, e);
    }
  }

  protected Command extractCommand(Message message) throws JMSException
  {
    Command command = null;
    if (message instanceof ObjectMessage)
    {
      log.debug("deserializing command from jms message...");
      ObjectMessage objectMessage = (ObjectMessage)message;
      Serializable object = objectMessage.getObject();
      if (object instanceof Command)
      {
        command = (Command)object;
      }
      else
      {
        log.warn("ignoring object message cause it isn't a command '" + object + "'" + (object != null ? " (" + object.getClass().getName() + ")" : ""));
      }
    }
    else
    {
      log.warn("ignoring message '" + message + "' cause it isn't an ObjectMessage (" + message.getClass().getName() + ")");
    }
    return command;
  }

  private void discard(Message message) throws JMSException
  {
    if (deadLetterQueue == null)
    {
      // lookup dead letter queue
      try
      {
        Context initial = new InitialContext();
        deadLetterQueue = (Destination)initial.lookup("java:comp/env/jms/DeadLetterQueue");
      }
      catch (NamingException e)
      {
        log.debug("failed to retrieve dead letter queue, rejecting message: " + message);
        messageDrivenContext.setRollbackOnly();
        return;
      }
    }
    // send message to dead letter queue
    Session jmsSession = createSession();
    try
    {
      jmsSession.createProducer(deadLetterQueue).send(message);
    }
    finally
    {
      jmsSession.close();
    }
  }

  private void sendResult(Serializable result, Destination destination, String correlationId) throws JMSException
  {
    log.debug("sending result " + result + " to " + destination);
    Session jmsSession = createSession();
    try
    {
      Message resultMessage = jmsSession.createObjectMessage(result);
      resultMessage.setJMSCorrelationID(correlationId);
      jmsSession.createProducer(destination).send(resultMessage);
    }
    finally
    {
      jmsSession.close();
    }
  }

  private Session createSession() throws JMSException
  {
    if (jmsConnection == null)
    {
      // lookup factory and create jms connection
      try
      {
        Context initial = new InitialContext();
        ConnectionFactory jmsConnectionFactory = (ConnectionFactory)initial.lookup("java:comp/env/jms/JbpmConnectionFactory");
        jmsConnection = jmsConnectionFactory.createConnection();
      }
      catch (NamingException e)
      {
        throw new EJBException("error retrieving jms connection factory", e);
      }
    }
    /*
     * if the connection supports xa, the session will be transacted, else the session will auto acknowledge; in either case no explicit transaction control must be
     * performed - see ejb 2.1 - 17.3.5
     */
    return jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
  }

  public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext)
  {
    this.messageDrivenContext = messageDrivenContext;
  }

  public void ejbRemove()
  {
    if (jmsConnection != null)
    {
      try
      {
        jmsConnection.close();
      }
      catch (JMSException e)
      {
        log.debug("failed to close jms connection", e);
      }
      jmsConnection = null;
    }
    deadLetterQueue = null;
    commandService = null;
    messageDrivenContext = null;
  }

  public void ejbCreate()
  {
    try
    {
      Context initial = new InitialContext();
      LocalCommandServiceHome commandServiceHome = (LocalCommandServiceHome)initial.lookup("java:comp/env/ejb/LocalCommandServiceBean");
      commandService = commandServiceHome.create();
    }
    catch (NamingException e)
    {
      throw new EJBException("error retrieving command service home", e);
    }
    catch (CreateException e)
    {
      throw new EJBException("error creating command service", e);
    }
  }

  private static final Log log = LogFactory.getLog(CommandListenerBean.class);
}
