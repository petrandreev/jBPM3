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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class TaskEventExecutionTest extends AbstractJbpmTestCase {
  
  static int counter = 0;
  
  public static class PlusPlus implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      counter++;
    }
  }
  
  protected void setUp() {
    counter = 0;
  }

  public void testTaskCreationEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-create'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventExecutionTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, counter);
    processInstance.signal();
    assertEquals(1, counter);
  }

  public void testTaskStartEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-start'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventExecutionTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    assertEquals(0, counter);
    processInstance.signal();
    assertEquals(0, counter);
    
    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();
    taskInstance.start();
    assertEquals(1, counter);
  }

  public void testTaskAssignEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-assign'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventExecutionTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    assertEquals(0, counter);
    processInstance.signal();
    assertEquals(0, counter);
    
    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();
    taskInstance.start();
    assertEquals(0, counter);
    
    taskInstance.setActorId("john doe");
    assertEquals(1, counter);
  }

  public void testTaskEndEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-end'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventExecutionTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "    <transition to='a' />" +
      "  </task-node>" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    assertEquals(0, counter);
    processInstance.signal();
    assertEquals(0, counter);
    
    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();
    taskInstance.start();
    assertEquals(0, counter);
    
    taskInstance.setActorId("john doe");
    assertEquals(0, counter);
    
    taskInstance.end();
    assertEquals(1, counter);
  }
}
