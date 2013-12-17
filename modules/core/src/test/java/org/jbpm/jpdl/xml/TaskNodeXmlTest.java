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
package org.jbpm.jpdl.xml;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.scheduler.def.CancelTimerAction;
import org.jbpm.scheduler.def.CreateTimerAction;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

public class TaskNodeXmlTest extends AbstractJbpmTestCase {
  
  public void testTaskNodeName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t'>" +
      "  </task-node>" +
      "</process-definition>" 
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    assertNotNull(taskNode);
    assertEquals("t", taskNode.getName());
  }
  
  public void testTaskNodeTasks() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t'>" +
      "    <task name='one' />" +
      "    <task name='two' />" +
      "    <task name='three' />" +
      "  </task-node>" +
      "</process-definition>");
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    assertNotNull(taskNode);
    assertEquals(3, taskNode.getTasks().size());
  }

  public void testTaskNodeDefaultSignal() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t' />" +
      "</process-definition>" 
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    assertEquals(TaskNode.SIGNAL_LAST, taskNode.getSignal());
  }

  public void testTaskNodeSignalFirst() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t' signal='first' />" +
      "</process-definition>" 
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    assertEquals(TaskNode.SIGNAL_FIRST, taskNode.getSignal());
  }

  public void testTaskNodeDefaultCreate() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t' />" +
      "</process-definition>" 
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    assertTrue(taskNode.getCreateTasks());
  }

  public void testTaskNodeNoCreate() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <task-node name='t' create-tasks='false'/>" +
      "</process-definition>" 
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    assertFalse(taskNode.getCreateTasks());
  }

  public void testSwimlane() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <swimlane name='initiator'>" +
      "    <assignment class='assignment-specified-just-to-prevent-a-warning'/>" +
      "  </swimlane>" +
      "</process-definition>" 
    );
    TaskMgmtDefinition taskMgmtDefinition = processDefinition.getTaskMgmtDefinition();
    Swimlane initiatorSwimlane = taskMgmtDefinition.getSwimlane("initiator");
    assertNotNull(initiatorSwimlane);
    assertEquals("initiator", initiatorSwimlane.getName());
  }

  public void testTaskSwimlane() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <swimlane name='initiator'>" +
      "    <assignment class='assignment-specified-just-to-prevent-a-warning'/>" +
      "  </swimlane>" +
      "  <task name='grow old' swimlane='initiator' />" +
      "</process-definition>" 
    );
    Task growOld = processDefinition.getTaskMgmtDefinition().getTask("grow old");
    assertNotNull(growOld);
    assertNotNull(growOld.getSwimlane());
    assertEquals("initiator", growOld.getSwimlane().getName());
  }

  
  public void testTaskCreationEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-create'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("a");
    Task task = taskNode.getTask("clean ceiling");
    assertNotNull( task.getEvent(Event.EVENTTYPE_TASK_CREATE) );
  }

  public void testTaskStartEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-start'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("a");
    Task task = taskNode.getTask("clean ceiling");
    assertNotNull( task.getEvent(Event.EVENTTYPE_TASK_START) );
  }

  public void testTaskAssignEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-assign'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("a");
    Task task = taskNode.getTask("clean ceiling");
    assertNotNull( task.getEvent(Event.EVENTTYPE_TASK_ASSIGN) );
  }

  public void testTaskEndEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <event type='task-end'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventTest$PlusPlus' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("a");
    Task task = taskNode.getTask("clean ceiling");
    assertNotNull( task.getEvent(Event.EVENTTYPE_TASK_END) );
  }

  public void testTaskTimer() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <timer duedate='2 business minutes'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventTest$PlusPlus' />" +
      "      </timer>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("a");
    Task task = taskNode.getTask("clean ceiling");
    Event event = task.getEvent(Event.EVENTTYPE_TASK_CREATE);
    assertNotNull(event);
    CreateTimerAction createTimerAction = (CreateTimerAction) event.getActions().get(0);
    assertNotNull(createTimerAction);
    assertEquals("2 business minutes", createTimerAction.getDueDate());

    // test default cancel event
    event = task.getEvent(Event.EVENTTYPE_TASK_END);
    assertNotNull(event);
    CancelTimerAction cancelTimerAction = (CancelTimerAction) event.getActions().get(0);
    assertNotNull(cancelTimerAction);
  }

  public void testTaskTimerCancelEvents() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <task-node name='a'>" +
      "    <task name='clean ceiling'>" +
      "      <timer duedate='2 business minutes' cancel-event='task-start, task-assign, task-end'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskEventTest$PlusPlus' />" +
      "      </timer>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    TaskNode taskNode = (TaskNode) processDefinition.getNode("a");
    Task task = taskNode.getTask("clean ceiling");
    Event event = task.getEvent(Event.EVENTTYPE_TASK_CREATE);
    assertNotNull(event);
    assertSame(CreateTimerAction.class, event.getActions().get(0).getClass());

    event = task.getEvent(Event.EVENTTYPE_TASK_START);
    assertNotNull(event);
    assertSame(CancelTimerAction.class, event.getActions().get(0).getClass());

    event = task.getEvent(Event.EVENTTYPE_TASK_ASSIGN);
    assertNotNull(event);
    assertSame(CancelTimerAction.class, event.getActions().get(0).getClass());

    event = task.getEvent(Event.EVENTTYPE_TASK_END);
    assertNotNull(event);
    assertSame(CancelTimerAction.class, event.getActions().get(0).getClass());
  }

}
