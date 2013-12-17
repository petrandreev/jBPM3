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

import org.jbpm.AbstractJbpmTestCase;

public class DefaultTransitionTest extends AbstractJbpmTestCase {
  
  private Node n = new Node();
  private Node n2 = new Node();
  private Node n3 = new Node();
  private Transition t = new Transition(); 
  private Transition t1 = new Transition("one"); 
  private Transition t2 = new Transition("two"); 
  private Transition t3 = new Transition("three"); 

  public void testOneTransition() {
    n.addLeavingTransition(t);
    assertSame(t, n.getDefaultLeavingTransition());
  }

  public void testUnnamedAndNamedTransition() {
    n.addLeavingTransition(t);
    n.addLeavingTransition(t1);
    assertSame(t, n.getDefaultLeavingTransition());
  }

  public void testNamedAndUnnamedTransition() {
    n.addLeavingTransition(t1);
    n.addLeavingTransition(t);
    assertSame(t1, n.getDefaultLeavingTransition());
  }

  public void test3NamedTransitions() {
    n.addLeavingTransition(t1);
    n.addLeavingTransition(t2);
    n.addLeavingTransition(t3);
    assertSame(t1, n.getDefaultLeavingTransition());
  }

  public void testAddRemoveAddScenario() {
    n.addLeavingTransition(t1);
    n.addLeavingTransition(t2);
    n.addLeavingTransition(t3);
    assertSame(t1, n.getDefaultLeavingTransition());
    n.removeLeavingTransition(t1);
    assertSame(t2, n.getDefaultLeavingTransition());
    n.removeLeavingTransition(t2);
    assertSame(t3, n.getDefaultLeavingTransition());
    n.removeLeavingTransition(t3);
    assertNull(n.getDefaultLeavingTransition());
    n.addLeavingTransition(t2);
    assertSame(t2, n.getDefaultLeavingTransition());
  }

  public void testDestinationOfDefaultTransition() {
    n.addLeavingTransition(t);
    n.removeLeavingTransition(t);
    n2.addLeavingTransition(t);
    n3.addLeavingTransition(t2);
    n3.removeLeavingTransition(t2);
    n2.addLeavingTransition(t2);
    
    assertEquals( n2, n2.getDefaultLeavingTransition().getFrom() );
  }
}
