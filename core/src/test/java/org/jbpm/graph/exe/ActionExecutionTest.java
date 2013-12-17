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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;

public class ActionExecutionTest extends AbstractJbpmTestCase {
  
  ProcessDefinition processDefinition = null;
  ProcessInstance processInstance = null;

  static List executedActions = null;
  
  public static class ExecutedAction {
    // ExectionContext members
    Token token = null;
    Event event = null;
    Action action = null;
    Throwable exception = null;
    // The node returned by the ExecutionContext at the time of execution
    Node node = null;
  }
  
  public static class Recorder implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      ExecutedAction executedAction = new ExecutedAction();
      executedAction.token = executionContext.getToken();
      executedAction.event = executionContext.getEvent();
      executedAction.action = executionContext.getAction();
      executedAction.exception = executionContext.getException();
      executedAction.node = executionContext.getNode();
      executedActions.add(executedAction);
    }
  }
  
  protected void setUp() throws Exception
  {
    super.setUp();
    executedActions = new ArrayList();
  }

  public void testProcessStartEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <event type='process-start'>" +
      "    <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "  </event>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertSame(getNode("start"), findExecutedAction(Event.EVENTTYPE_PROCESS_START).node);
  }

  public void testProcessEndEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "  <event type='process-end'>" +
      "    <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "  </event>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());
    processInstance.signal();
    assertEquals(0, executedActions.size());
    processInstance.signal();
    assertEquals(1, executedActions.size());
    assertSame(getNode("end"), findExecutedAction(Event.EVENTTYPE_PROCESS_END).node);
  }

  public void testProcessBeforeSignalEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <event type='before-signal'>" +
      "    <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "  </event>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());

    // leave the start state by sending a signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = findExecutedAction(Event.EVENTTYPE_BEFORE_SIGNAL);
    assertSame(getNode("start"), executedAction.node);
    assertSame(processDefinition, executedAction.event.getGraphElement());

    // leave the state by sending another signal
    processInstance.signal();
    assertEquals(2, executedActions.size());
  }

  public void testProcessAfterSignalEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <event type='after-signal'>" +
      "    <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "  </event>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());

    // leave the start state by sending a signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = findExecutedAction(Event.EVENTTYPE_AFTER_SIGNAL);
    assertSame(getNode("state"), executedAction.node);
    assertSame(processDefinition, executedAction.event.getGraphElement());

    // leave the state by sending another signal
    processInstance.signal();
    assertEquals(2, executedActions.size());
  }
  
  public void testNodeBeforeSignalEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <event type='before-signal'>" +
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());

    // leave the start state by sending a signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = findExecutedAction(Event.EVENTTYPE_BEFORE_SIGNAL);
    assertSame(getNode("start"), executedAction.node);
    assertSame(getNode("start"), executedAction.event.getGraphElement());

    // leave the state by sending another signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
  }

  public void testNodeAfterSignalEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <event type='after-signal'>" +
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());

    // leave the start state by sending a signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = findExecutedAction(Event.EVENTTYPE_AFTER_SIGNAL);
    assertSame(getNode("state"), executedAction.node);
    assertSame(getNode("start"), executedAction.event.getGraphElement());

    // leave the state by sending another signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
  }
  
  public void testNodeEnterEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <event type='node-enter'>" +
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());

    // leave the start state by sending a signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = findExecutedAction(Event.EVENTTYPE_NODE_ENTER);
    assertSame(getNode("state"), executedAction.node);
    assertSame(getNode("state"), executedAction.event.getGraphElement());

    // leave the state by sending another signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
  }

  public void testNodeLeaveEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <event type='node-leave'>" +
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <transition to='end'/>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertEquals(0, executedActions.size());

    // leave the start state by sending a signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = findExecutedAction(Event.EVENTTYPE_NODE_LEAVE);
    assertSame(getNode("state"), executedAction.node);
    assertSame(getNode("state"), executedAction.event.getGraphElement());
  }

  public void testTransitionEvent() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='state'/>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <transition to='end'>" +
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$Recorder' />" +
      "    </transition>" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertEquals(0, executedActions.size());

    // leave the start state by sending a signal
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = findExecutedAction(Event.EVENTTYPE_TRANSITION);
    assertNull(executedAction.node);
    assertSame(getNode("state").getDefaultLeavingTransition(), executedAction.event.getGraphElement());
  }
  
  static List sequence = new ArrayList();
  public static class SequenceRecorder implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      Event event = executionContext.getEvent();
      sequence.add(event.getGraphElement().getName()+" "+event.getEventType());
    }
  }
  
  public void testExecutionSequence() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='process'>" +
      "  <event type='process-start'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "  <start-state name='start-state'>" +
      "    <event type='node-enter'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='node-leave'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='before-signal'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='after-signal'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <transition name='start-to-state' to='state'>" +
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <state name='state'>" +
      "    <event type='node-enter'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='node-leave'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='before-signal'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='after-signal'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <transition name='state-to-end' to='end-state'>" +
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' />" +
      "    </transition>" +
      "  </state>" +
      "  <end-state name='end-state'>" +
      "    <event type='node-enter'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='node-leave'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='before-signal'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "    <event type='after-signal'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "  </end-state>" +
      "  <event type='process-end'><action class='org.jbpm.graph.exe.ActionExecutionTest$SequenceRecorder' /></event>" +
      "</process-definition>"
    );
    
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    processInstance.signal();
    
    // format of the sequence messages : 
    //   node-name event-type
    // separated by a space
    assertEquals("process process-start", sequence.get(0));
    assertEquals("start-state before-signal", sequence.get(1));
    assertEquals("start-state node-leave", sequence.get(2));
    assertEquals("start-to-state transition", sequence.get(3));
    assertEquals("state node-enter", sequence.get(4));
    assertEquals("start-state after-signal", sequence.get(5));
    assertEquals("state before-signal", sequence.get(6));
    assertEquals("state node-leave", sequence.get(7));
    assertEquals("state-to-end transition", sequence.get(8));
    assertEquals("end-state node-enter", sequence.get(9));
    assertEquals("process process-end", sequence.get(10));
    assertEquals("state after-signal", sequence.get(11));
  }

  private Node getNode(String nodeName) {
    return processDefinition.getNode(nodeName);
  }

  private ExecutedAction findExecutedAction(String eventType) {
    Iterator iter = executedActions.iterator();
    while (iter.hasNext()) {
      ExecutedAction executedAction = (ExecutedAction) iter.next();
      if (eventType.equals(executedAction.event.getEventType())) {
        return executedAction;
      }
    }
    throw new RuntimeException("no action was executed on eventtype '"+eventType+"'");
  }
  
  public static class ProblematicActionHandler implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      throw new IllegalArgumentException("problematic problem");
    }
  }
  
  public void testProblematicReferencedAction() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" + 
      "  <start-state name='start'>" + 
      "    <transition to='state'>" + 
      "      <action ref-name='problematic action'/>" + 
      "    </transition>"+ 
      "  </start-state>" + 
      "  <state name='state' />" + 
      "  <action name='problematic action' class='org.jbpm.graph.exe.ActionExecutionTest$$ProblematicActionHandler'/>"+ 
      "</process-definition>");
    
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    try {
      processInstance.signal();
      fail("expected exception");
    } catch (DelegationException e) {
      // OK
    }
  }

  public static class SignallingActionHandler implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      executionContext.getToken().signal();
    }
  }
  
  public void testAttemptToSignalInAnAction() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" + 
      "  <start-state name='start'>" + 
      "    <transition to='state'/>" +
      "    <event type='node-leave'>" + 
      "      <action class='org.jbpm.graph.exe.ActionExecutionTest$SignallingActionHandler'/>" + 
      "    </event>"+ 
      "  </start-state>" + 
      "  <state name='state' />" + 
      "</process-definition>");
    
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    try {
      processInstance.signal();
      fail("expected exception");
    } catch (JbpmException e) {
      // OK
    }
  }
}