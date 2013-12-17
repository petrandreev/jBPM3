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

public class SuperStateActionExecutionTest extends AbstractJbpmTestCase {

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

  public void testSuperStateEnter() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/insidesuperstate'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='superstate-enter'>" +
      "      <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <state name='insidesuperstate' />" +
      "  </super-state>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.getNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("superstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }

  public void testNestedSuperStateEnter() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/nestedsuperstate/insidenestedsuperstate'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='superstate-enter'>" +
      "      <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <super-state name='nestedsuperstate'>" +
      "      <event type='superstate-enter'>" +
      "        <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "      </event>" +
      "      <state name='insidenestedsuperstate' />" +
      "    </super-state>" +
      "  </super-state>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertEquals(3, executedActions.size());

    // the first action called is the superstate-enter on the 'superstate'
    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.getNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("superstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
    
    // the second action called is the superstate-enter on the 'nestedsuperstate'
    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    // the third action called is the *propagated* event of the 'nestedsuperstate' to the 'superstate'
    executedAction = (ExecutedAction) executedActions.get(2);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }

  public void testSuperStateEnterViaTransitionToSuperState() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='superstate-enter'>" +
      "      <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <super-state name='nestedsuperstate'>" +
      "      <event type='superstate-enter'>" +
      "        <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "      </event>" +
      "      <state name='insidenestedsuperstate' />" +
      "    </super-state>" +
      "  </super-state>" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertEquals(3, executedActions.size());

    // the first action called is the superstate-enter on the 'superstate'
    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.getNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("superstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
    
    // the second action called is the superstate-enter on the 'nestedsuperstate'
    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    // the third action called is the *propagated* event of the 'nestedsuperstate' to the 'superstate'
    executedAction = (ExecutedAction) executedActions.get(2);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }

  public void testSuperStateLeave() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/stateinside'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='superstate-leave'>" +
      "      <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <state name='stateinside'>" +
      "      <transition to='../toplevelstate' />" +
      "    </state>" +
      "  </super-state>" +
      "  <state name='toplevelstate' />" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    // put the execution in the nestedsuperstate
    processInstance.signal();
    assertEquals(0, executedActions.size());
    
    // the next signal results in a node-enter internally to the superstate so it should have no effect.
    // by default, event propagation is turned on.  that is why we decided to have a separated event type for superstate leave and enter. 
    processInstance.signal();
    assertEquals(1, executedActions.size());
    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.getNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.getNode("superstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }

  public void testNestedSuperStateLeave() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/nestedsuperstate/stateinside'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='superstate-leave'>" +
      "      <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <super-state name='nestedsuperstate'>" +
      "      <event type='superstate-leave'>" +
      "        <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "      </event>" +
      "      <state name='stateinside'>" +
      "        <transition to='../../toplevelstate' />" +
      "      </state>" +
      "    </super-state>" +
      "  </super-state>" +
      "  <state name='toplevelstate' />" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    // put the execution in the nestedsuperstate
    processInstance.signal();
    assertEquals(0, executedActions.size());
    
    // the next signal results in a node-enter internally to the superstate so it should have no effect.
    // by default, event propagation is turned on.  that is why we decided to have a separated event type for superstate leave and enter. 
    processInstance.signal();
    assertEquals(3, executedActions.size());
    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    executedAction = (ExecutedAction) executedActions.get(2);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }

  public void testNestedSuperStateLeaveViaSuperStateTransition() {
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='superstate/nestedsuperstate/stateinside'/>" +
      "  </start-state>" +
      "  <super-state name='superstate'>" +
      "    <event type='superstate-leave'>" +
      "      <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "    </event>" +
      "    <super-state name='nestedsuperstate'>" +
      "      <event type='superstate-leave'>" +
      "        <action class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "      </event>" +
      "      <state name='stateinside' />" +
      "    </super-state>" +
      "    <transition to='toplevelstate' />" +
      "  </super-state>" +
      "  <state name='toplevelstate' />" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    // put the execution in the nestedsuperstate
    processInstance.signal();
    assertEquals(0, executedActions.size());
    
    // the next signal results in a node-enter internally to the superstate so it should have no effect.
    // by default, event propagation is turned on.  that is why we decided to have a separated event type for superstate leave and enter. 
    processInstance.signal();
    assertEquals(3, executedActions.size());
    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate/nestedsuperstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    executedAction = (ExecutedAction) executedActions.get(2);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("superstate"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("superstate"), executedAction.eventSource);
    assertSame(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }

  public void testInterNestedSuperStateTransition() {
    //          +--------------------------------------+
    //          |one                                   |
    //          | +---------------+  +---------------+ |
    //          | |one.one        |  |one.two        | | 
    //          | | +-----------+ |  | +-----------+ | |
    //          | | |one.one.one| |  | |one.two.one| | |
    //  +-----+ | | | +---+     | |  | |  +---+    | | |
    //  |start|-+-+-+>| a |-----+-+--+-+->| b |    | | |
    //  +-----+ | | | +---+     | |  | |  +---+    | | |
    //          | | +-----------+ |  | +-----------+ | |
    //          | +---------------+  +---------------+ |
    //          +--------------------------------------+
    processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='one/one.one/one.one.one/a'/>" +
      "  </start-state>" +
      "  <super-state name='one'>" +
      "    <event type='superstate-enter'><action ref-name='record' /></event>" +
      "    <event type='superstate-leave'><action ref-name='record' /></event>" +
      "    <super-state name='one.one'>" +
      "      <event type='superstate-leave'><action ref-name='record' /></event>" +
      "      <super-state name='one.one.one'>" +
      "        <event type='superstate-leave'><action ref-name='record' /></event>" +
      "        <state name='a'>" +
      "          <transition to='../../one.two/one.two.one/b' />" +
      "        </state>" +
      "      </super-state>" +
      "    </super-state>" +
      "    <super-state name='one.two'>" +
      "      <event type='superstate-enter'><action ref-name='record' /></event>" +
      "      <super-state name='one.two.one'>" +
      "        <event type='superstate-enter'><action ref-name='record' /></event>" +
      "        <state name='b' />" +
      "      </super-state>" +
      "    </super-state>" +
      "  </super-state>" +
      "  <action name='record' class='org.jbpm.graph.exe.SuperStateActionExecutionTest$Recorder' />" +
      "</process-definition>"
    );
    // create the process instance
    processInstance = new ProcessInstance(processDefinition);
    // put the execution in the nestedsuperstate
    processInstance.signal();
    assertEquals(3, executedActions.size());
    
    // the next signal results in a node-enter internally to the superstate so it should have no effect.
    // by default, event propagation is turned on.  that is why we decided to have a separated event type for superstate leave and enter. 
    processInstance.signal();
    assertEquals(13, executedActions.size());

    ExecutedAction executedAction = (ExecutedAction) executedActions.get(3);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one/one.one/one.one.one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.one/one.one.one"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(4);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one/one.one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.one/one.one.one"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(5);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.one/one.one.one"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(6);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one/one.one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.one"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(7);
    assertEquals("superstate-leave", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.one"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(8);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one/one.two"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.two"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(9);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.two"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(10);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one/one.two/one.two.one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.two/one.two.one"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(11);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one/one.two"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.two/one.two.one"), executedAction.eventSource);

    executedAction = (ExecutedAction) executedActions.get(12);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertSame(processDefinition.findNode("one"), executedAction.event.getGraphElement());
    assertSame(processDefinition.findNode("one/one.two/one.two.one"), executedAction.eventSource);
  }
}
