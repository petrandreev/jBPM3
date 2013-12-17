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
package org.jbpm.tdd;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.State;

public class AuctionXmlTest extends AbstractJbpmTestCase {

  // parse the process definition
  static ProcessDefinition auctionProcess = 
      ProcessDefinition.parseXmlResource("org/jbpm/tdd/auction.xml");

  // get the nodes for easy asserting
  static StartState start = (StartState) auctionProcess.getStartState();
  static State auction = (State) auctionProcess.getNode("auction");
  static EndState end = (EndState) auctionProcess.getNode("end");

  // the process instance
  ProcessInstance processInstance;

  // the main path of execution
  Token token;

  protected void setUp() {
    // create a new process instance for the given process definition
    processInstance = new ProcessInstance(auctionProcess);

    // the main path of execution is the root token
    token = processInstance.getRootToken();
  }

  public void testMainScenario() {
    // after process instance creation, the main path of 
    // execution is positioned in the start state.
    assertSame(start, token.getNode());
    
    token.signal();
    
    // after the signal, the main path of execution has 
    // moved to the auction state
    assertSame(auction, token.getNode());
    
    token.signal();
    
    // after the signal, the main path of execution has 
    // moved to the end state and the process has ended
    assertSame(end, token.getNode());
    assertTrue(processInstance.hasEnded());
  }
}
