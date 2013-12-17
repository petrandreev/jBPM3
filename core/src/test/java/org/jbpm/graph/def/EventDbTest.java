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
package org.jbpm.graph.def;

import java.util.List;

import org.jbpm.db.AbstractDbTestCase;

public class EventDbTest extends AbstractDbTestCase {

  public void testEventEventType() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <event type='process-start' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertEquals("process-start", processDefinition.getEvent("process-start").getEventType());
  }

  public void testEventGraphElementProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <event type='process-start' />"
      + "</process-definition>");
    assertSame(processDefinition, processDefinition.getEvent("process-start").getGraphElement());

    processDefinition = saveAndReload(processDefinition);
    assertSame(processDefinition, processDefinition.getEvent("process-start").getGraphElement());
  }

  public void testEventGraphElementNode() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'/>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertSame(processDefinition.getNode("n"), processDefinition.getNode("n")
      .getEvent("node-enter")
      .getGraphElement());
  }

  public void testEventGraphElementTransition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition name='t' to='n'>"
      + "      <action class='unimportant'/>"
      + "    </transition>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Transition t = processDefinition.getNode("n").getLeavingTransition("t");
    assertSame(t, t.getEvent("transition").getGraphElement());
  }

  public void testEventActions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <event type='process-start'>"
      + "    <action class='a'/>"
      + "    <action class='b'/>"
      + "    <action class='c'/>"
      + "    <action class='d'/>"
      + "  </event>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    List actions = processDefinition.getEvent("process-start").getActions();
    assertEquals("a", ((Action) actions.get(0)).getActionDelegation().getClassName());
    assertEquals("b", ((Action) actions.get(1)).getActionDelegation().getClassName());
    assertEquals("c", ((Action) actions.get(2)).getActionDelegation().getClassName());
    assertEquals("d", ((Action) actions.get(3)).getActionDelegation().getClassName());
  }
}
