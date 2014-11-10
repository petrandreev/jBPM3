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
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_16.swf
 */
public class Wfp17DeferredChoiceTest extends AbstractJbpmTestCase {

  public static ProcessDefinition deferredChoiceProcessDefinition = createDeferredChoiceProcessDefinition();
  
  public static ProcessDefinition createDeferredChoiceProcessDefinition() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "state a",
                   "state b",
                   "state c"}, 
      new String[]{"start --> a",
                   "a --to a--> b",
                   "a --to b--> c"});
    return pd;
  }

  public void testDeferredChoiceScenario1() {
    ProcessDefinition pd = deferredChoiceProcessDefinition;

    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token root = pi.getRootToken();
    assertSame( pd.getNode("a"), root.getNode() );
    root.signal("to a"); // take the transition with name 'to a'
    assertSame( pd.getNode("b"), root.getNode() );
  }

  public void testDeferredChoiceScenario2() {
    ProcessDefinition pd = deferredChoiceProcessDefinition;

    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token root = pi.getRootToken();
    assertSame( pd.getNode("a"), root.getNode() );
    root.signal("to b"); // take the transition with name 'to b'
    assertSame( pd.getNode("c"), root.getNode() );
  }

}
