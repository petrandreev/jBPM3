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
package org.jbpm.jbpm1914;

import java.io.IOException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Problem in retrieving variables of Serializable objects.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1914">JBPM-1914</a>
 * @author Alejandro Guizar
 */
public class JBPM1914Test extends AbstractDbTestCase {

  @Override
  protected String getJbpmTestConfig() {
    return "org/jbpm/jbpm1914/jbpm.cfg.xml";
  }

  public void testCustomSerializableVariableClass() throws IOException {
    // create and save the process definition
    ProcessDefinition processDefinition = ProcessDefinition.parseParResource("org/jbpm/jbpm1024/CustomSerializable.zip");
    graphSession.deployProcessDefinition(processDefinition);
    SessionFactory sessionFactory = jbpmContext.getSessionFactory();

    commitAndCloseSession();
    try {
      Session session = sessionFactory.openSession();
      Transaction transaction = session.beginTransaction();

      jbpmContext = jbpmConfiguration.createJbpmContext();
      jbpmContext.setSession(session);

      // create the process instance
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      jbpmContext.save(processInstance);
      jbpmContext.close();

      transaction.commit();
      session.close();

      // get the custom object from the context instance
      beginSessionTransaction();
      processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
      Object customSerializable = processInstance.getContextInstance().getVariable(
          "custom serializable");
      assertEquals("org.jbpm.context.exe.CustomSerializableClass", customSerializable.getClass()
          .getName());
      assertEquals("1984", customSerializable.toString());
    }
    finally {
      newTransaction();
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
