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
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.node.State;

public class StateXmlTest extends AbstractXmlTestCase {

  public void testMultipleStates() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <state name='one' />" +
      "  <state name='two' />" +
      "  <state name='three' />" +
      "  <state name='four' />" +
      "</process-definition>" 
    );
    assertEquals(4, processDefinition.getNodes().size());
    assertEquals(State.class, processDefinition.getNode("one").getClass());
  }

  public void testState() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode( new State() );
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/state[1]" );
    assertNotNull(element);
    assertEquals("state", element.getName());
    assertEquals(0, element.attributeCount());
  }
  
  public void testStateName() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode( new State("mystate") );
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/state[1]" );
    assertNotNull(element);
    assertEquals("state", element.getName());
    assertEquals(1, element.attributeCount());
    assertEquals("mystate", element.attributeValue("name"));
  }
  
  public void testThreeStatesOrder() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.addNode( new State("one") );
    processDefinition.addNode( new State("two") );
    processDefinition.addNode( new State("three") );
    processDefinition.addNode( new State("four") );
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition );
    assertNotNull(element);
    assertEquals( "one", ((Element)element.selectSingleNode("/process-definition/state[1]")).attributeValue("name"));
    assertEquals( "two", ((Element)element.selectSingleNode("/process-definition/state[2]")).attributeValue("name"));
    assertEquals( "three", ((Element)element.selectSingleNode("/process-definition/state[3]")).attributeValue("name"));
    assertEquals( "four", ((Element)element.selectSingleNode("/process-definition/state[4]")).attributeValue("name"));
  }
}
