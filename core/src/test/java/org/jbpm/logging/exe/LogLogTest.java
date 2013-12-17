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
package org.jbpm.logging.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.log.MessageLog;

public class LogLogTest extends AbstractJbpmTestCase {
  
  public static class MessageAction implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      executionContext.getToken().addLog(new MessageLog("hello from inside the message action"));
      executionContext.getContextInstance().setVariable("number", new Float(3.3));
    }
  }

  public void testLoggingTheLogs() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='fork'/>" +
      "  </start-state>" +
      "  <fork name='fork'>" +
      "    <transition name='a' to='a'>" +
      "      <action class='org.jbpm.logging.exe.LogLogTest$MessageAction' />" +
      "    </transition>" +
      "    <transition name='b' to='b' />" +
      "  </fork>" +
      "  <state name='a' />" +
      "  <end-state name='b' />" +
      "</process-definition>"
    );
    processDefinition.addDefinition(new ContextDefinition());
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("number", new Float(5.5));
    contextInstance.setVariable("text", "one of the few");
    processInstance.signal();
    
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    loggingInstance.logLogs();
  }
}
