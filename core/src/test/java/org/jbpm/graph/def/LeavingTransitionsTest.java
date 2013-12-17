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

import java.util.Iterator;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;

public class LeavingTransitionsTest extends AbstractJbpmTestCase {
  
  private Node node = new Node("n");
  private Transition transition = new Transition("t");

  public void testAddLeavingTransitionWithoutName() {
    Transition transitionWithoutName = new Transition();
    node.addLeavingTransition( transitionWithoutName );
    assertEquals( 1, node.getLeavingTransitionsMap().size() );
    assertEquals( 1, node.getLeavingTransitionsMap().size() );
    assertSame( transitionWithoutName, node.getLeavingTransition(null) );
    assertSame( transitionWithoutName, node.getLeavingTransitionsList().get(0) );
    assertSame( node, transitionWithoutName.getFrom() );
  }

  public void testAddLeavingTransition() {
    node.addLeavingTransition( transition );
    assertEquals( 1, node.getLeavingTransitionsMap().size() );
    assertEquals( 1, node.getLeavingTransitionsMap().size() );
    assertSame( transition, node.getLeavingTransition("t") );
    assertSame( transition, node.getLeavingTransitionsList().get(0) );
    assertSame( node, transition.getFrom() );
  }

  public void testRename() {
    node.addLeavingTransition( transition );
    transition.setName("t2");
    assertSame(transition, node.getLeavingTransition("t2"));
  }

  public void testRemoveLeavingTransition() {
    node.addLeavingTransition( transition );
    node.removeLeavingTransition( transition );
    assertNull( node.getLeavingTransition("t") );
    assertNull( transition.getFrom() );
    assertEquals( 0, node.getLeavingTransitionsMap().size() );
  }

  public void testOverwriteLeavingTransitionAllowed() {
    node.addLeavingTransition( transition );
    node.addLeavingTransition( new Transition() );
    assertEquals(2, node.getLeavingTransitionsMap().size());
    
    Iterator iter = node.getLeavingTransitionsList().iterator();
    while (iter.hasNext()) {
      assertSame( node, ((Transition)iter.next()).getFrom() );
    }
  }

  public void testLeavingTransitionsXmlTest() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <state name='a'>" +
      "    <transition to='b' />" +
      "    <transition name='to-c' to='c' />" +
      "    <transition name='to-d' to='d' />" +
      "  </state>" +
      "  <state name='b' />" +
      "  <state name='c'/>" +
      "  <state name='d'/>" +
      "</process-definition>"
    );
    
    List leavingTransitions = processDefinition.getNode("a").getLeavingTransitionsList();
    assertEquals(3, leavingTransitions.size());
    assertNull( ((Transition)leavingTransitions.get(0)).getName());
  }
}
