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

import org.jbpm.db.AbstractDbTestCase;

public class NodeDbTest extends AbstractDbTestCase
{

  public void testNodeName()
  {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node node = new Node("n");
    processDefinition.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    try
    {
      assertNotNull(processDefinition);
      assertEquals("n", processDefinition.getNode("n").getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testNodeProcessDefinition()
  {
    ProcessDefinition processDefinition = new ProcessDefinition("p");
    Node node = new Node("n");
    processDefinition.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    try
    {
      assertNotNull(processDefinition);
      assertEquals("p", processDefinition.getNode("n").getProcessDefinition().getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testNodeEvents()
  {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node node = new Node("n");
    processDefinition.addNode(node);
    node.addEvent(new Event("node-enter"));
    node.addEvent(new Event("node-leave"));
    node.addEvent(new Event("transition"));
    node.addEvent(new Event("process-start"));
    node.addEvent(new Event("process-end"));

    processDefinition = saveAndReload(processDefinition);
    try
    {
      node = processDefinition.getNode("n");
      assertNotNull(node.getEvent("node-enter"));
      assertNotNull(node.getEvent("node-leave"));
      assertNotNull(node.getEvent("transition"));
      assertNotNull(node.getEvent("process-start"));
      assertNotNull(node.getEvent("process-end"));
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testNodeExceptionHandlers()
  {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node node = new Node("n");
    processDefinition.addNode(node);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName("org.disaster.FirstException");
    node.addExceptionHandler(exceptionHandler);
    exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName("org.disaster.SecondException");
    node.addExceptionHandler(exceptionHandler);
    exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName("org.disaster.ThirdException");
    node.addExceptionHandler(exceptionHandler);

    processDefinition = saveAndReload(processDefinition);
    try
    {
      assertEquals("org.disaster.FirstException", ((ExceptionHandler)processDefinition.getNode("n").getExceptionHandlers().get(0)).getExceptionClassName());
      assertEquals("org.disaster.SecondException", ((ExceptionHandler)processDefinition.getNode("n").getExceptionHandlers().get(1)).getExceptionClassName());
      assertEquals("org.disaster.ThirdException", ((ExceptionHandler)processDefinition.getNode("n").getExceptionHandlers().get(2)).getExceptionClassName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testNodeLeavingTransitions()
  {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node a = new Node("a");
    Node b = new Node("b");
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
    try
    {
      a = processDefinition.getNode("a");
      b = processDefinition.getNode("b");

      assertEquals("one", ((Transition)a.getLeavingTransitionsList().get(0)).getName());
      assertEquals("two", ((Transition)a.getLeavingTransitionsList().get(1)).getName());
      assertEquals("three", ((Transition)a.getLeavingTransitionsList().get(2)).getName());

      assertSame(b, a.getLeavingTransition("one").getTo());
      assertSame(b, a.getLeavingTransition("two").getTo());
      assertSame(b, a.getLeavingTransition("three").getTo());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testNodeArrivingTransitions()
  {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node a = new Node("a");
    Node b = new Node("b");
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
    try
    {
      a = processDefinition.getNode("a");
      b = processDefinition.getNode("b");

      Iterator arrivingTransitionIter = b.getArrivingTransitions().iterator();
      assertSame(b, ((Transition)arrivingTransitionIter.next()).getTo());
      assertSame(b, ((Transition)arrivingTransitionIter.next()).getTo());
      assertSame(b, ((Transition)arrivingTransitionIter.next()).getTo());

      Collection expectedTransitionNames = new HashSet(Arrays.asList(new String[] { "one", "two", "three" }));
      arrivingTransitionIter = b.getArrivingTransitions().iterator();
      expectedTransitionNames.remove(((Transition)arrivingTransitionIter.next()).getName());
      expectedTransitionNames.remove(((Transition)arrivingTransitionIter.next()).getName());
      expectedTransitionNames.remove(((Transition)arrivingTransitionIter.next()).getName());
      assertEquals(0, expectedTransitionNames.size());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testNodeAction()
  {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node node = new Node("n");
    processDefinition.addNode(node);
    Action action = new Action();
    action.setName("a");
    node.setAction(action);

    processDefinition = saveAndReload(processDefinition);
    try
    {
      assertNotNull(processDefinition.getNode("n").getAction());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testNodeSuperState()
  {
    ProcessDefinition processDefinition = new ProcessDefinition();
    SuperState superState = new SuperState("s");
    processDefinition.addNode(superState);
    Node node = new Node("n");
    superState.addNode(node);

    processDefinition = saveAndReload(processDefinition);
    try
    {
      superState = (SuperState)processDefinition.getNode("s");
      node = superState.getNode("n");
      assertNotNull(node);
      assertNotNull(superState);
      assertSame(node, superState.getNode("n"));
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
