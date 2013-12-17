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
package org.jbpm.graph.def;

import java.util.List;

import org.jbpm.AbstractJbpmTestCase;

public class SuperStateTest extends AbstractJbpmTestCase {
  
  public void testChildNodeAdditions() {
    SuperState superState = new SuperState();
    superState.addNode(new Node("one"));
    superState.addNode(new Node("two"));
    superState.addNode(new Node("three"));
    
    assertEquals(3, superState.getNodes().size());
    assertEquals(superState.getNode("one"), superState.getNodes().get(0));
    assertEquals(superState.getNode("two"), superState.getNodes().get(1));
    assertEquals(superState.getNode("three"), superState.getNodes().get(2));
  }

  public void testChildNodeRemoval() {
    SuperState superState = new SuperState();
    superState.addNode(new Node("one"));
    superState.addNode(new Node("two"));
    superState.addNode(new Node("three"));
    superState.removeNode(superState.getNode("two"));
    
    assertEquals(2, superState.getNodes().size());
    assertEquals(superState.getNode("one"), superState.getNodes().get(0));
    assertEquals(superState.getNode("three"), superState.getNodes().get(1));
  }

  public void testSuperStateXmlParsing() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <super-state name='phase one'>" +
      "    <node name='ignition' />" +
      "    <node name='explosion' />" +
      "    <node name='cleanup' />" +
      "    <node name='repare' />" +
      "  </super-state>" +
      "</process-definition>"
    );

    assertEquals(1, processDefinition.getNodes().size());
    
    SuperState phaseOne = (SuperState) processDefinition.getNode("phase one");
    assertNotNull(phaseOne);
    assertEquals(4, phaseOne.getNodes().size());
    
    assertSame(phaseOne.getNode("ignition"), phaseOne.getNodes().get(0));
    assertSame(phaseOne.getNode("explosion"), phaseOne.getNodes().get(1));
    assertSame(phaseOne.getNode("cleanup"), phaseOne.getNodes().get(2));
    assertSame(phaseOne.getNode("repare"), phaseOne.getNodes().get(3));

    // check parents
    assertSame(processDefinition, phaseOne.getParent());
    assertSame(phaseOne, phaseOne.getNode("ignition").getParent());
    assertSame(phaseOne, phaseOne.getNode("explosion").getParent());
    assertSame(phaseOne, phaseOne.getNode("cleanup").getParent());
    assertSame(phaseOne, phaseOne.getNode("repare").getParent());
    
    // check process definition references
    assertSame(processDefinition, phaseOne.getParent());
    assertSame(processDefinition, phaseOne.getNode("ignition").getProcessDefinition());
    assertSame(processDefinition, phaseOne.getNode("explosion").getProcessDefinition());
    assertSame(processDefinition, phaseOne.getNode("cleanup").getProcessDefinition());
    assertSame(processDefinition, phaseOne.getNode("repare").getProcessDefinition());
  }

  public void testNestedSuperStateXmlParsing() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <super-state name='phase one'>" +
      "    <node name='ignition' />" +
      "    <node name='explosion' />" +
      "    <super-state name='cleanup'>" +
      "      <node name='take brush' />" +
      "      <node name='sweep floor' />" +
      "    </super-state>" +
      "    <node name='repare' />" +
      "  </super-state>" +
      "</process-definition>"
    );
    
    SuperState phaseOne = (SuperState) processDefinition.getNode("phase one");
    assertNotNull(phaseOne);
    // check phase one parent
    assertSame(processDefinition, phaseOne.getParent());
    
    // check phase one child nodes
    List phaseOneNodes = phaseOne.getNodes();
    assertNotNull(phaseOneNodes);
    assertEquals(4, phaseOneNodes.size());
    assertEquals("ignition", ((Node)phaseOneNodes.get(0)).getName());
    assertEquals("explosion", ((Node)phaseOneNodes.get(1)).getName());
    assertEquals("cleanup", ((Node)phaseOneNodes.get(2)).getName());
    assertEquals("repare", ((Node)phaseOneNodes.get(3)).getName());
    // check phase one child nodes parent
    assertEquals(phaseOne, ((Node)phaseOneNodes.get(0)).getParent());
    assertEquals(phaseOne, ((Node)phaseOneNodes.get(1)).getParent());
    assertEquals(phaseOne, ((Node)phaseOneNodes.get(2)).getParent());
    assertEquals(phaseOne, ((Node)phaseOneNodes.get(3)).getParent());

    SuperState cleanUp = (SuperState) processDefinition.findNode("phase one/cleanup");
    assertSame( cleanUp, phaseOneNodes.get(2));
    // check clea up child nodes
    List cleanUpNodes = cleanUp.getNodes();
    assertNotNull(cleanUpNodes);
    assertEquals(2, cleanUpNodes.size());
    assertEquals("take brush", ((Node)cleanUpNodes.get(0)).getName());
    assertEquals("sweep floor", ((Node)cleanUpNodes.get(1)).getName());
    // check clean up child nodes parent
    assertEquals(cleanUp, ((Node)cleanUpNodes.get(0)).getParent());
    assertEquals(cleanUp, ((Node)cleanUpNodes.get(1)).getParent());

    assertEquals("take brush", processDefinition.findNode("phase one/cleanup/take brush").getName());
  }

  public void testNestedSuperStateXmlTransitionParsing() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <node name='preparation'>" +
      "    <transition name='local' to='phase one' />" +
      "    <transition name='superstate-node' to='phase one/cleanup' />" +
      "    <transition name='nested-superstate-node' to='phase one/cleanup/take brush' />" +
      "  </node>" +
      "  <super-state name='phase one'>" +
      "    <node name='ignition'>" +
      "      <transition name='parent' to='../preparation' />" +
      "      <transition name='local' to='explosion' />" +
      "      <transition name='superstate-node' to='cleanup/take brush' />" +
      "    </node>" +
      "    <node name='explosion' />" +
      "    <super-state name='cleanup'>" +
      "      <node name='take brush'>" +
      "        <transition name='recursive-parent' to='../../preparation' />" +
      "        <transition name='parent' to='../explosion' />" +
      "        <transition name='local' to='take brush' />" +
      "        <transition name='absolute-superstate' to='/phase one' />" +
      "        <transition name='absolute-node' to='/phase two' />" +
      "      </node>" +
      "      <node name='sweep floor' />" +
      "    </super-state>" +
      "    <node name='repare' />" +
      "  </super-state>" +
      "  <node name='phase two' />" +
      "</process-definition>"
    );

    Node preparation = processDefinition.getNode("preparation");
    assertNotNull(preparation);
    assertEquals("phase one", preparation.getLeavingTransition("local").getTo().getName() );
    assertEquals("cleanup", preparation.getLeavingTransition("superstate-node").getTo().getName() );
    assertEquals("take brush", preparation.getLeavingTransition("nested-superstate-node").getTo().getName() );

    Node ignition = processDefinition.findNode("phase one/ignition");
    assertNotNull(ignition);
    assertEquals("preparation", ignition.getLeavingTransition("parent").getTo().getName() );
    assertEquals("explosion", ignition.getLeavingTransition("local").getTo().getName() );
    assertEquals("take brush", ignition.getLeavingTransition("superstate-node").getTo().getName() );

    Node cleanup = processDefinition.findNode("phase one/cleanup/take brush");
    assertNotNull(ignition);
    assertEquals("preparation", cleanup.getLeavingTransition("recursive-parent").getTo().getName() );
    assertEquals("explosion", cleanup.getLeavingTransition("parent").getTo().getName() );
    assertEquals("take brush", cleanup.getLeavingTransition("local").getTo().getName() );
    assertEquals("phase one", cleanup.getLeavingTransition("absolute-superstate").getTo().getName() );
    assertEquals("phase two", cleanup.getLeavingTransition("absolute-node").getTo().getName() );
  }

  public void testSuperStateTransitionsParsing() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <node name='preparation'>" +
      "    <transition to='phase one' />" +
      "  </node>" +
      "  <super-state name='phase one'>" +
      "    <transition name='to-node' to='preparation' />" +
      "    <transition name='self' to='phase one' />" +
      "  </super-state>" +
      "</process-definition>"
    );
    
    assertEquals("preparation", processDefinition.getNode("phase one").getLeavingTransition("to-node").getTo().getName() );
    assertEquals("phase one", processDefinition.getNode("phase one").getLeavingTransition("self").getTo().getName() );
    assertEquals("phase one", processDefinition.getNode("preparation").getDefaultLeavingTransition().getTo().getName() );
  }

  public void testLeavingTransitionOfSuperState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <super-state name='super'>" +
      "    <node name='child' />" +
      "    <transition name='take me' to='super' />" +
      "  </super-state>" +
      "</process-definition>"
    );
    
    Node child = processDefinition.findNode("super/child");
    Transition takeMe = processDefinition.getNode("super").getLeavingTransition("take me");
    
    assertSame(takeMe, child.getLeavingTransition("take me") );
  }
}
