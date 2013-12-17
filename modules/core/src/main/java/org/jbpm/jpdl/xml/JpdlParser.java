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

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import org.jbpm.util.ClassLoaderUtil;

/**
 * Validate an XML document using JAXP techniques and an XML Schema. This helper
 * class wraps the processing of a schema to aid in schema validation throughout
 * the product.
 * 
 * @author Tom Baeyens
 * @author Jim Rigsbee
 */
public class JpdlParser implements Serializable {

  private static final long serialVersionUID = 1L;

  static SAXParserFactory saxParserFactory = createSaxParserFactory();
  private static Set schemaResources = getDefaultSchemaResources();
  private static Object schemaSource;

  public static Document parse(InputSource inputSource, ProblemListener problemListener) throws Exception {
    Document document = null;
    SAXReader saxReader = createSaxReader(problemListener);
    document = saxReader.read(inputSource);
    return document;
  }

  public static SAXReader createSaxReader(ProblemListener problemListener) throws Exception {
    XMLReader xmlReader = createXmlReader();
    SAXReader saxReader = new SAXReader(xmlReader);
    saxReader.setErrorHandler(new JpdlErrorHandler(problemListener));
    return saxReader;
  }
  
  public static XMLReader createXmlReader() throws Exception {
    SAXParser saxParser = saxParserFactory.newSAXParser();
    XMLReader xmlReader = saxParser.getXMLReader();
    
    try {
      saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
    } catch (SAXException e){
      log.warn("couldn't set schema language property", e);
    }

    try {
      saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", getSchemaSource());
    } catch (SAXException e){
      log.warn("couldn't set schema source property", e);
    }

    try {
      xmlReader.setFeature("http://apache.org/xml/features/validation/dynamic", true);
    } catch (SAXException e){
      log.warn("couldn't set dynamic validation feature", e);
    }
    return xmlReader;
  }

  private static Object getSchemaSource() {
    if (schemaSource == null) {
      ClassLoader classLoader = ClassLoaderUtil.getClassLoader();
      List schemaLocations = new ArrayList(schemaResources.size()); 
      for (Iterator i = schemaResources.iterator(); i.hasNext();) {
        String schemaResource = (String) i.next();
        URL schemaURL = classLoader.getResource(schemaResource);
        if (schemaURL != null) {
          String schemaLocation = schemaURL.toString();
          log.debug("schema resource found: " + schemaResource);
          schemaLocations.add(schemaLocation);
        }
      }
      schemaSource = schemaLocations.toArray(new String[schemaLocations.size()]);
    }
    return schemaSource;
  }

  static class JpdlErrorHandler implements ErrorHandler, Serializable {

    private ProblemListener problemListener = null;

    private static final long serialVersionUID = 1L;

    JpdlErrorHandler(ProblemListener problemListener) {
      this.problemListener = problemListener;
    }

    public void warning(SAXParseException pe) {
      addProblem(Problem.LEVEL_WARNING, pe);
    }

    public void error(SAXParseException pe) {
      addProblem(Problem.LEVEL_ERROR, pe);
    }

    public void fatalError(SAXParseException pe) {
      addProblem(Problem.LEVEL_FATAL, pe);
    }

    private void addProblem(int level, SAXParseException pe) {
      Problem problem = new Problem(level, pe.getMessage(), pe);
      problem.setResource(pe.getSystemId());
      int line = pe.getLineNumber();
      if (line != -1) problem.setLine(new Integer(line));
      problemListener.addProblem(problem);
    }
  }
  
  public static void addSchemaResource(String resource) {
    schemaResources.add(resource);
    schemaSource = null;
  }

  private static Set getDefaultSchemaResources() {
    Set schemaResources = new HashSet();
    schemaResources.add("org/jbpm/jpdl/xml/jpdl-3.0.xsd");
    schemaResources.add("org/jbpm/jpdl/xml/jpdl-3.1.xsd");
    schemaResources.add("org/jbpm/jpdl/xml/jpdl-3.2.xsd");
    schemaResources.add("org/jbpm/jpdl/xml/jpdl-3.3.xsd");
    schemaResources.add("org/jboss/seam/pageflow-2.0.xsd");
    return schemaResources;
  }

  private static SAXParserFactory createSaxParserFactory() {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setValidating(true);
    saxParserFactory.setNamespaceAware(true);
    return saxParserFactory;
  }

  private static final Log log = LogFactory.getLog(JpdlParser.class);
}
