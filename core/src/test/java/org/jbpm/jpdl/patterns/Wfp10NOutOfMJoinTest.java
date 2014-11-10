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
import org.jbpm.graph.node.Join;

public class Wfp10NOutOfMJoinTest extends AbstractJbpmTestCase {

  private static ProcessDefinition nOutOfMProcessDefinition = createNOutOfMProcessDefinition();

  public static ProcessDefinition createNOutOfMProcessDefinition() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "state a",
                   "fork fork",
                   "state b",
                   "state c",
                   "state d",
                   "join noutofm",
                   "state e"}, 
      new String[]{"start --> a",
                   "a --> fork",
                   "fork --b--> b",
                   "fork --c--> c",
                   "fork --d--> d",
                   "b --> noutofm",
                   "c --> noutofm",
                   "d --> noutofm",
                   "noutofm --> e"});
    
    // put the script in the multichoice handler
    Join nOutOfMJoin = (Join) pd.getNode("noutofm");
    nOutOfMJoin.setNOutOfM( 2 );

    return pd;
  }


  public void testNOutOfMScenario1() {
    ProcessDefinition pd = nOutOfMProcessDefinition;
    Token root = startNOutOfMScenario(pd);
    Token tokenB = root.getChild("b");
    Token tokenC = root.getChild("c");
    Token tokenD = root.getChild("d");
    
    tokenB.signal();
    assertSame( pd.getNode("fork"), root.getNode() );
    assertSame( pd.getNode("noutofm"), tokenB.getNode() );
    assertSame( pd.getNode("c"), tokenC.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );

    tokenC.signal();
    assertSame( pd.getNode("e"), root.getNode() );
    assertSame( pd.getNode("noutofm"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );

    tokenD.signal();
    assertSame( pd.getNode("e"), root.getNode() );
    assertSame( pd.getNode("noutofm"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("noutofm"), tokenD.getNode() );
  }
  
  public void testNOutOfMScenario2() {
    ProcessDefinition pd = nOutOfMProcessDefinition;
    Token root = startNOutOfMScenario(pd);
    Token tokenB = root.getChild("b");
    Token tokenC = root.getChild("c");
    Token tokenD = root.getChild("d");
    
    tokenC.signal();
    assertSame( pd.getNode("fork"), root.getNode() );
    assertSame( pd.getNode("b"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );

    tokenB.signal();
    assertSame( pd.getNode("e"), root.getNode() );
    assertSame( pd.getNode("noutofm"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );

    tokenD.signal();
    assertSame( pd.getNode("e"), root.getNode() );
    assertSame( pd.getNode("noutofm"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("noutofm"), tokenD.getNode() );
  }

  public void testNOutOfMScenario3() {
    ProcessDefinition pd = nOutOfMProcessDefinition;
    Token root = startNOutOfMScenario(pd);
    Token tokenB = root.getChild("b");
    Token tokenC = root.getChild("c");
    Token tokenD = root.getChild("d");
    
    tokenC.signal();
    assertSame( pd.getNode("fork"), root.getNode() );
    assertSame( pd.getNode("b"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("d"), tokenD.getNode() );

    tokenD.signal();
    assertSame( pd.getNode("e"), root.getNode() );
    assertSame( pd.getNode("b"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("noutofm"), tokenD.getNode() );

    tokenB.signal();
    assertSame( pd.getNode("e"), root.getNode() );
    assertSame( pd.getNode("noutofm"), tokenB.getNode() );
    assertSame( pd.getNode("noutofm"), tokenC.getNode() );
    assertSame( pd.getNode("noutofm"), tokenD.getNode() );
  }
  
  public static Token startNOutOfMScenario(ProcessDefinition pd) {
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token root = pi.getRootToken();
    root.signal();
    return root;
  }
}
