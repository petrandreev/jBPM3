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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;

public class NodeDbTest extends AbstractDbTestCase {

  public void testNodeName() {
    Node node = new Node("n");

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition);
    assertEquals("n", processDefinition.getNode("n").getName());
  }

  public void testNodeProcessDefinition() {
    Node node = new Node("n");

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition);
    assertSame(processDefinition, processDefinition.getNode("n").getProcessDefinition());
  }

  public void testNodeEvents() {
    Node node = new Node("n");
    node.addEvent(new Event("node-enter"));
    node.addEvent(new Event("node-leave"));
    node.addEvent(new Event("transition"));
    node.addEvent(new Event("process-start"));
    node.addEvent(new Event("process-end"));

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    node = processDefinition.getNode("n");
    assertNotNull(node.getEvent("node-enter"));
    assertNotNull(node.getEvent("node-leave"));
    assertNotNull(node.getEvent("transition"));
    assertNotNull(node.getEvent("process-start"));
    assertNotNull(node.getEvent("process-end"));
  }

  public void testNodeExceptionHandlers() {
    ExceptionHandler exceptionHandler1 = new ExceptionHandler();
    exceptionHandler1.setExceptionClassName("org.disaster.FirstException");

    ExceptionHandler exceptionHandler2 = new ExceptionHandler();
    exceptionHandler2.setExceptionClassName("org.disaster.SecondException");

    ExceptionHandler exceptionHandler3 = new ExceptionHandler();
    exceptionHandler3.setExceptionClassName("org.disaster.ThirdException");

    Node node = new Node("n");
    node.addExceptionHandler(exceptionHandler1);
    node.addExceptionHandler(exceptionHandler2);
    node.addExceptionHandler(exceptionHandler3);

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    node = processDefinition.getNode("n");
    List exceptionHandlers = node.getExceptionHandlers();

    exceptionHandler1 = (ExceptionHandler) exceptionHandlers.get(0);
    assertEquals("org.disaster.FirstException", exceptionHandler1.getExceptionClassName());

    exceptionHandler2 = (ExceptionHandler) exceptionHandlers.get(1);
    assertEquals("org.disaster.SecondException", exceptionHandler2.getExceptionClassName());

    exceptionHandler3 = (ExceptionHandler) exceptionHandlers.get(2);
    assertEquals("org.disaster.ThirdException", exceptionHandler3.getExceptionClassName());
  }

  public void testNodeLeavingTransitions() {
    Node a = new Node("a");
    Node b = new Node("b");

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(a);
    processDefinition.addNode(b);

    Transition t = new Transition("one");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    t = new Transition("two");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    t = new Transition("three");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    processDefinition = saveAndReload(processDefinition);
    a = processDefinition.getNode("a");
    b = processDefinition.getNode("b");

    assertEquals("one", ((Transition) a.getLeavingTransitionsList().get(0)).getName());
    assertEquals("two", ((Transition) a.getLeavingTransitionsList().get(1)).getName());
    assertEquals("three", ((Transition) a.getLeavingTransitionsList().get(2)).getName());

    assertSame(b, a.getLeavingTransition("one").getTo());
    assertSame(b, a.getLeavingTransition("two").getTo());
    assertSame(b, a.getLeavingTransition("three").getTo());
  }

  public void testNodeArrivingTransitions() {
    Node a = new Node("a");
    Node b = new Node("b");

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(a);
    processDefinition.addNode(b);

    Transition t = new Transition("one");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    t = new Transition("two");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    t = new Transition("three");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    processDefinition = saveAndReload(processDefinition);
    a = processDefinition.getNode("a");
    b = processDefinition.getNode("b");

    Iterator arrivingTransitionIter = b.getArrivingTransitions().iterator();
    assertSame(b, ((Transition) arrivingTransitionIter.next()).getTo());
    assertSame(b, ((Transition) arrivingTransitionIter.next()).getTo());
    assertSame(b, ((Transition) arrivingTransitionIter.next()).getTo());

    Collection expectedTransitionNames = new HashSet(Arrays.asList(new String[] {
      "one", "two", "three"
    }));
    arrivingTransitionIter = b.getArrivingTransitions().iterator();
    expectedTransitionNames.remove(((Transition) arrivingTransitionIter.next()).getName());
    expectedTransitionNames.remove(((Transition) arrivingTransitionIter.next()).getName());
    expectedTransitionNames.remove(((Transition) arrivingTransitionIter.next()).getName());
    assertEquals(0, expectedTransitionNames.size());
  }

  public void testNodeAction() {
    Action action = new Action();
    action.setName("a");

    Node node = new Node("n");
    node.setAction(action);

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    assertNotNull(processDefinition.getNode("n").getAction());
  }

  public void testNodeSuperState() {
    Node node = new Node("n");

    SuperState superState = new SuperState("s");
    superState.addNode(node);

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(superState);

    processDefinition = saveAndReload(processDefinition);
    superState = (SuperState) processDefinition.getNode("s");
    node = superState.getNode("n");
    assertNotNull(node);
    assertNotNull(superState);
    assertSame(node, superState.getNode("n"));
  }
}
