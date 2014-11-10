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
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_5.swf
 * 
 * <p>in jbpm every node has an implicit merge in front of it.
 * so it's not necessary to use the merge node.  in fact, i can 
 * not think of a situation where implicit merge nodeMap are not 
 * sufficient.  for the sake of workflow patterns, we leave if in.
 * jbpm supports merging of both alternative paths of execution 
 * and concurrent paths of execution.
 * </p>
 * 
 * <p>first the merge node is demonstrated exactly as in the pattern.
 * then the implicit variant is demonstrated, then the merging of 
 * concurrent paths is demonstrated.
 * </p>
 */
public class Wfp05SimpleMergeTest extends AbstractJbpmTestCase {

  private static ProcessDefinition simpleMergeProcessDefinition = createSimpleMergeProcessDefinition();
  
  public static ProcessDefinition createSimpleMergeProcessDefinition() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "state a",
                   "state b",
                   "merge xor", 
                   "state c"},
      new String[]{"start --to a--> a",
                   "start --to b--> b",
                   "a --> xor",
                   "b --> xor",
                   "xor --> c"});
    return pd;
  }

  public void testSimpleMergeScenario1() {
    ProcessDefinition pd = simpleMergeProcessDefinition;
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal("to a");
    Token root = pi.getRootToken();
    assertSame( pd.getNode("a"), root.getNode() );
    root.signal();
    assertSame( pd.getNode("c"), root.getNode() );
  }

  public void testSimpleMergeScenario2() {
    ProcessDefinition pd = simpleMergeProcessDefinition;
    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal("to b");
    Token root = pi.getRootToken();
    assertSame( pd.getNode("b"), root.getNode() );
    root.signal();
    assertSame( pd.getNode("c"), root.getNode() );
  }
  
  private static ProcessDefinition implicitMergeProcessDefinition = createImplicitMergeProcessDefinition();
  
  public static ProcessDefinition createImplicitMergeProcessDefinition() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "state a",
                   "state b",
                   "state c"},
      new String[]{"start --to a--> a",
                   "start --to b--> b",
                   "a --> c",
                   "b --> c"});
    return pd;
  }

  public void testImplicitMergeScenario1() {
    ProcessDefinition pd = implicitMergeProcessDefinition;
    ProcessInstance pi = new ProcessInstance( implicitMergeProcessDefinition );
    pi.signal("to a");
    Token root = pi.getRootToken();
    assertSame( pd.getNode("a"), root.getNode() );
    root.signal();
    assertSame( pd.getNode("c"), root.getNode() );
  }
  
  public void testImplicitMergeScenario2() {
    ProcessDefinition pd = implicitMergeProcessDefinition;
    ProcessInstance pi = new ProcessInstance( implicitMergeProcessDefinition );
    pi.signal("to b");
    Token root = pi.getRootToken();
    assertSame( pd.getNode("b"), root.getNode() );
    root.signal();
    assertSame( pd.getNode("c"), root.getNode() );
  }
  
}
