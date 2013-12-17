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
package org.jbpm.graph.log;

import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.exe.LoggingInstance;

public class NodeLogTest extends AbstractJbpmTestCase {
  
  public void testNodeLogCreation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='s' />" +
      "  </start-state>" +
      "  <state name='s'>" +
      "    <transition to='s' />" +
      "  </state>" +
      "</process-definition>" 
    );
    // start a process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    assertEquals(0, loggingInstance.getLogs(NodeLog.class).size());

    processInstance.signal();
    
    List nodeLogs = loggingInstance.getLogs(NodeLog.class);
    assertEquals(1, nodeLogs.size());
    NodeLog nodeLog = (NodeLog) nodeLogs.get(0);
    assertSame(processDefinition.getNode("s"), nodeLog.node);
    assertNotNull(nodeLog.enter);
    assertNotNull(nodeLog.leave);
    assertEquals(nodeLog.leave.getTime()-nodeLog.enter.getTime(), nodeLog.duration);
  }
}
