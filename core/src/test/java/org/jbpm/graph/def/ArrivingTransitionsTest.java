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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jbpm.AbstractJbpmTestCase;

public class ArrivingTransitionsTest extends AbstractJbpmTestCase {

  private Node n = new Node("n");
  private Transition t = new Transition("t");

  public void testAddArrivingTransition() {
    n.addArrivingTransition( t );
    assertSame( n, t.getTo() );
    assertEquals( 1, n.getArrivingTransitions().size() );
    assertEquals( t, n.getArrivingTransitions().iterator().next() );
  }

  public void testRemoveArrivingTransition() {
    n.addArrivingTransition( t );
    n.removeArrivingTransition( t );
    assertNull( t.getTo() );
    assertEquals( 0, n.getArrivingTransitions().size() );
  }

  public void testArrivingTransitions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <state name='a'>" +
      "    <transition name='to-c' to='c' />" +
      "  </state>" +
      "  <state name='b'>" +
      "    <transition name='to-c' to='c' />" +
      "  </state>" +
      "  <state name='c'/>" +
      "</process-definition>"
    );
          
    Collection arrivingTransitions = processDefinition.getNode("c").getArrivingTransitions();
    assertEquals(2, arrivingTransitions.size());
    
    Set fromNodes = new HashSet();
    Iterator iter = arrivingTransitions.iterator();
    while (iter.hasNext()) {
      Transition transition = (Transition) iter.next();
      fromNodes.add( transition.getFrom() );
      assertEquals("to-c", transition.getName());
    }
    
    Set expectedFromNodes = new HashSet();
    expectedFromNodes.add( processDefinition.getNode("a") );
    expectedFromNodes.add( processDefinition.getNode("b") );
    
    assertEquals(expectedFromNodes, fromNodes);
  }
}
