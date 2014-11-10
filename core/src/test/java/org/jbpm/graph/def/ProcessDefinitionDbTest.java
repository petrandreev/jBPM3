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
package org.jbpm.graph.def;

import org.jbpm.context.def.ContextDefinition;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.node.StartState;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

public class ProcessDefinitionDbTest extends AbstractDbTestCase {

  public void testProcessDefinitionVersion() {
    ProcessDefinition processDefinition = new ProcessDefinition("name");
    processDefinition.setVersion(3);

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition);
    assertEquals(3, processDefinition.getVersion());
  }

  public void testProcessDefinitionIsTerminationImplicit() {
    ProcessDefinition processDefinition = new ProcessDefinition("name");
    processDefinition.setTerminationImplicit(false);

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition);
    assertFalse(processDefinition.isTerminationImplicit());
  }

  public void testProcessDefinitionStartState() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.setStartState(new StartState());

    processDefinition = saveAndReload(processDefinition);
    // the start state of a process definition is mapped as a node.
    // therefor the hibernate proxy will be a node
    Node startState = processDefinition.getStartState();
    assertTrue(Node.class.isAssignableFrom(startState.getClass()));
    // reloading gives a better typed proxy
    assertTrue(StartState.class.isAssignableFrom(session.load(StartState.class, new Long(
      startState.getId())).getClass()));
  }

  public void testProcessDefinitionNodes() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.setStartState(new StartState("s"));
    processDefinition.addNode(new Node("a"));
    processDefinition.addNode(new Node("b"));
    processDefinition.addNode(new Node("c"));
    processDefinition.addNode(new Node("d"));

    processDefinition = saveAndReload(processDefinition);
    assertEquals("s", processDefinition.getStartState().getName());
    assertEquals("s", ((Node) processDefinition.getNodes().get(0)).getName());
    assertEquals("a", ((Node) processDefinition.getNodes().get(1)).getName());
    assertEquals("b", ((Node) processDefinition.getNodes().get(2)).getName());
    assertEquals("c", ((Node) processDefinition.getNodes().get(3)).getName());
    assertEquals("d", ((Node) processDefinition.getNodes().get(4)).getName());
  }

  public void testActions() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Action action = new Action();
    action.setName("a");
    processDefinition.addAction(action);
    action = new Action();
    action.setName("b");
    processDefinition.addAction(action);

    processDefinition = saveAndReload(processDefinition);
    assertEquals(2, processDefinition.getActions().size());
    assertNotNull(processDefinition.getActions().get("a"));
    assertNotNull(processDefinition.getActions().get("b"));
    assertTrue(Action.class.isAssignableFrom(processDefinition.getAction("a").getClass()));
    assertTrue(Action.class.isAssignableFrom(processDefinition.getAction("b").getClass()));
  }

  public void testEvents() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addEvent(new Event("node-enter"));
    processDefinition.addEvent(new Event("node-leave"));
    processDefinition.addEvent(new Event("transition"));
    processDefinition.addEvent(new Event("process-start"));
    processDefinition.addEvent(new Event("process-end"));

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition.getEvent("node-enter"));
    assertNotNull(processDefinition.getEvent("node-leave"));
    assertNotNull(processDefinition.getEvent("transition"));
    assertNotNull(processDefinition.getEvent("process-start"));
    assertNotNull(processDefinition.getEvent("process-end"));
  }

  public void testExceptionHandlers() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName("org.disaster.FirstException");
    processDefinition.addExceptionHandler(exceptionHandler);
    exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName("org.disaster.SecondException");
    processDefinition.addExceptionHandler(exceptionHandler);
    exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName("org.disaster.ThirdException");
    processDefinition.addExceptionHandler(exceptionHandler);

    processDefinition = saveAndReload(processDefinition);
    assertEquals("org.disaster.FirstException", ((ExceptionHandler) processDefinition.getExceptionHandlers()
      .get(0)).getExceptionClassName());
    assertEquals("org.disaster.SecondException", ((ExceptionHandler) processDefinition.getExceptionHandlers()
      .get(1)).getExceptionClassName());
    assertEquals("org.disaster.ThirdException", ((ExceptionHandler) processDefinition.getExceptionHandlers()
      .get(2)).getExceptionClassName());
  }

  public void testContextModuleDefinition() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addDefinition(new ContextDefinition());

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition.getContextDefinition());
    assertSame(ContextDefinition.class, processDefinition.getContextDefinition().getClass());
  }

  public void testFileDefinition() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addDefinition(new FileDefinition());

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition.getFileDefinition());
    assertSame(FileDefinition.class, processDefinition.getFileDefinition().getClass());
  }

  public void testTaskMgmtDefinition() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addDefinition(new TaskMgmtDefinition());

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition.getTaskMgmtDefinition());
    assertSame(TaskMgmtDefinition.class, processDefinition.getTaskMgmtDefinition().getClass());
  }
}
