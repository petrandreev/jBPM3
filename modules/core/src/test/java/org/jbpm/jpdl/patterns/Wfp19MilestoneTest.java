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
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.MilestoneEvent;
import org.jbpm.instantiation.Delegation;

public class Wfp19MilestoneTest extends AbstractJbpmTestCase {

  public static ProcessDefinition milestoneProcessDefinition = createMilestoneProcessDefinition();
  
  public static ProcessDefinition createMilestoneProcessDefinition() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "fork fork",
                   "state b",
                   "milestone-node m",
                   "state c",
                   "state d",
                   "join join",
                   "end-state end"}, 
      new String[]{"start --> fork",
                   "fork --m--> b",
                   "fork --d--> d",
                   "b --> m",
                   "m --> c",
                   "c --> join",
                   "d --> join",
                   "join --> end"});
    
    Node d = pd.getNode("d");
    
    Delegation instantiatableDelegate = new Delegation(new MilestoneEvent("m", "../m"));
    Event event = new Event(Event.EVENTTYPE_NODE_LEAVE);
    d.addEvent(event);
    event.addAction(new Action(instantiatableDelegate));
    
    pd.addDefinition(new ContextDefinition());

    return pd;
  }
  
  public void testMilestoneScenario1() {
    ProcessDefinition pd = milestoneProcessDefinition;
    Token root = startScenario();
    Token tokenM = root.getChild("m");
    Token tokenD = root.getChild("d");
    assertSame( pd.getNode("b"), tokenM.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );
    tokenM.signal();
    assertSame( pd.getNode("m"), tokenM.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );
    tokenD.signal();
    assertSame( pd.getNode("c"), tokenM.getNode() );
    assertSame( pd.getNode("join"), tokenD.getNode() );
  }

  public void testMilestoneScenario2() {
    ProcessDefinition pd = milestoneProcessDefinition;
    Token root = startScenario();
    Token tokenM = root.getChild("m");
    Token tokenD = root.getChild("d");
    assertSame( pd.getNode("b"), tokenM.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );
    tokenD.signal();
    assertSame( pd.getNode("b"), tokenM.getNode() );
    assertSame( pd.getNode("join"), tokenD.getNode() );
    tokenM.signal();
    assertSame( pd.getNode("c"), tokenM.getNode() );
    assertSame( pd.getNode("join"), tokenD.getNode() );
  }

  public Token startScenario() {
    ProcessDefinition pd = milestoneProcessDefinition;
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    return pi.getRootToken();
  }
}
