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
import org.jbpm.graph.node.StartState;

public class StartStateXmlTest extends AbstractXmlTestCase {

  public void testParseStartState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state />" +
      "</process-definition>" 
    );
    assertNotNull(processDefinition.getStartState());
    assertSame(processDefinition.getStartState(), processDefinition.getNodes().get(0));
  }

  public void testParseStartStateName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state name='start'/>" +
      "</process-definition>" 
    );
    assertEquals("start", processDefinition.getStartState().getName());
  }

  public void testWriteStartState() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.setStartState( new StartState() );
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/start-state[1]" );
    assertNotNull(element);
    assertEquals("start-state", element.getName());
    assertEquals(0, element.attributeCount());
  }

  public void testWriteStartStateName() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    processDefinition.setStartState( new StartState("mystartstate") );
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/start-state[1]" );
    assertEquals("start-state", element.getName());
    assertEquals(1, element.attributeCount());
    assertEquals("mystartstate", element.attributeValue("name"));
  }


}
