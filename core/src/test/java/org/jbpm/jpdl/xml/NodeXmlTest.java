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
package org.jbpm.jpdl.xml;

import java.util.List;

import org.dom4j.Element;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;

public class NodeXmlTest extends AbstractXmlTestCase {

  public void testReadNode() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node />"
      + "</process-definition>");
    assertNotNull(processDefinition.getNodes().get(0));
  }

  public void testWriteNode() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(new Node());

    Element element = toXmlAndParse(processDefinition, "/process-definition/node");
    assertNotNull(element);
    assertEquals("node", element.getName());
    assertEquals(0, element.attributeCount());
  }

  public void testReadNodeName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='wash car' />"
      + "</process-definition>");
    assertEquals("wash car", processDefinition.getNode("wash car").getName());
  }

  public void testWriteNodeName() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(new Node("n"));

    Element element = toXmlAndParse(processDefinition, "/process-definition/node");
    assertEquals("n", element.attributeValue("name"));
    assertEquals(1, element.attributeCount());
  }

  public void testReadNodeEvents() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <event type='node-enter'/>"
      + "    <event type='customeventtype' />"
      + "  </node>"
      + "</process-definition>");

    Node node = processDefinition.getNode("n");
    assertEquals(2, node.getEvents().size());
    assertEquals("node-enter", node.getEvent("node-enter").getEventType());
    assertEquals("customeventtype", node.getEvent("customeventtype").getEventType());
  }

  public void testWriteNodeEvents() throws Exception {
    Node node = new Node("n");
    node.addEvent(new Event("one"));
    node.addEvent(new Event("two"));
    node.addEvent(new Event("three"));

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);

    Element element = toXmlAndParse(processDefinition, "/process-definition/node");
    assertNotNull(element);
    assertEquals(3, element.elements("event").size());
  }

  public void testReadNodeTransitions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='n'>"
      + "    <transition name='one' to='n'/>"
      + "    <transition name='two' to='n'/>"
      + "    <transition name='three' to='n'/>"
      + "  </node>"
      + "</process-definition>");

    Node node = processDefinition.getNode("n");
    List leavingTransitionsList = node.getLeavingTransitionsList();
    assertEquals(3, leavingTransitionsList.size());
    assertEquals("one", ((Transition) leavingTransitionsList.get(0)).getName());
    assertEquals("two", ((Transition) leavingTransitionsList.get(1)).getName());
    assertEquals("three", ((Transition) leavingTransitionsList.get(2)).getName());
  }

  public void testWriteNodeTransitions() throws Exception {
    Node node = new Node("n");
    node.addLeavingTransition(new Transition("one"));
    node.addLeavingTransition(new Transition("two"));
    node.addLeavingTransition(new Transition("three"));

    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode(node);

    Element element = toXmlAndParse(processDefinition, "/process-definition/node");
    assertEquals(3, element.elements("transition").size());
    assertEquals("one", ((Element) element.elements("transition").get(0)).attributeValue("name"));
    assertEquals("two", ((Element) element.elements("transition").get(1)).attributeValue("name"));
    assertEquals("three", ((Element) element.elements("transition").get(2)).attributeValue("name"));
  }
}
