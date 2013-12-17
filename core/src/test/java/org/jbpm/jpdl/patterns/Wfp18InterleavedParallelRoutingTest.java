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

import java.util.Collection;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.InterleaveStart;
import org.jbpm.graph.node.State;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_17.swf
 */
public class Wfp18InterleavedParallelRoutingTest extends AbstractJbpmTestCase {
  
  int scenario = -1;
  private ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
    "<process-definition>" +
    "  <start-state name='a'>" +
    "    <transition to='startinterleaving' />" +
    "  </start-state>" +
    "  <interleave-start name='startinterleaving'>" +
    "    <transition name='b' to='b' />" +
    "    <transition name='c' to='c' />" +
    "    <transition name='d' to='d' />" +
    "  </interleave-start>" +
    "  <state name='b'>" +
    "    <transition to='endinterleaving' />" +
    "  </state>" +
    "  <state name='c'>" +
    "    <transition to='endinterleaving' />" +
    "  </state>" +
    "  <state name='d'>" +
    "    <transition to='endinterleaving' />" +
    "  </state>" +
    "  <interleave-end name='endinterleaving'>" +
    "    <transition name='back' to='startinterleaving' />" +
    "    <transition name='done' to='e' />" +
    "  </interleave-end>" +
    "  <state name='e' />" +
    "</process-definition>"
  );
  
  private InterleaveStart interleaveStart = (InterleaveStart) processDefinition.getNode("startinterleaving");
  private State b = (State) processDefinition.getNode("b");
  private State c = (State) processDefinition.getNode("c");
  private State d = (State) processDefinition.getNode("d");
  private State e = (State) processDefinition.getNode("e");
  
  static String[][] scenarioSequences = new String[][] {
    new String[] {"b", "c", "d"},
    new String[] {"b", "d", "c"},
    new String[] {"c", "b", "d"},
    new String[] {"c", "d", "b"},
    new String[] {"d", "b", "c"},
    new String[] {"d", "c", "b"}
  };
  
  public class ScenarioInterleaver implements InterleaveStart.Interleaver {
    public String selectNextTransition(Collection transitionNames) {
      // this piece of code is executed at runtime when a decision 
      // needs to be made in the start-interleaving-node about which
      // transition to take
      return scenarioSequences[scenario-1][3-transitionNames.size()];
    }
  }

  protected void setUp() {
    interleaveStart.setInterleaver(new ScenarioInterleaver());
  }
  
  public void testInterleavedParallelRoutingScenario1() {
    scenario = 1;
    
    ProcessInstance processInstance = new ProcessInstance( processDefinition );
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    assertSame( b, token.getNode() );
    token.signal();
    assertSame( c, token.getNode() );
    token.signal();
    assertSame( d, token.getNode() );
    token.signal();
    assertSame( e, token.getNode() );
  }

  public void testInterleavedParallelRoutingScenario2() {
    scenario = 2;
    
    ProcessInstance processInstance = new ProcessInstance( processDefinition );
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    assertSame( b, token.getNode() );
    token.signal();
    assertSame( d, token.getNode() );
    token.signal();
    assertSame( c, token.getNode() );
    token.signal();
    assertSame( e, token.getNode() );
  }

  public void testInterleavedParallelRoutingScenario3() {
    scenario = 3;
    
    ProcessInstance processInstance = new ProcessInstance( processDefinition );
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    assertSame( c, token.getNode() );
    token.signal();
    assertSame( b, token.getNode() );
    token.signal();
    assertSame( d, token.getNode() );
    token.signal();
    assertSame( e, token.getNode() );
  }

  public void testInterleavedParallelRoutingScenario4() {
    scenario = 4;
    
    ProcessInstance processInstance = new ProcessInstance( processDefinition );
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    assertSame( c, token.getNode() );
    token.signal();
    assertSame( d, token.getNode() );
    token.signal();
    assertSame( b, token.getNode() );
    token.signal();
    assertSame( e, token.getNode() );
  }

  public void testInterleavedParallelRoutingScenario5() {
    scenario = 5;
    
    ProcessInstance processInstance = new ProcessInstance( processDefinition );
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    assertSame( d, token.getNode() );
    token.signal();
    assertSame( b, token.getNode() );
    token.signal();
    assertSame( c, token.getNode() );
    token.signal();
    assertSame( e, token.getNode() );
  }

  public void testInterleavedParallelRoutingScenario6() {
    scenario = 6;
    
    ProcessInstance processInstance = new ProcessInstance( processDefinition );
    Token token = processInstance.getRootToken();
    processInstance.signal();
    
    assertSame( d, token.getNode() );
    token.signal();
    assertSame( c, token.getNode() );
    token.signal();
    assertSame( b, token.getNode() );
    token.signal();
    assertSame( e, token.getNode() );
  }
}
