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
package org.jbpm.examples.rulesaction;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * This example shows how to invoke JBoss Drools from an ActionHandler.
 */
public class RulesActionTest extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    deployProcess();
  }

  void deployProcess() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("rulesaction/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  public void testRulesAssignment() {
    // start process
    long processInstanceId = createNewProcessInstance();
    assertFalse("ProcessInstanceId is 0", processInstanceId == 0);

    newTransaction();
    ProcessInstance processInstance = jbpmContext.loadProcessInstance(processInstanceId);
    assertTrue("ProcessInstance has not ended", processInstance.hasEnded());

    String shipper = (String) processInstance.getContextInstance().getVariable("shipper");
    assertEquals("FEDX", shipper);
  }

  long createNewProcessInstance() {
    String processDefinitionName = "RulesAction";
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(processDefinitionName);

    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("processDefinitionName", processDefinitionName);

    Order order = new Order(300);
    Customer customer = new Customer("Fred", new Integer(5), new Integer(25), new Long(100000));
    contextInstance.setVariable("order", order);
    contextInstance.setVariable("customer", customer);

    processInstance.signal();
    return processInstance.getId();
  }
}
