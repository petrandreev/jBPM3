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
import org.jbpm.graph.exe.Token;

public class Wfp07SynchronizingMergeTest extends AbstractJbpmTestCase {

  private static ProcessDefinition synchronizingMergeProcessDefinition = createSynchronizingMergeProcessDefinition();

  public static ProcessDefinition createSynchronizingMergeProcessDefinition() {
    ProcessDefinition pd = Wfp06MultiChoiceTest.createMultiChoiceProcessDefinition();
    return pd;
  }

  public void testSynchronizingMergeScenario1() {
    ProcessDefinition pd = synchronizingMergeProcessDefinition;

    // the token names come from the leaving transitions
    Token root = Wfp06MultiChoiceTest.executeScenario(pd, 1);
    Token tokenB = root.getChild("to b");

    tokenB.signal();
    assertSame(pd.getNode("end"), root.getNode());
    assertSame(pd.getNode("syncmerge"), tokenB.getNode());
  }

  public void testSynchronizingMergeScenario2() {
    ProcessDefinition pd = synchronizingMergeProcessDefinition;

    // the token names come from the leaving transitions
    Token root = Wfp06MultiChoiceTest.executeScenario(pd, 2);
    Token tokenC = root.getChild("to c");

    tokenC.signal();
    assertSame(pd.getNode("end"), root.getNode());
    assertSame(pd.getNode("syncmerge"), tokenC.getNode());
  }

  public void testSynchronizingMergeScenario3() {
    ProcessDefinition pd = synchronizingMergeProcessDefinition;

    // the token names come from the leaving transitions
    Token root = Wfp06MultiChoiceTest.executeScenario(pd, 3);
    Token tokenB = root.getChild("to b");
    Token tokenC = root.getChild("to c");

    tokenB.signal();
    assertSame(pd.getNode("multichoice"), root.getNode());
    assertSame(pd.getNode("syncmerge"), tokenB.getNode());
    assertSame(pd.getNode("c"), tokenC.getNode());

    tokenC.signal();
    assertSame(pd.getNode("end"), root.getNode());
    assertSame(pd.getNode("syncmerge"), tokenB.getNode());
    assertSame(pd.getNode("syncmerge"), tokenC.getNode());
  }

  public void testSynchronizingMergeScenario4() {
    ProcessDefinition pd = synchronizingMergeProcessDefinition;

    // the token names come from the leaving transitions
    Token root = Wfp06MultiChoiceTest.executeScenario(pd, 3);
    Token tokenB = root.getChild("to b");
    Token tokenC = root.getChild("to c");

    tokenC.signal();
    assertSame(pd.getNode("multichoice"), root.getNode());
    assertSame(pd.getNode("b"), tokenB.getNode());
    assertSame(pd.getNode("syncmerge"), tokenC.getNode());

    tokenB.signal();
    assertSame(pd.getNode("end"), root.getNode());
    assertSame(pd.getNode("syncmerge"), tokenB.getNode());
    assertSame(pd.getNode("syncmerge"), tokenC.getNode());
  }
}
