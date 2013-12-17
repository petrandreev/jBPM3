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
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.exe.LoggingInstance;

public class ActionLogTest extends AbstractJbpmTestCase {
  
  public static class LoggedAction implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      // just for testing the logs, so we don't have to do anything here
    }
  }
  
  public void testSimpleActionLog() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='state'>" +
      "      <action class='org.jbpm.graph.log.ActionLogTest$LoggedAction' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <state name='state' />" +
      "</process-definition>" 
    );
    
    // start a process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    // check the transition log (from the start state to the state)
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    List actionLogs = loggingInstance.getLogs(ActionLog.class);
    
    assertEquals(1, actionLogs.size());

    ActionLog actionLog = (ActionLog) actionLogs.get(0);
    assertSame(token, actionLog.getToken());
    assertSame(LoggedAction.class, actionLog.getAction().getActionDelegation().getInstance().getClass());
    assertNull(actionLog.getException());
    assertNotNull(actionLog.getDate());
  }

  public static class LoggedExceptionAction implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      throw new RuntimeException("please, log me");
    }
  }
  
  public void testActionExceptionLog() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='state'>" +
      "      <action class='org.jbpm.graph.log.ActionLogTest$LoggedExceptionAction' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <state name='state' />" +
      "</process-definition>" 
    );
    
    // start a process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    try {
      processInstance.signal();
      fail("expected exception");
    } catch (RuntimeException e) {

      // check the transition log (from the start state to the state)
      LoggingInstance loggingInstance = processInstance.getLoggingInstance();
      List actionLogs = loggingInstance.getLogs(ActionLog.class);
      
      assertEquals(1, actionLogs.size());

      ActionLog actionLog = (ActionLog) actionLogs.get(0);
      assertSame(token, actionLog.getToken());
      assertSame(LoggedExceptionAction.class, actionLog.getAction().getActionDelegation().getInstance().getClass());
      assertNotNull(actionLog.getException());
      assertNotNull(actionLog.getDate());
    }
  }
}
