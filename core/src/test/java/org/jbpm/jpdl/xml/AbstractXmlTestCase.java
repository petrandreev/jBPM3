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

import java.io.StringWriter;
import java.util.Collections;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;

abstract class AbstractXmlTestCase extends AbstractJbpmTestCase {

  protected static Element toXmlAndParse(ProcessDefinition processDefinition,
    String xpathExpression) throws Exception {
    Element element = toXmlAndParse(processDefinition);
    return (Element) element.selectSingleNode(xpathExpression);
  }

  protected static Element toXmlAndParse(ProcessDefinition processDefinition,
    String xpathExpression, String namespace) throws Exception {
    Element element = toXmlAndParseWithNamespace(processDefinition);
    XPath xpath = DocumentHelper.createXPath(xpathExpression);
    xpath.setNamespaceURIs(Collections.singletonMap("", namespace));
    return (Element) xpath.selectSingleNode(element);
  }

  protected static Element toXmlAndParse(ProcessDefinition processDefinition) throws Exception {
    String xml = JpdlXmlWriter.toString(processDefinition);
    return DocumentHelper.parseText(xml).getRootElement();
  }

  protected static Element toXmlAndParseWithNamespace(ProcessDefinition processDefinition)
    throws Exception {
    StringWriter stringWriter = new StringWriter();
    JpdlXmlWriter jpdlWriter = new JpdlXmlWriter(stringWriter);
    jpdlWriter.setUseNamespace(true);
    jpdlWriter.write(processDefinition);
    String xml = stringWriter.toString();
    return DocumentHelper.parseText(xml).getRootElement();
  }
}
