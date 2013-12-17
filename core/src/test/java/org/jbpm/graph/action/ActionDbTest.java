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
package org.jbpm.graph.action;

import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;

public class ActionDbTest extends AbstractDbTestCase {

  public void testIsPropagationAllowedFalse() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'>"
      + "      <action name='a' class='unimportant' accept-propagated-events='false' />"
      + "    </event>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Action action = processDefinition.getAction("a");
    assertFalse(action.acceptsPropagatedEvents());
  }

  public void testIsPropagationAllowedTrue() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'>"
      + "      <action name='a' class='unimportant' accept-propagated-events='true' />"
      + "    </event>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Action action = processDefinition.getAction("a");
    assertTrue(action.acceptsPropagatedEvents());
  }

  public void testReferencedAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'>"
      + "      <action ref-name='a'/>"
      + "    </event>"
      + "  </node>"
      + "  <action name='a' class='unimportant'/>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Action nodeAction = (Action) processDefinition.getNode("n")
      .getEvent("node-enter")
      .getActions()
      .get(0);
    assertSame(processDefinition.getAction("a"), nodeAction.getReferencedAction());
  }

  public void testActionDelegation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <action name='a' class='myclass' config-type='constructor'>"
      + "    <myconfiguration></myconfiguration>"
      + "  </action>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Action action = processDefinition.getAction("a");
    assertNotNull(action.getActionDelegation());
  }

  public void testEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'>"
      + "      <action name='a' class='unimportant'/>"
      + "    </event>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Event event = processDefinition.getNode("n").getEvent("node-enter");
    Action action = processDefinition.getAction("a");
    assertSame(event, action.getEvent());
  }

  public void testProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'>"
      + "      <action name='a' class='unimportant'/>"
      + "    </event>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Action action = processDefinition.getAction("a");
    assertSame(processDefinition, action.getProcessDefinition());
  }

  public void testProcessDefinitionActionMap() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <action name='a' class='unimportant'/>"
      + "  <action name='b' class='unimportant'/>"
      + "  <action name='c' class='unimportant'/>"
      + "  <action name='d' class='unimportant'/>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertEquals("a", processDefinition.getAction("a").getName());
    assertEquals("b", processDefinition.getAction("b").getName());
    assertEquals("c", processDefinition.getAction("c").getName());
    assertEquals("d", processDefinition.getAction("d").getName());
  }

  public void testNodeEventCascading() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'>"
      + "      <action name='a' class='unimportant'/>"
      + "      <action name='b' class='unimportant'/>"
      + "      <action name='c' class='unimportant'/>"
      + "      <action name='d' class='unimportant'/>"
      + "    </event>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    List actions = processDefinition.getNode("n").getEvent("node-enter").getActions();
    assertEquals(processDefinition.getAction("a"), actions.get(0));
    assertEquals(processDefinition.getAction("b"), actions.get(1));
    assertEquals(processDefinition.getAction("c"), actions.get(2));
    assertEquals(processDefinition.getAction("d"), actions.get(3));
  }

  public void testTransitionEventCascading() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition name='t' to='n'>"
      + "      <action name='a' class='unimportant'/>"
      + "      <action name='b' class='unimportant'/>"
      + "      <action name='c' class='unimportant'/>"
      + "      <action name='d' class='unimportant'/>"
      + "    </transition>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    List actions = processDefinition.getNode("n")
      .getLeavingTransition("t")
      .getEvent("transition")
      .getActions();
    assertEquals(processDefinition.getAction("a"), actions.get(0));
    assertEquals(processDefinition.getAction("b"), actions.get(1));
    assertEquals(processDefinition.getAction("c"), actions.get(2));
    assertEquals(processDefinition.getAction("d"), actions.get(3));
  }
}
