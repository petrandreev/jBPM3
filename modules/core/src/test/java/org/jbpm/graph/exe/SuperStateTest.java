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
package org.jbpm.graph.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;

public class SuperStateTest extends AbstractJbpmTestCase {

  public void testTakeSuperStateTransition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/insidesuperstate'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <state name='insidesuperstate' />" +
      "    <transition to='s' />" +
      "  </super-state>" +
      "  <state name='s' />" +
      "</process-definition>"
    );
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    assertEquals(processDefinition.findNode("superstate/insidesuperstate"), token.getNode());
    processInstance.signal();
    assertEquals(processDefinition.getNode("s"), token.getNode());
  }

  public void testTransitionToSuperState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <state name='insidesuperstate' />" +
      "  </super-state>" +
      "</process-definition>"
    );
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    assertEquals(processDefinition.findNode("superstate/insidesuperstate"), token.getNode());
  }

}
