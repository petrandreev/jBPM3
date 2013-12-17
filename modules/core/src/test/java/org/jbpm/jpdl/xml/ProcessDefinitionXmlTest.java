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
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.StartState;

public class ProcessDefinitionXmlTest extends AbstractXmlTestCase {

  public void testParseProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition />" 
    );
    assertNotNull(processDefinition);
  }

  public void testParseProcessDefinitionNonUTFEncoding() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource(
        "org/jbpm/jpdl/xml/encodedprocess.xml");
    assertEquals("jbpm en espa\u00f1ol", processDefinition.getName());
    List nodes = processDefinition.getNodes();
    StartState startState = (StartState) nodes.get(0);
    assertEquals("introducci\u00f3n", startState.getName());
    EndState endState = (EndState) nodes.get(2);
    assertEquals("conclusi\u00f3n", endState.getName());
  }

  public void testParseProcessDefinitionName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition name='make coffee' />" 
    );
    assertEquals("make coffee", processDefinition.getName());
  }

  public void testWriteProcessDefinition() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Element element = toXmlAndParse( processDefinition );
    assertNotNull(element);
    assertEquals("process-definition", element.getName());
    assertEquals(0, element.attributeCount());
  }

  public void testWriteProcessDefinitionName() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition( "myprocess" );
    Element element = toXmlAndParse( processDefinition );
    assertNotNull(element);
    assertEquals("process-definition", element.getName());
    assertEquals("myprocess", element.attributeValue("name"));
    assertEquals(1, element.attributeCount());
  }

}
