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

import org.jbpm.AbstractJbpmTestCase;

public class ParentTest extends AbstractJbpmTestCase {
  
  ProcessDefinition processDefinition = null;

  public void testProcessDefinitionParent() {
    assertNull(new ProcessDefinition().getParent());
  }

  public void testNodeInProcessParents() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );

    assertSame(processDefinition, processDefinition.getStartState().getParent());
    assertSame(processDefinition, processDefinition.getNode("state").getParent());
    assertSame(processDefinition, processDefinition.getNode("end").getParent());
  }

  public void testTransitionInProcessParents() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    assertSame(processDefinition, processDefinition.getStartState().getDefaultLeavingTransition().getParent());
    assertSame(processDefinition, processDefinition.getNode("state").getDefaultLeavingTransition().getParent());
  }
  
  public void testNodeInSuperProcessParent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/state'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <state name='state'>" +
      "      <transition to='../end'/>" +
      "    </state>" +
      "  </super-state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );

    SuperState superState = (SuperState) processDefinition.getNode("superstate");

    assertSame(processDefinition, processDefinition.getStartState().getParent());
    assertSame(processDefinition, superState.getParent());
    assertSame(processDefinition, processDefinition.getNode("end").getParent());
    assertSame(superState, processDefinition.findNode("superstate/state").getParent());
  }

  public void testTransitionInSuperProcessParent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/state'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <state name='state'>" +
      "      <transition to='../end'/>" +
      "      <transition name='loop' to='state'/>" +
      "      <transition name='tostate2' to='state2'/>" +
      "    </state>" +
      "    <state name='state2' />" +
      "  </super-state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );

    SuperState superState = (SuperState) processDefinition.getNode("superstate");

    assertSame(processDefinition, processDefinition.getStartState().getDefaultLeavingTransition().getParent());
    assertSame(processDefinition, processDefinition.findNode("superstate/state").getDefaultLeavingTransition().getParent());
    assertSame(superState, processDefinition.findNode("superstate/state").getLeavingTransition("loop").getParent());
    assertSame(superState, processDefinition.findNode("superstate/state").getLeavingTransition("tostate2").getParent());
  }
}
