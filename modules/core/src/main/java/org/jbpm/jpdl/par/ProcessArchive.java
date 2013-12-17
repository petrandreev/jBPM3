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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.jpdl.xml.ProblemListener;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.IoUtil;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProcessArchive implements ProblemListener
{

  private static final long serialVersionUID = 1L;

  static List processArchiveParsers = getProcessArchiveParsers();

  // fields ///////////////////////////////////////////////////////////////////

  String name = "";
  // maps entry-names (String) to byte-arrays (byte[])
  Map entries = new HashMap();
  List problems = new ArrayList();

  // constructors /////////////////////////////////////////////////////////////

  public ProcessArchive(ZipInputStream zipInputStream) throws IOException
  {
    ZipEntry zipEntry = zipInputStream.getNextEntry();
    while (zipEntry != null)
    {
      String entryName = zipEntry.getName();
      byte[] bytes = IoUtil.readBytes(zipInputStream);
      if (bytes != null)
      {
        entries.put(entryName, bytes);
      }
      zipEntry = zipInputStream.getNextEntry();
    }
  }

  // parse the process definition from the contents ///////////////////////////

  public ProcessDefinition parseProcessDefinition()
  {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    Iterator iter = processArchiveParsers.iterator();
    while (iter.hasNext())
    {
      ProcessArchiveParser processArchiveParser = (ProcessArchiveParser)iter.next();
      processDefinition = processArchiveParser.readFromArchive(this, processDefinition);
    }
    if (Problem.containsProblemsOfLevel(problems, Problem.LEVEL_ERROR))
    {
      throw new JpdlException(problems);
    }
    return processDefinition;
  }

  // methods for the process archive parsers //////////////////////////////////

  public String toString()
  {
    return "process-archive(" + name + ")";
  }

  public Map getEntries()
  {
    return entries;
  }

  public byte[] getEntry(String entryName)
  {
    return (byte[])entries.get(entryName);
  }

  public InputStream getEntryInputStream(String entryName)
  {
    return new ByteArrayInputStream(getEntry(entryName));
  }

  public byte[] removeEntry(String entryName)
  {
    return (byte[])entries.remove(entryName);
  }

  public InputStream removeEntryInputStream(String entryName)
  {
    return new ByteArrayInputStream(removeEntry(entryName));
  }

  public void addProblem(Problem problem)
  {
    problems.add(problem);
  }

  public List getProblems()
  {
    return problems;
  }

  public void resetProblems()
  {
    problems = new ArrayList();
  }

  static List getProcessArchiveParsers()
  {
    List processArchiveParsers = new ArrayList();
    try
    {
      String resource = JbpmConfiguration.Configs.getString("resource.parsers");
      InputStream parsersStream = ClassLoaderUtil.getStream(resource);
      Document document = XmlUtil.parseXmlInputStream(parsersStream);
      Iterator iter = XmlUtil.elementIterator(document.getDocumentElement(), "parser");
      while (iter.hasNext())
      {
        Element element = (Element)iter.next();
        String className = element.getAttribute("class");
        ProcessArchiveParser processArchiveParser = (ProcessArchiveParser)ClassLoaderUtil.classForName(className).newInstance();
        if (processArchiveParser instanceof ConfigurableParser)
        {
          ((ConfigurableParser)processArchiveParser).configure(element);
        }
        processArchiveParsers.add(processArchiveParser);
      }
    }
    catch (Exception e)
    {
      throw new JbpmException("couldn't parse process archive parsers (jbpm.parsers.xml)", e);
    }
    return processArchiveParsers;
  }

}
