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
package org.jbpm.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class XmlUtil {

  private static final DocumentBuilderFactory documentBuilderFactory = createDocumentBuilderFactory();

  private static DocumentBuilderFactory createDocumentBuilderFactory() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setCoalescing(true);
    factory.setIgnoringComments(true);
    factory.setExpandEntityReferences(false);
    return factory;
  }

  private XmlUtil() {
    // hide default constructor to prevent instantiation
  }

  public static Document parseXmlText(String xml) {
    ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    return parseXmlInputSource(new InputSource(bais));
  }

  /**
   * @param useConfiguredLoader if <code>true</code>, this method searches for the resource in
   * the context class loader, if not found it falls back on the class loader of this class
   * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1148">JBPM-1148</a>
   */
  public static Document parseXmlResource(String resource, boolean useConfiguredLoader) {
    InputStream inputStream = ClassLoaderUtil.getStream(resource, useConfiguredLoader);
    if (inputStream == null) {
      throw new IllegalArgumentException("resource not found: " + resource);
    }
    InputSource inputSource = new InputSource(inputStream);
    return parseXmlInputSource(inputSource);
  }

  public static Document parseXmlInputStream(InputStream inputStream) {
    Document document = null;
    try {
      document = getDocumentBuilder().parse(inputStream);
    }
    catch (IOException e) {
      throw new XmlException("could not read input", e);
    }
    catch (SAXException e) {
      throw new XmlException("failed to parse xml", e);
    }
    return document;
  }

  public static Document parseXmlInputSource(InputSource inputSource) {
    Document document = null;
    try {
      document = getDocumentBuilder().parse(inputSource);
    }
    catch (IOException e) {
      throw new XmlException("could not read input", e);
    }
    catch (SAXException e) {
      throw new XmlException("failed to parse xml", e);
    }
    return document;
  }

  public static DocumentBuilder getDocumentBuilder() {
    try {
      return documentBuilderFactory.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      throw new XmlException("failed to create document builder", e);
    }
  }

  public static String attribute(Element element, String attrName) {
    Attr attr = element.getAttributeNode(attrName);
    return attr != null ? attr.getValue() : null;
  }

  public static Iterator elementIterator(Element element, final String tagName) {
    return IteratorUtils.filteredIterator(new NodeIterator(element), new Predicate() {
      public boolean evaluate(Object arg) {
        Node node = (Node) arg;
        return tagName.equals(node.getNodeName());
      }
    });
  }

  public static List elements(Element element, String tagName) {
    ArrayList elements = new ArrayList();
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element && tagName.equals(child.getNodeName())) {
        elements.add(child);
      }
    }
    return elements;
  }

  public static Element element(Element element, String tagName) {
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element && tagName.equals(child.getNodeName())) {
        return (Element) child;
      }
    }
    return null;
  }

  public static Iterator elementIterator(Element element) {
    return IteratorUtils.filteredIterator(new NodeIterator(element), ElementPredicate.INSTANCE);
  }

  private static class ElementPredicate implements Predicate {
    static final Predicate INSTANCE = new ElementPredicate();

    public boolean evaluate(Object arg) {
      return ((Node) arg).getNodeType() == Node.ELEMENT_NODE;
    }
  }

  public static List elements(Element element) {
    ArrayList elements = new ArrayList();
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) elements.add(child);
    }
    return elements;
  }

  public static Element element(Element element) {
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) return (Element) child;
    }
    return null;
  }

  public static String toString(Element element) {
    if (element == null) return "null";
    Source source = new DOMSource(element);

    StringWriter stringWriter = new StringWriter();
    Result result = new StreamResult(stringWriter);
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(source, result);
    }
    catch (TransformerException e) {
      throw new XmlException("could not transform to string: " + element, e);
    }
    return stringWriter.toString();
  }

  public static String getContentText(Element element) {
    StringBuffer buffer = new StringBuffer();
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Text) buffer.append(child.getNodeValue());
    }
    return buffer.toString();
  }
}