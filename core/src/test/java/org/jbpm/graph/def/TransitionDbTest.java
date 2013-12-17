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

import org.jbpm.db.AbstractDbTestCase;

public class TransitionDbTest extends AbstractDbTestCase {

  public void testTranisitionName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition name='t' to='n' />"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node n = processDefinition.getNode("n");
    Transition t = (Transition) n.getLeavingTransitionsList().get(0);
    assertEquals("t", t.getName());
  }

  public void testTranisitionFrom() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition name='t' to='m' />"
      + "  </node>"
      + "  <node name='m' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node n = processDefinition.getNode("n");
    Transition t = (Transition) n.getLeavingTransitionsList().get(0);
    assertSame(n, t.getFrom());
  }

  public void testTranisitionTo() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition name='t' to='m' />"
      + "  </node>"
      + "  <node name='m' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node n = processDefinition.getNode("n");
    Node m = processDefinition.getNode("m");
    Transition t = (Transition) n.getLeavingTransitionsList().get(0);
    assertSame(m, t.getTo());
  }

  public void testUnnamedTransition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition to='m' />"
      + "  </node>"
      + "  <node name='m' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node n = processDefinition.getNode("n");
    Node m = processDefinition.getNode("m");

    Transition t = n.getDefaultLeavingTransition();
    assertNotNull(t);
    assertEquals(n, t.getFrom());
    assertEquals(m, t.getTo());
    assertEquals(1, n.getLeavingTransitionsList().size());
  }

  public void testTwoUnnamedTransitions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition to='m' />"
      + "    <transition to='o' />"
      + "  </node>"
      + "  <node name='m' />"
      + "  <node name='o' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node n = processDefinition.getNode("n");
    Node m = processDefinition.getNode("m");

    Transition t = n.getDefaultLeavingTransition();
    assertNotNull(t);
    assertEquals(n, t.getFrom());
    assertEquals(m, t.getTo());
    assertEquals(2, n.getLeavingTransitionsList().size());

    assertEquals(1, n.getLeavingTransitionsMap().size());
    t = n.getLeavingTransition(null);
    assertNotNull(t);
    assertEquals(n, t.getFrom());
    assertEquals(m, t.getTo());
  }

  public void testThreeSameNameTransitions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition name='t' to='m' />"
      + "    <transition name='t' to='o' />"
      + "    <transition name='t2' to='p' />"
      + "  </node>"
      + "  <node name='m' />"
      + "  <node name='o' />"
      + "  <node name='p' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node n = processDefinition.getNode("n");
    Node m = processDefinition.getNode("m");
    Node p = processDefinition.getNode("p");

    Transition t = n.getDefaultLeavingTransition();
    assertNotNull(t);
    assertEquals("t", t.getName());
    assertEquals(n, t.getFrom());
    assertEquals(m, t.getTo());
    assertEquals(3, n.getLeavingTransitionsList().size());

    assertEquals(2, n.getLeavingTransitionsMap().size());
    t = n.getLeavingTransition("t");
    assertNotNull(t);
    assertEquals("t", t.getName());
    assertEquals(n, t.getFrom());
    assertEquals(m, t.getTo());
    t = n.getLeavingTransition("t2");
    assertNotNull(t);
    assertEquals("t2", t.getName());
    assertEquals(n, t.getFrom());
    assertEquals(p, t.getTo());
  }
}
