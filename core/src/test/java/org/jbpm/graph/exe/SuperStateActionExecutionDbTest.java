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

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;

public class SuperStateActionExecutionDbTest extends AbstractDbTestCase {

  static List executedActions = new ArrayList();

  public static class ExecutedAction {
    Token token;
    Event event;
    GraphElement eventSource;
    Action action;
    Throwable exception;
    Node node;

    public ExecutedAction(ExecutionContext executionContext) {
      token = executionContext.getToken();
      event = executionContext.getEvent();
      eventSource = executionContext.getEventSource();
      action = executionContext.getAction();
      exception = executionContext.getException();
      node = executionContext.getNode();
    }
  }

  public static class Recorder implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      executedActions.add(new ExecutedAction(executionContext));
    }
  }

  public void testSuperStateEnterViaTransitionToSuperState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='enterviatransitiontosuperstate'>"
      + "  <start-state name='start'>"
      + "    <transition to='superstate'/>"
      + "  </start-state>"
      + "  <super-state name='superstate'>"
      + "    <event type='superstate-enter'>"
      + "      <action class='"
      + Recorder.class.getName()
      + "' />"
      + "    </event>"
      + "    <super-state name='nestedsuperstate'>"
      + "      <event type='superstate-enter'>"
      + "        <action class='"
      + Recorder.class.getName()
      + "' />"
      + "      </event>"
      + "      <state name='insidenestedsuperstate' />"
      + "    </super-state>"
      + "  </super-state>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    // create the process instance
    ProcessInstance processInstance = jbpmContext.newProcessInstance("enterviatransitiontosuperstate");

    processInstance = saveAndReload(processInstance);
    processInstance.signal();

    assertEquals(3, executedActions.size());

    // the first action called is the superstate-enter on the 'superstate'
    ExecutedAction executedAction = (ExecutedAction) executedActions.get(0);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertEquals("superstate", executedAction.event.getGraphElement().getName());
    assertEquals("superstate", executedAction.eventSource.getName());
    assertEquals(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    // the second action called is the superstate-enter on the
    // 'nestedsuperstate'
    executedAction = (ExecutedAction) executedActions.get(1);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertEquals("nestedsuperstate", executedAction.event.getGraphElement().getName());
    assertEquals("nestedsuperstate", executedAction.eventSource.getName());
    assertEquals(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);

    // the third action called is the *propagated* event of the
    // 'nestedsuperstate' to the 'superstate'
    executedAction = (ExecutedAction) executedActions.get(2);
    assertEquals("superstate-enter", executedAction.event.getEventType());
    assertEquals("superstate", executedAction.event.getGraphElement().getName());
    assertEquals("nestedsuperstate", executedAction.eventSource.getName());
    assertEquals(processInstance.getRootToken(), executedAction.token);
    assertNull(executedAction.node);
  }
}
