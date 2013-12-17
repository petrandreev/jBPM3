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
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.exe.LoggingInstance;

public class ProcessLogsTest extends AbstractJbpmTestCase {

  public void testProcessInstanceStartLog() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition />"
    );
    
    // start a process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    
    // check the transition log (from the start state to the state)
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    List processLogs = loggingInstance.getLogs(ProcessInstanceCreateLog.class);
    
    assertEquals(1, processLogs.size());

    ProcessInstanceCreateLog processInstanceCreateLog = (ProcessInstanceCreateLog) processLogs.get(0);
    assertSame(token, processInstanceCreateLog.getToken());
    assertNotNull(processInstanceCreateLog.getDate());
  }

  public void testProcessInstanceEndLog() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end' />" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>" 
    );
    
    // start a process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    // check the transition log (from the start state to the state)
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    List processLogs = loggingInstance.getLogs(ProcessInstanceEndLog.class);
    
    assertEquals(1, processLogs.size());

    ProcessInstanceEndLog processInstanceEndLog = (ProcessInstanceEndLog) processLogs.get(0);
    assertSame(token, processInstanceEndLog.getToken());
    assertNotNull(processInstanceEndLog.getDate());
  }

}
