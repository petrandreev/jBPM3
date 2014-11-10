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

public class Wfp20CancelActivityTest extends AbstractJbpmTestCase {
  
  public static ProcessDefinition cancelProcessDefinition = createCancelProcessDefinition();
  
  public static ProcessDefinition createCancelProcessDefinition() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "fork f1",
                   "state a",
                   "fork f2",
                   "state b",
                   "state c"}, 
      new String[]{"start --> f1",
                   "f1 --a--> a",
                   "f1 --f2--> f2",
                   "f2 --b--> b",
                   "f2 --c--> c"});
    
    return pd;
  }
  
  public void testCancelActivityScenario1() {
    ProcessDefinition pd = cancelProcessDefinition;
    ProcessInstance pi = new ProcessInstance(pd);
    pi.signal();
    Token root = pi.getRootToken();
    Token tokenA = root.getChild("a");
    Token tokenF2 = root.getChild("f2");
    Token tokenF2B = tokenF2.getChild("b");
    Token tokenF2C = tokenF2.getChild("c");
    
    assertFalse( pi.hasEnded() );
    assertFalse( root.hasEnded() );
    assertFalse( tokenA.hasEnded() );
    assertFalse( tokenF2.hasEnded() );
    assertFalse( tokenF2B.hasEnded() );
    assertFalse( tokenF2C.hasEnded() );
    
    tokenF2B.end();

    assertFalse( pi.hasEnded() );
    assertFalse( root.hasEnded() );
    assertFalse( tokenA.hasEnded() );
    assertFalse( tokenF2.hasEnded() );
    assertTrue( tokenF2B.hasEnded() );
    assertFalse( tokenF2C.hasEnded() );

    tokenF2C.end();

    assertFalse( pi.hasEnded() );
    assertFalse( root.hasEnded() );
    assertFalse( tokenA.hasEnded() );
    assertTrue( tokenF2.hasEnded() );
    assertTrue( tokenF2B.hasEnded() );
    assertTrue( tokenF2C.hasEnded() );

    tokenA.end();

    assertTrue( pi.hasEnded() );
    assertTrue( root.hasEnded() );
    assertTrue( tokenA.hasEnded() );
    assertTrue( tokenF2.hasEnded() );
    assertTrue( tokenF2B.hasEnded() );
    assertTrue( tokenF2C.hasEnded() );
  }

}
