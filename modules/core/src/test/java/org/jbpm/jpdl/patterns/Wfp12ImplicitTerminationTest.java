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
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_11.swf
 * 
 * Must the implicit termination be based on states only or also on tasks ?
 */
public class Wfp12ImplicitTerminationTest extends AbstractJbpmTestCase {
  
  private static ProcessDefinition implicitTerminationProcessDefinition = createImplicitTerminationDefinition();

  public static ProcessDefinition createImplicitTerminationDefinition() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "fork fork",
                   "state b",
                   "state c",
                   "state d",
                   "state e"}, 
      new String[]{"start --> fork",
                   "fork --b--> b",
                   "fork --c--> c",
                   "b --> d",
                   "c --> e"});
    pd.setTerminationImplicit( true );
    return pd;
  }

  public void testImplicitTerminationScenario1() {
    ProcessDefinition pd = implicitTerminationProcessDefinition;
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token rootToken = pi.getRootToken();
    Token tokenB = rootToken.getChild("b");
    Token tokenC = rootToken.getChild("c");

    assertFalse( pi.hasEnded() );
    tokenB.signal();
    assertFalse( pi.hasEnded() );
    tokenC.signal();
    assertTrue( pi.hasEnded() );
  }

  public void testImplicitTerminationScenario2() {
    ProcessDefinition pd = implicitTerminationProcessDefinition;
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token rootToken = pi.getRootToken();
    Token tokenB = rootToken.getChild("b");
    Token tokenC = rootToken.getChild("c");

    assertFalse( pi.hasEnded() );
    tokenC.signal();
    assertFalse( pi.hasEnded() );
    tokenB.signal();
    assertTrue( pi.hasEnded() );
  }
}
