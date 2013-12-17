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
package org.jbpm.graph.node;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

public class StartStateDbTest extends AbstractDbTestCase {

  public void testStartState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state name='start' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node startState = processDefinition.getNode("start");
    assertEquals("start", startState.getName());
    assertSame(startState, processDefinition.getStartState());
  }

  public void testStartStateSwimlane() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='initiator' />"
      + "  <start-state name='start'>"
      + "    <task swimlane='initiator' />"
      + "  </start-state>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
    Swimlane initiatorSwimlaneViaStartTask = taskMgmtDefinition.getStartTask().getSwimlane();
    assertNotNull(initiatorSwimlaneViaStartTask);

    Swimlane initiatorSwimlaneViaDefinition = taskMgmtDefinition.getSwimlane("initiator");
    assertSame(initiatorSwimlaneViaDefinition, initiatorSwimlaneViaStartTask);
  }
}
