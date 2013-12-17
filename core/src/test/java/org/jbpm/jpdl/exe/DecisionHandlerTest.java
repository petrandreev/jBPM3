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
package org.jbpm.jpdl.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.DecisionHandler;

public class DecisionHandlerTest extends AbstractJbpmTestCase {

  public static class LeadEvaluator implements DecisionHandler {
    private static final long serialVersionUID = 1L;

    public String decide(ExecutionContext executionContext) {
      Number wrapper = (Number) executionContext.getContextInstance().getVariable("budget");
      int budget = wrapper.intValue();
      return budget > 1000 ? "important lead" : budget == 777 ? "lucky number"
        : budget > 100 ? "lead" : "beggars";
    }
  }

  ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
    + "  <start-state>"
    + "    <transition to='d' />"
    + "  </start-state>"
    + "  <decision name='d'>"
    + "    <handler class='org.jbpm.jpdl.exe.DecisionHandlerTest$LeadEvaluator'/>"
    + "    <transition name='beggars' to='forget about it'/>"
    + "    <transition name='lead' to='put it in the lead db'/>"
    + "    <transition name='important lead' to='harass them'/>"
    + "    <exception-handler exception-class='java.lang.ClassCastException'/>"
    + "  </decision>"
    + "  <state name='harass them' />"
    + "  <state name='put it in the lead db' />"
    + "  <state name='forget about it' />"
    + "</process-definition>");

  public void testBudgetHignerThenThousand() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(3500));
    processInstance.signal();

    assertEquals(processDefinition.getNode("harass them"), processInstance.getRootToken()
      .getNode());
  }

  public void testBudgetBetweenHundredAndThousand() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(350));
    processInstance.signal();

    assertEquals(processDefinition.getNode("put it in the lead db"), processInstance.getRootToken()
      .getNode());
  }

  public void testSmallBudget() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(35));
    processInstance.signal();

    assertEquals(processDefinition.getNode("forget about it"), processInstance.getRootToken()
      .getNode());
  }

  public void testBadTransitionName() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(777));

    try {
      processInstance.signal();
      fail("expected exception");
    }
    catch (JbpmException e) {
      assert e.getMessage().indexOf("lucky number") != -1;
    }
  }

  public void testHandledException() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", "nothing");
    processInstance.signal();

    // decision node should take default transition
    assertEquals(processDefinition.getNode("forget about it"), processInstance.getRootToken()
      .getNode());
  }
}
