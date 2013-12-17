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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;

public class XmlSchemaTest extends AbstractJbpmTestCase {
  
  /**
   * parses the xml file in the subdir 'files' that corresponds
   * with the test method name.
   */
  private ProcessDefinition parseXmlForThisMethod() {
    String resource = "org/jbpm/jpdl/xml/"+getName()+".xml";
    return ProcessDefinition.parseXmlResource(resource);
  }

  public void testInvalidXml() {
    try {
      parseXmlForThisMethod();
      fail("expected exception");
    } catch (JpdlException e) {
      // OK
    }
  }

  public void testNoSchemaReference() {
    // without a reference to the schema, the process definition is 
    // not validated and parsing succeeds
    parseXmlForThisMethod();
  }

  public void testSimpleSchemaReference() {
    try {
      parseXmlForThisMethod();
      fail("expected exception");
    } catch (JpdlException e) {
      // OK
    }
  }

  public void testProcessDefinitionWithSchemaLocation() {
    try {
      parseXmlForThisMethod();
      fail("expected exception");
    } catch (JpdlException e) {
      // OK
    }
  }

  public void testMultipleNamespaces() {
    JpdlParser.addSchemaResource("org/jbpm/jpdl/xml/sitemap.xsd");
    try {
      parseXmlForThisMethod();
      fail("expected exception");
    } catch (JpdlException e) {
      // OK
    }
  }

  public void testInvalidProcessDefinitionAttribute() {
    try {
      parseXmlForThisMethod();
      fail("expected exception");
    } catch (JpdlException e) {
      // OK
    }
  }

  public void testInvalidProcessDefinitionContent() {
    try {
      parseXmlForThisMethod();
      fail("expected exception");
    } catch (JpdlException e) {
      // OK
    }
  }

  public void testTwoStartStates() {
    try {
      parseXmlForThisMethod();
      fail("expected exception");
    } catch (JpdlException e) {
      // OK
    }
  }

  public void testAction() {parseXmlForThisMethod();}
  public void testDecision() {parseXmlForThisMethod();}
  public void testEvent() {parseXmlForThisMethod();}
  public void testStartState() {parseXmlForThisMethod();}
  public void testTask() {parseXmlForThisMethod();}
  public void testExceptionHandler() {parseXmlForThisMethod();}
  public void testEndState() {parseXmlForThisMethod();}
  public void testScript() {parseXmlForThisMethod();}
}
