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
package org.jbpm.taskmgmt.exe;

import java.util.Iterator;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;

public class RuntimeTaskCreationTest extends AbstractJbpmTestCase {

  public static class CreateTasks implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Token token = executionContext.getToken();
      TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();

      TaskNode taskNode = (TaskNode) executionContext.getNode();
      Task changeNappy = taskNode.getTask("change nappy");

      // now, 2 task instances are created for the same task.
      tmi.createTaskInstance(changeNappy, token);
      tmi.createTaskInstance(changeNappy, token);
    }
  }

  public void testRuntimeTaskCreation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a' create-tasks='false'>"
      + "    <event type='node-enter'>"
      + "      <action class='org.jbpm.taskmgmt.exe.RuntimeTaskCreationTest$CreateTasks' />"
      + "    </event>"
      + "    <task name='change nappy' />'"
      + "    <task name='make bottle' />'"
      + "    <task name='do dishes' />'"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    assertNotNull(tmi);
    assertEquals(2, tmi.getTaskInstances().size());
    for (Iterator iter = tmi.getTaskInstances().iterator(); iter.hasNext();) {
      TaskInstance taskInstance = (TaskInstance) iter.next();
      assertEquals("change nappy", taskInstance.getName());
    }
  }
}
