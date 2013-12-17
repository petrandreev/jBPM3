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
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class DecisionConditionsTest extends AbstractJbpmTestCase {
  
  ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
    "<process-definition>" +
    "  <start-state>" +
    "    <transition to='d' />" +
    "  </start-state>" +
    "  <decision name='d'>" +
    "    <transition to='forget about it'/>" +
    "    <transition name='imporant lead' to='harras them'>" +
    "      <condition>#{budget > 1000}</condition>" +
    "    </transition>" +
    "    <transition name='lead' to='put it in the lead db'>" +
    "      <condition>#{budget > 100}</condition>" +
    "    </transition>" +
    "  </decision>" +
    "  <state name='harras them' />" +
    "  <state name='put it in the lead db' />" +
    "  <state name='forget about it' />" +
    "</process-definition>" );
  
  public void testBudgetHignerThenThousand() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(3500));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("harras them"), processInstance.getRootToken().getNode());
  }

  public void testBudgetBetweenHundredAndThousand() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(350));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("put it in the lead db"), processInstance.getRootToken().getNode());
  }

  public void testSmallBudget() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(35));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("forget about it"), processInstance.getRootToken().getNode());
  }

}
