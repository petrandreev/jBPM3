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

public class ProcessInstanceLogTest extends AbstractJbpmTestCase {

  public void testProcessInstanceCreateLog() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    List processInstanceCreateLogs = loggingInstance.getLogs(ProcessInstanceCreateLog.class);
    assertEquals(1, processInstanceCreateLogs.size());
    ProcessInstanceCreateLog processInstanceCreateLog = (ProcessInstanceCreateLog) processInstanceCreateLogs.get(0);
    assertSame(processInstance.getRootToken(), processInstanceCreateLog.getToken());
  }

  public void testNoRootTokenCreateLog() {
    // the root token logs are replaced by the process instance logs
    ProcessDefinition processDefinition = new ProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    assertEquals(0, loggingInstance.getLogs(TokenCreateLog.class).size());
  }

  public void testProcessInstanceEndLog() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.end();
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    List processInstanceEndLogs = loggingInstance.getLogs(ProcessInstanceEndLog.class);
    assertEquals(1, processInstanceEndLogs.size());
    ProcessInstanceEndLog processInstanceEndLog = (ProcessInstanceEndLog) processInstanceEndLogs.get(0);
    assertSame(processInstance.getRootToken(), processInstanceEndLog.getToken());
  }

  public void testNoRootTokenEndLog() {
    // the root token logs are replaced by the process instance logs
    ProcessDefinition processDefinition = new ProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getRootToken().end();
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    assertEquals(0, loggingInstance.getLogs(TokenEndLog.class).size());
  }
}
