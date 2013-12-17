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
package org.jbpm.graph.action;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;

public class ScriptXmlTest extends AbstractJbpmTestCase {

  public void testReadScriptExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <script name='s'>" +
      "    e = m * Math.pow(c,2);" +
      "  </script>" +
      "</process-definition>"
    );
    
    Script script = (Script) processDefinition.getAction("s");
    assertTextPresent("e = m * Math.pow(c,2);", script.getExpression() ); 
  }
  
  public void assertTextPresent(String text, String string) {
    if (string.indexOf(text)==-1) {
      fail("'"+text+"' is not present in '"+string+"'");
    }
  }

  public void testReadScriptWithVariables() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <node name='a'>" +
      "    <script>" +
      "      <variable name='a' access='read' />" +
      "      <variable name='b' access='write' />" +
      "      <variable name='c' />" +
      "      <expression>e = m * Math.pow(c,2);</expression>" +
      "    </script>" +
      "  </node>" +
      "</process-definition>"
    );
    
    Node node = processDefinition.getNode("a");
    Script script = (Script) node.getAction();
    
    assertEquals( 3, script.getVariableAccesses().size() ); 
    assertTextPresent("e = m * Math.pow(c,2);", script.getExpression() ); 
  }
}
