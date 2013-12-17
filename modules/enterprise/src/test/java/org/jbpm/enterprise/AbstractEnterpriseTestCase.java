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
package org.jbpm.enterprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.cactus.ServletTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Environment;
import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.command.CommandService;
import org.jbpm.command.DeleteProcessDefinitionCommand;
import org.jbpm.command.DeployProcessCommand;
import org.jbpm.command.GetProcessInstanceCommand;
import org.jbpm.command.SignalCommand;
import org.jbpm.command.StartProcessInstanceCommand;
import org.jbpm.ejb.LocalCommandServiceHome;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

public abstract class AbstractEnterpriseTestCase extends ServletTestCase {

  protected CommandService commandService;

  private List<ProcessDefinition> processDefinitions = new ArrayList<ProcessDefinition>();

  private static Context environment;
  private static LocalCommandServiceHome commandServiceHome;
  private static Destination commandQueue;
  private static ConnectionFactory jmsConnectionFactory;

  private static final Log log = LogFactory.getLog(AbstractEnterpriseTestCase.class);

  protected AbstractEnterpriseTestCase() {
  }

  protected void setUp() throws Exception {
    commandService = createCommandService();
    log.info("### " + getName() + " started ###");
  }

  protected void tearDown() throws Exception {
    log.info("### " + getName() + " done ###");
    for (ProcessDefinition processDefinition : processDefinitions) {
      deleteProcessDefinition(processDefinition.getId());
    }
    commandService = null;
    EventCallback.clear();
  }

  protected CommandService createCommandService() throws Exception {
    if (commandServiceHome == null) {
      commandServiceHome = (LocalCommandServiceHome) getEnvironment()
          .lookup("ejb/CommandServiceBean");
    }
    return commandServiceHome.create();
  }

  protected ProcessDefinition deployProcessDefinition(String xml) {
    ProcessDefinition processDefinition = (ProcessDefinition) commandService.execute(
        new DeployProcessCommand(xml));
    processDefinitions.add(processDefinition);
    return processDefinition;
  }

  protected ProcessDefinition deployProcessDefinition(byte[] processArchive) {
    ProcessDefinition processDefinition = (ProcessDefinition) commandService.execute(
        new DeployProcessCommand(processArchive));
    processDefinitions.add(processDefinition);
    return processDefinition;
  }

  protected ProcessInstance startProcessInstance(String processName) {
    StartProcessInstanceCommand command = new StartProcessInstanceCommand();
    command.setProcessDefinitionName(processName);
    command.setVariables(Collections.singletonMap("eventCallback", new EventCallback()));
    return (ProcessInstance) commandService.execute(command);
  }

  protected void signalToken(long tokenId) {
    commandService.execute(new SignalCommand(tokenId, null));
  }

  protected boolean hasProcessInstanceEnded(final long processInstanceId) {
    ProcessInstance processInstance = (ProcessInstance) commandService.execute(
        new GetProcessInstanceCommand(processInstanceId));
    return processInstance.hasEnded();
  }

  protected Object getVariable(final long processInstanceId, final String variableName) {
    return commandService.execute(new Command() {
      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        ProcessInstance processInstance = jbpmContext.loadProcessInstance(processInstanceId);
        return processInstance.getContextInstance().getVariable(variableName);
      }
    });
  }

  protected String getHibernateDialect() {
    return (String) commandService.execute(new Command() {
      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        DbPersistenceServiceFactory factory = (DbPersistenceServiceFactory) jbpmContext
            .getServiceFactory(Services.SERVICENAME_PERSISTENCE);
        return factory.getConfiguration().getProperty(Environment.DIALECT);
      }
    });
  }

  private void deleteProcessDefinition(long processDefinitionId) throws Exception {
    if (true) {
      // [JBPM-1812] Fix tests that don't cleanup the database
      // deleting process definition makes subsequent tests unstable
      return;
    }

    Connection jmsConnection = getConnectionFactory().createConnection();
    try {
      Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      try {
        Command command = new DeleteProcessDefinitionCommand(processDefinitionId);
        jmsSession.createProducer(getCommandQueue()).send(jmsSession.createObjectMessage(command));
      }
      finally {
        jmsSession.close();
      }
    }
    finally {
      jmsConnection.close();
    }
  }

  private Destination getCommandQueue() throws NamingException {
    if (commandQueue == null) {
      commandQueue = (Destination) getEnvironment().lookup("jms/CommandQueue");
    }
    return commandQueue;
  }

  private ConnectionFactory getConnectionFactory() throws NamingException {
    if (jmsConnectionFactory == null) {
      jmsConnectionFactory = (ConnectionFactory) getEnvironment()
          .lookup("jms/JbpmConnectionFactory");
    }
    return jmsConnectionFactory;
  }

  private static Context getEnvironment() throws NamingException {
    if (environment == null) {
      Context initial = new InitialContext();
      try {
        environment = (Context) initial.lookup("java:comp/env");
      }
      finally {
        initial.close();
      }
    }
    return environment;
  }

}