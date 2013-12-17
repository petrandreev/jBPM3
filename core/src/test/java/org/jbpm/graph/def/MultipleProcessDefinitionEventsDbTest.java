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

import java.util.Map;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.node.State;

public class MultipleProcessDefinitionEventsDbTest extends AbstractDbTestCase {

  public void testEventPersistence() {
    // Add a start state so that state '1' gets assigned id = 2
    ProcessDefinition processDefinitionOne = ProcessDefinition.parseXmlString("<process-definition name='one'>"
      + "  <start-state name='start'>"
      + "    <transition name='start transition to 1' to='1' />"
      + "  </start-state>"
      + "  <state name='1'>"
      + "    <event type='node-enter'>"
      + "      <action class='foo' />"
      + "    </event>"
      + "  </state>"
      + "</process-definition>");
    deployProcessDefinition(processDefinitionOne);

    ProcessDefinition processDefinitionTwo = ProcessDefinition.parseXmlString("<process-definition name='two'>"
      + "  <state name='1'>"
      + "    <event type='node-enter'>"
      + "      <action class='bar' />"
      + "    </event>"
      + "  </state>"
      + "</process-definition>");
    deployProcessDefinition(processDefinitionTwo);

    processDefinitionOne = graphSession.loadProcessDefinition(processDefinitionOne.getId());
    processDefinitionTwo = graphSession.loadProcessDefinition(processDefinitionTwo.getId());

    State stateOne = (State) processDefinitionOne.getNode("1");
    State stateTwo = (State) processDefinitionTwo.getNode("1");
    assertTrue(stateOne.getEvent("node-enter") != stateTwo.getEvent("node-enter"));

    Map processEvents = processDefinitionTwo.getEvents();
    assertEquals("Process Definition should not have any events. events = " + processEvents, 0,
      processEvents.size());
  }
}
