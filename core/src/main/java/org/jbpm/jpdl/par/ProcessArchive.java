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
package org.jbpm.jpdl.par;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.jpdl.xml.ProblemListener;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.IoUtil;
import org.jbpm.util.XmlUtil;

public class ProcessArchive implements ProblemListener, Serializable {

  private static final long serialVersionUID = 1L;

  private static final Map parsersByResource = new HashMap();

  // fields ///////////////////////////////////////////////////////////////////

  private String name = "";
  // maps entry-names (String) to byte-arrays (byte[])
  private Map entries = new HashMap();
  private List problems = new ArrayList();

  // constructors /////////////////////////////////////////////////////////////

  public ProcessArchive(ZipInputStream zipInputStream) throws IOException {
    for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipEntry = zipInputStream.getNextEntry()) {
      String entryName = zipEntry.getName();
      byte[] bytes = IoUtil.readBytes(zipInputStream);
      entries.put(entryName, bytes);
    }
  }

  // parse the process definition from the contents ///////////////////////////

  public ProcessDefinition parseProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    for (Iterator iter = getParsers().iterator(); iter.hasNext();) {
      ProcessArchiveParser processArchiveParser = (ProcessArchiveParser) iter.next();
      processDefinition = processArchiveParser.readFromArchive(this, processDefinition);
    }
    if (Problem.containsProblemsOfLevel(problems, Problem.LEVEL_ERROR)) {
      throw new JpdlException(problems);
    }
    return processDefinition;
  }

  // methods for the process archive parsers //////////////////////////////////

  public String toString() {
    return "process-archive(" + name + ")";
  }

  public Map getEntries() {
    return entries;
  }

  public byte[] getEntry(String entryName) {
    return (byte[]) entries.get(entryName);
  }

  public InputStream getEntryInputStream(String entryName) {
    return new ByteArrayInputStream(getEntry(entryName));
  }

  public byte[] removeEntry(String entryName) {
    return (byte[]) entries.remove(entryName);
  }

  public InputStream removeEntryInputStream(String entryName) {
    return new ByteArrayInputStream(removeEntry(entryName));
  }

  public void addProblem(Problem problem) {
    problems.add(problem);
  }

  public List getProblems() {
    return problems;
  }

  public void resetProblems() {
    problems = new ArrayList();
  }

  private static List getParsers() {
    String resource = Configs.getString("resource.parsers");
    synchronized (parsersByResource) {
      List parsers = (List) parsersByResource.get(resource);
      if (parsers == null) {
        parsers = createParsers(resource);
        parsersByResource.put(resource, parsers);
      }
      return parsers;
    }
  }

  private static List createParsers(String resource) {
    // read parsers resource
    InputStream resourceStream = ClassLoaderUtil.getStream(resource);
    Element parsersElement = XmlUtil.parseXmlInputStream(resourceStream).getDocumentElement();
    List parsers = new ArrayList();

    Log log = LogFactory.getLog(ProcessArchive.class);
    boolean debug = log.isDebugEnabled();

    for (Iterator iter = XmlUtil.elementIterator(parsersElement, "parser"); iter.hasNext();) {
      Element parserElement = (Element) iter.next();
      String parserClassName = parserElement.getAttribute("class");
      // load parser class
      try {
        Class parserClass = ClassLoaderUtil.classForName(parserClassName);
        // instantiate parser
        try {
          ProcessArchiveParser parser = (ProcessArchiveParser) parserClass.newInstance();
          if (parser instanceof ConfigurableParser) {
            ((ConfigurableParser) parser).configure(parserElement);
          }
          parsers.add(parser);
          if (debug) log.debug("loaded " + parserClass);
        }
        catch (InstantiationException e) {
          if (debug) log.debug("failed to instantiate " + parserClass, e);
        }
        catch (IllegalAccessException e) {
          if (debug) log.debug(ProcessArchive.class + " has no access to " + parserClass, e);
        }
      }
      catch (ClassNotFoundException e) {
        if (debug) log.debug("parser not found: " + parserClassName, e);
      }
    }
    return parsers;
  }
}
