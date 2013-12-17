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
package org.jbpm.jpdl.patterns;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_3.swf
 */
public class Wfp03SynchronizationTest extends AbstractJbpmTestCase {

  public ProcessDefinition createSynchronizationProcessDefinition() {
    ProcessDefinition pd = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='fork' />" +
      "  </start-state>" +
      "  <fork name='fork'>" +
      "    <transition name='first' to='one' />" +
      "    <transition name='second' to='two' />" +
      "  </fork>" +
      "  <state name='one'>" +
      "    <transition to='join' />" +
      "  </state>" +
      "  <state name='two'>" +
      "    <transition to='join' />" +
      "  </state>" +
      "  <join name='join'>" +
      "    <transition to='end' />" +
      "  </join>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    return pd;
  }

  public void testSynchronizationFirstTokenFirst() {
    ProcessDefinition pd = createSynchronizationProcessDefinition();
          
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token root = pi.getRootToken();
    Token firstToken = root.getChild( "first" );
    Token secondToken = root.getChild( "second" );

    // check that the two tokens are in the states one and two respectively
    assertSame( pd.getNode("fork"), root.getNode() );
    assertSame( pd.getNode("one"), firstToken.getNode() );
    assertSame( pd.getNode("two"), secondToken.getNode() );

    firstToken.signal();
    assertSame( pd.getNode("fork"), root.getNode() );
    assertSame( pd.getNode("join"), firstToken.getNode() );
    assertSame( pd.getNode("two"), secondToken.getNode() );
    
    secondToken.signal();
    assertSame( pd.getNode("end"), root.getNode() );
    assertSame( pd.getNode("join"), firstToken.getNode() );
    assertSame( pd.getNode("join"), secondToken.getNode() );
  }

  /**
   * variation of the pattern where the second token fires first.
   */
  public void testSynchronizationSecondTokenFirst() {
    ProcessDefinition pd = createSynchronizationProcessDefinition();
          
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token root = pi.getRootToken();
    Token firstToken = root.getChild( "first" );
    Token secondToken = root.getChild( "second" );

    // check that the two tokens are in the states one and two respectively
    assertSame( pd.getNode("fork"), root.getNode() );
    assertSame( pd.getNode("one"), firstToken.getNode() );
    assertSame( pd.getNode("two"), secondToken.getNode() );

    secondToken.signal();
    assertSame( pd.getNode("fork"), root.getNode() );
    assertSame( pd.getNode("one"), firstToken.getNode() );
    assertSame( pd.getNode("join"), secondToken.getNode() );
    
    firstToken.signal();
    assertSame( pd.getNode("end"), root.getNode() );
    assertSame( pd.getNode("join"), firstToken.getNode() );
    assertSame( pd.getNode("join"), secondToken.getNode() );
  }

  /**
   * nested form of the synchronization pattern.
   */
  public ProcessDefinition createNestedSynchronizationProcessDefinition() {
    // tip : draw this visually if you want to understand it.
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "fork fork",
                   "fork fork1",
                   "fork fork2",
                   "state state1.1",
                   "state state1.2",
                   "state state2.1",
                   "state state2.2",
                   "join join2",
                   "join join1",
                   "join join",
                   "end-state end"}, 
      new String[]{"start --> fork",
                   "fork --first--> fork1",
                   "fork --second--> fork2",
                   "fork1 --first--> state1.1",
                   "fork1 --second--> state1.2",
                   "fork2 --first--> state2.1",
                   "fork2 --second--> state2.2",
                   "state1.1 --> join1",
                   "state1.2 --> join1",
                   "state2.1 --> join2",
                   "state2.2 --> join2",
                   "join1 --> join",
                   "join2 --> join",
                   "join --> end"});
    return pd;
  }

  public void testSynchronizationNested() {
    ProcessDefinition pd = createNestedSynchronizationProcessDefinition();
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token rootToken = pi.getRootToken();
    Token token1 = rootToken.getChild( "first" );
    Token token2 = rootToken.getChild( "second" );
    Token token11 = token1.getChild( "first" );
    Token token12 = token1.getChild( "second" );
    Token token21 = token2.getChild( "first" );
    Token token22 = token2.getChild( "second" );
    
    assertSame( pd.getNode("fork"), rootToken.getNode() );
    assertSame( pd.getNode("fork1"), token1.getNode() );
    assertSame( pd.getNode("fork2"), token2.getNode() );
    assertSame( pd.getNode("state1.1"), token11.getNode() );
    assertSame( pd.getNode("state1.2"), token12.getNode() );
    assertSame( pd.getNode("state2.1"), token21.getNode() );
    assertSame( pd.getNode("state2.2"), token22.getNode() );
    
    token11.signal();

    assertSame( pd.getNode("fork"), rootToken.getNode() );
    assertSame( pd.getNode("fork1"), token1.getNode() );
    assertSame( pd.getNode("fork2"), token2.getNode() );
    assertSame( pd.getNode("join1"), token11.getNode() );
    assertSame( pd.getNode("state1.2"), token12.getNode() );
    assertSame( pd.getNode("state2.1"), token21.getNode() );
    assertSame( pd.getNode("state2.2"), token22.getNode() );

    token12.signal();

    assertSame( pd.getNode("fork"), rootToken.getNode() );
    assertSame( pd.getNode("join"), token1.getNode() );
    assertSame( pd.getNode("fork2"), token2.getNode() );
    assertSame( pd.getNode("join1"), token11.getNode() );
    assertSame( pd.getNode("join1"), token12.getNode() );
    assertSame( pd.getNode("state2.1"), token21.getNode() );
    assertSame( pd.getNode("state2.2"), token22.getNode() );

    token21.signal();

    assertSame( pd.getNode("fork"), rootToken.getNode() );
    assertSame( pd.getNode("join"), token1.getNode() );
    assertSame( pd.getNode("fork2"), token2.getNode() );
    assertSame( pd.getNode("join1"), token11.getNode() );
    assertSame( pd.getNode("join1"), token12.getNode() );
    assertSame( pd.getNode("join2"), token21.getNode() );
    assertSame( pd.getNode("state2.2"), token22.getNode() );

    token22.signal();

    assertSame( pd.getNode("end"), rootToken.getNode() );
    assertSame( pd.getNode("join"), token1.getNode() );
    assertSame( pd.getNode("join"), token2.getNode() );
    assertSame( pd.getNode("join1"), token11.getNode() );
    assertSame( pd.getNode("join1"), token12.getNode() );
    assertSame( pd.getNode("join2"), token21.getNode() );
    assertSame( pd.getNode("join2"), token22.getNode() );
  }
}
