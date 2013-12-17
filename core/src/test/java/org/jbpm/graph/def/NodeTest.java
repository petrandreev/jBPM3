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

import org.jbpm.AbstractJbpmTestCase;

public class NodeTest extends AbstractJbpmTestCase {
  
  public void testNameChange() {
    Node node = new Node();
    assertNull(node.getName());
    node.setName("jos");
    assertEquals("jos", node.getName());
    node.setName("piet");
    assertEquals("piet", node.getName());
  }

  public void testNameChangeInProcessDefinition() {
    Node node = new Node();
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);
    
    assertSame(node, processDefinition.getNode(null));
    node.setName("jos");
    assertNull(processDefinition.getNode(null));
    assertSame(node, processDefinition.getNode("jos"));
    assertEquals("jos", node.getName());
    node.setName("piet");
    assertNull(processDefinition.getNode(null));
    assertNull(processDefinition.getNode("jos"));
    assertSame(node, processDefinition.getNode("piet"));
  }

}
