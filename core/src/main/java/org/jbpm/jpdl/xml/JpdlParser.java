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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.util.ClassLoaderUtil;

/**
 * This helper class wraps the processing of XML schema documents to aid {@link JpdlXmlReader}
 * in validation.
 * 
 * @author Tom Baeyens
 * @author Jim Rigsbee
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JpdlParser implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
  private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
  private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

  private static final SAXParserFactory saxParserFactory = createSaxParserFactory();
  private static final Set schemaResources = (Set) Configs.getObject("jbpm.schema.resources");
  private static String[] schemaSource;

  private JpdlParser() {
    // hide default constructor to prevent instantiation
  }

  public static Document parse(InputSource inputSource, ProblemListener problemListener)
    throws DocumentException {
    try {
      SAXReader saxReader = createSaxReader(problemListener);
      return saxReader.read(inputSource);
    }
    catch (SAXException e) {
      throw new DocumentException("failed to create sax reader", e);
    }
  }

  public static SAXReader createSaxReader(ProblemListener problemListener) throws SAXException {
    XMLReader xmlReader = createXmlReader();
    SAXReader saxReader = new SAXReader(xmlReader);
    saxReader.setErrorHandler(new JpdlErrorHandler(problemListener));
    return saxReader;
  }

  public static XMLReader createXmlReader() throws SAXException {
    SAXParser saxParser;
    try {
      saxParser = saxParserFactory.newSAXParser();
    }
    catch (ParserConfigurationException e) {
      // validating, namespace-aware sax parser should be available
      throw new AssertionError(e);
    }

    try {
      saxParser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
    }
    catch (SAXException e) {
      log.warn("failed to set schema language to xml schema", e);
    }

    Object schemaSource = getSchemaSource();
    try {
      saxParser.setProperty(JAXP_SCHEMA_SOURCE, schemaSource);
    }
    catch (SAXException e) {
      log.warn("failed to set schema source to " + schemaSource, e);
    }

    XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
    xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    try {
      xmlReader.setFeature("http://apache.org/xml/features/validation/dynamic", true);
    }
    catch (SAXException e) {
      log.warn("failed to enable dynamic validation", e);
    }
    return xmlReader;
  }

  private synchronized static Object getSchemaSource() {
    if (schemaSource == null) {
      ClassLoader classLoader = ClassLoaderUtil.getClassLoader();
      List schemaLocations = new ArrayList(schemaResources.size());
      for (Iterator i = schemaResources.iterator(); i.hasNext();) {
        String schemaResource = (String) i.next();
        URL schemaURL = classLoader.getResource(schemaResource);
        if (schemaURL != null) {
          String schemaLocation = schemaURL.toString();
          if (log.isDebugEnabled()) log.debug("located schema resource " + schemaResource);
          schemaLocations.add(schemaLocation);
        }
      }
      schemaSource = new String[schemaLocations.size()];
      schemaLocations.toArray(schemaSource);
    }
    return schemaSource;
  }

  private static class JpdlErrorHandler implements ErrorHandler, Serializable {

    private ProblemListener problemListener;

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

  public synchronized static void addSchemaResource(String resourceName) {
    // register resource
    schemaResources.add(resourceName);
    // invalidate existing schema source
    schemaSource = null;
  }

  private static SAXParserFactory createSaxParserFactory() {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setValidating(true);
    saxParserFactory.setNamespaceAware(true);
    return saxParserFactory;
  }

  private static final Log log = LogFactory.getLog(JpdlParser.class);
}
