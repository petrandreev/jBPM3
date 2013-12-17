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
package org.jbpm.jbpm1775;

import org.hibernate.TransactionException;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.persistence.db.DbPersistenceService;

/**
 * Verify if exception handlers collection can be loaded lazily.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1775">JBPM-1775</a>
 * @author Alejandro Guizar
 */
public class JBPM1775Test extends AbstractDbTestCase {

  public void testExceptionInAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<?xml version='1.0'?>"
      + "<process-definition name='jbpm1775'>"
      + "  <exception-handler exception-class='" + TransactionException.class.getName() + "'>"
      + "    <action class='org.example.NoSuchAction' />"
      + "  </exception-handler>"
      + "  <start-state name='start'>"
      + "    <transition to='end'>"
      + "      <action class='" + RollbackAction.class.getName() + "' />"
      + "    </transition>"
      + "  </start-state>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);
    try {
      ProcessInstance processInstance = jbpmContext.newProcessInstance(processDefinition.getName());
      processInstance.signal();
      fail("expected delegation exception");
    }
    catch (DelegationException e) {
      jbpmContext.setRollbackOnly();
      assert e.getCause() instanceof TransactionException : e.getCause();

      try {
        closeJbpmContext();
      }
      catch (JbpmPersistenceException pe) {
        // discard failure to close normally
      }
      createJbpmContext();
    }
  }

  public static class RollbackAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      // NOTE that manipulating the transaction directly is bad practice
      // use JbpmContext or TxService instead!
      DbPersistenceService persistenceService = (DbPersistenceService) executionContext.getJbpmContext()
        .getServices()
        .getPersistenceService();
      persistenceService.getTransaction().rollback();
      // throw exception
      throw new TransactionException("transaction rolled back");
    }
  }
}
