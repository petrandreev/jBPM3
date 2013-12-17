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
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;

public class EventPropagationTest extends AbstractJbpmTestCase {
        
  ProcessDefinition processDefinition = null;
  ProcessInstance processInstance = null;

  static List executedActions = null;
  
  public static class ExecutedAction {
    // ExectionContext members
    Token token = null;
    Event event = null;
    GraphElement eventSource = null;
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
      executedAction.eventSource = executionContext.getEventSource();
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

  public void testNodeToProcessEventPropagation() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <event type='node-enter'>" +
      "    <action class='org.jbpm.graph.exe.EventPropagationTest$Recorder' />" +
      "  </event>" +
      "  <event type='node-leave'>" +
      "    <action class='org.jbpm.graph.exe.EventPropagationTest$Recorder' />" +
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

    processInstance.signal();
    
    assertEquals(2, executedActions.size());

    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("node-leave", executedAction.event.getEventType());
    assertSame(processDefinition, executedAction.event.getGraphElement());
    assertSame(processDefinition.getStartState(), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertSame(processDefinition.getStartState(), executedAction.node);

    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("node-enter", executedAction.event.getEventType());
    assertSame(processDefinition, executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("state"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertSame(processDefinition.getNode("state"), executedAction.node);

    processInstance.signal();

    assertEquals(4, executedActions.size());

    executedAction = (ExecutedAction) executedActions.get(2);
    assertEquals("node-leave", executedAction.event.getEventType());
    assertSame(processDefinition, executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("state"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertSame(processDefinition.getNode("state"), executedAction.node);

    executedAction = (ExecutedAction) executedActions.get(3);
    assertEquals("node-enter", executedAction.event.getEventType());
    assertSame(processDefinition, executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("end"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertSame(processDefinition.getNode("end"), executedAction.node);
  }

  public void testTransitionToProcessEventPropagation() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <event type='transition'>" +
      "    <action class='org.jbpm.graph.exe.EventPropagationTest$Recorder' />" +
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

    processInstance.signal();
    
    assertEquals(1, executedActions.size());

    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("transition", executedAction.event.getEventType());
    assertSame(processDefinition, executedAction.event.getGraphElement());
    assertSame(processDefinition.getStartState().getDefaultLeavingTransition(), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    processInstance.signal();

    assertEquals(2, executedActions.size());

    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("transition", executedAction.event.getEventType());
    assertSame(processDefinition, executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("state").getDefaultLeavingTransition(), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }

  public void testNodeToSuperStateEventPropagation() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/state'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='node-enter'>" +
      "      <action class='org.jbpm.graph.exe.EventPropagationTest$Recorder' />" +
      "    </event>" +
      "    <event type='node-leave'>" +
      "      <action class='org.jbpm.graph.exe.EventPropagationTest$Recorder' />" +
      "    </event>" +
      "    <state name='state'>" +
      "      <transition to='../end'/>" +
      "    </state>" +
      "  </super-state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());

    processInstance.signal();
    
    assertEquals(1, executedActions.size());

    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("node-enter", executedAction.event.getEventType());
    assertSame(processDefinition.getNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/state"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertSame(processDefinition.findNode("superstate/state"), executedAction.node);

    processInstance.signal();
    
    assertEquals(2, executedActions.size());

    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("node-leave", executedAction.event.getEventType());
    assertSame(processDefinition.getNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/state"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertSame(processDefinition.findNode("superstate/state"), executedAction.node);
  }

  public void testTransitionToSuperStateEventPropagation() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/state'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='transition'>" +
      "      <action class='org.jbpm.graph.exe.EventPropagationTest$Recorder' />" +
      "    </event>" +
      "    <state name='state'>" +
      "      <transition to='../end'/>" +
      "      <transition name='loop' to='state'/>" +
      "    </state>" +
      "  </super-state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    assertEquals(0, executedActions.size());

    processInstance.signal();

    assertEquals(0, executedActions.size());

    processInstance.signal("loop");

    assertEquals(1, executedActions.size());

    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("transition", executedAction.event.getEventType());
    assertSame(processDefinition.getNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/state").getLeavingTransition("loop"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    processInstance.signal();

    assertEquals(1, executedActions.size());
  }
  
  // let's count the number of messages printed.
  // that way we have something to assert at the end of the test.
  static int nbrOfMessagesPrinted = 0; 

  public static class PrintMessage implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      nbrOfMessagesPrinted++;
    }
  }

  public void testStraightThrough() {

    nbrOfMessagesPrinted = 0;
    
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='phase one/a' />" +
      "  </start-state>" +
      "  <super-state name='phase one'>" +
      "    <event type='transition'>" +
      "      <action ref-name='print action' accept-propagated-events='false'/>" +
      "    </event>" +
      "    <state name='a'>" +
      "      <transition to='b' />" +
      "    </state>" +
      "    <state name='b'>" +
      "      <transition to='/phase two/c' />" +
      "      <transition name='back' to='a' />" +
      "    </state>" +
      "    <transition name='cancel' to='cancelled' />" +
      "  </super-state>" +
      "  <super-state name='phase two'>" +
      "    <state name='c'>" +
      "      <transition to='d' />" +
      "    </state>" +
      "    <state name='d'>" +
      "      <transition to='/end' />" +
      "    </state>" +
      "  </super-state>" +
      "  <end-state name='end' />" +
      "  <end-state name='cancelled' />" +
      "  <action name='print action' class='"+PrintMessage.class.getName()+"' />" +
      "</process-definition>"
    );
  
    Node a = processDefinition.findNode("phase one/a");
    Node b = processDefinition.findNode("phase one/b");
    Node c = processDefinition.findNode("phase two/c");
    Node d = processDefinition.findNode("phase two/d");
    Node end = processDefinition.getNode("end");

    // starting a new process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    
    processInstance.signal();
    assertEquals(a, token.getNode());
    processInstance.signal();
    assertEquals(b, token.getNode());
    processInstance.signal("back");
    assertEquals(a, token.getNode());
    processInstance.signal();
    assertEquals(b, token.getNode());
    processInstance.signal();
    assertEquals(c, token.getNode());
    processInstance.signal();
    assertEquals(d, token.getNode());
    processInstance.signal();
    assertEquals(end, token.getNode());
    assertEquals(0, nbrOfMessagesPrinted);
  }

}
