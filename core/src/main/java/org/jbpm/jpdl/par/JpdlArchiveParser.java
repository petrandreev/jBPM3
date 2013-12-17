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

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.xml.sax.InputSource;

public class JpdlArchiveParser implements ProcessArchiveParser {

  private static final long serialVersionUID = 1L;

  public ProcessDefinition readFromArchive(ProcessArchive processArchive,
    ProcessDefinition processDefinition) throws JpdlException {
    // getting the value
    byte[] processBytes = processArchive.getEntry("processdefinition.xml");
    if (processBytes == null) {
      throw new JpdlException("no processdefinition.xml inside process archive");
    }

    ByteArrayInputStream processStream = new ByteArrayInputStream(processBytes);
    JpdlXmlReader jpdlXmlReader = new JpdlXmlReader(new InputSource(processStream),
      processArchive);
    return jpdlXmlReader.readProcessDefinition();
  }
}
