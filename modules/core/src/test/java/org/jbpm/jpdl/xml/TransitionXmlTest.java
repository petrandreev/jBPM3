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

import org.dom4j.Element;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;

public class TransitionXmlTest extends AbstractXmlTestCase {

  public void testReadNodeTransition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <node name='a'>" +
      "    <transition to='b' />" +
      "  </node>" +
      "  <node name='b' />" +
      "</process-definition>"
    );
    Node a = processDefinition.getNode("a");
    Node b = processDefinition.getNode("b");
    assertSame(a, a.getDefaultLeavingTransition().getFrom());
    assertSame(b, a.getDefaultLeavingTransition().getTo());
  }

  public void testWriteNodeTransition() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node a = new Node("a");
    Node b = new Node("b");
    processDefinition.addNode(a);
    processDefinition.addNode(b);

    Transition t = new Transition();
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);
    
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/node[1]/transition" );
    assertNotNull(element);
    assertEquals("transition", element.getName());
    assertEquals(1, element.attributeCount());
    assertEquals("b", element.attributeValue("to"));
  }

  public void testReadNodeTransitionName() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <node name='a'>" +
      "    <transition name='hertransition' to='b' />" +
      "  </node>" +
      "  <node name='b' />" +
      "</process-definition>"
    );
            
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/node[1]/transition[1]" );
    assertNotNull(element);
    assertEquals("hertransition", element.attributeValue("name"));
  }

  public void testWriteNodeTransitionName() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node a = new Node("a");
    Node b = new Node("b");
    processDefinition.addNode(a);
    processDefinition.addNode(b);

    Transition t = new Transition("hertransition");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/node[1]/transition" );
    assertNotNull(element);
    assertEquals("hertransition", element.attributeValue("name"));
  }

  public void testTransitionOrder() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node a = new Node("a");
    Node b = new Node("b");
    processDefinition.addNode(a);
    processDefinition.addNode(b);

    Transition t = new Transition("one");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    t = new Transition("two");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    t = new Transition("three");
    a.addLeavingTransition(t);
    b.addArrivingTransition(t);

    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition );
    assertNotNull(element);
    assertEquals( "one", ((Element)element.selectSingleNode("/process-definition/node[1]/transition[1]")).attributeValue("name"));
    assertEquals( "two", ((Element)element.selectSingleNode("/process-definition/node[1]/transition[2]")).attributeValue("name"));
    assertEquals( "three", ((Element)element.selectSingleNode("/process-definition/node[1]/transition[3]")).attributeValue("name"));
  }
  
}
