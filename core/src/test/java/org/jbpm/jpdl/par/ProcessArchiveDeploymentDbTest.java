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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.IoUtil;

public class ProcessArchiveDeploymentDbTest extends AbstractDbTestCase {

  public void testDeployProcess() throws Exception {
    // create a process archive
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
    addEntry(zipOutputStream, "processdefinition.xml",
      "org/jbpm/jpdl/par/deployableprocess.xml");
    zipOutputStream.close();
    byte[] zipBytes = baos.toByteArray();

    // deploy the process archive
    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
    ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(zipInputStream);
    deployProcessDefinition(processDefinition);

    List allProcessDefinitions = graphSession.findAllProcessDefinitionVersions(processDefinition.getName());
    assertEquals(1, allProcessDefinitions.size());

    processDefinition = (ProcessDefinition) allProcessDefinitions.get(0);
    assertEquals("the deployable process", processDefinition.getName());
  }

  public void testDeployProcessWithFile() throws Exception {
    // create a process archive
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
    addEntry(zipOutputStream, "processdefinition.xml",
      "org/jbpm/jpdl/par/deployableprocess.xml");
    addEntry(zipOutputStream, "classes/org/jbpm/jpdl/par/ProcessArchiveDeploymentDbTest.class",
      "org/jbpm/jpdl/par/ProcessArchiveDeploymentDbTest.class");
    zipOutputStream.close();
    byte[] zipBytes = baos.toByteArray();

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
    ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(zipInputStream);
    deployProcessDefinition(processDefinition);

    List allProcessDefinitions = graphSession.findAllProcessDefinitionVersions(processDefinition.getName());
    assertEquals(1, allProcessDefinitions.size());
    processDefinition = (ProcessDefinition) allProcessDefinitions.get(0);
    byte[] processBytes = processDefinition.getFileDefinition().getBytes(
      "classes/org/jbpm/jpdl/par/ProcessArchiveDeploymentDbTest.class");
    byte[] expectedBytes = IoUtil.readBytes(ClassLoaderUtil.getStream("org/jbpm/jpdl/par/ProcessArchiveDeploymentDbTest.class"));

    assertTrue(Arrays.equals(expectedBytes, processBytes));
  }

  public void testDeployTwoVersionOfProcess() throws Exception {
    // create a process archive
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
    addEntry(zipOutputStream, "processdefinition.xml",
      "org/jbpm/jpdl/par/deployableprocess.xml");
    zipOutputStream.close();
    byte[] zipBytes = baos.toByteArray();

    // deploy the saved process archive
    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
    ProcessDefinition processDefinitionOne = ProcessDefinition.parseParZipInputStream(zipInputStream);
    deployProcessDefinition(processDefinitionOne);

    // deploy the saved process file again
    zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
    ProcessDefinition processDefinitionTwo = ProcessDefinition.parseParZipInputStream(zipInputStream);
    deployProcessDefinition(processDefinitionTwo);

    String processDefinitionName = processDefinitionTwo.getName();
    assertEquals(processDefinitionName, processDefinitionOne.getName());

    assertEquals(2, graphSession.findAllProcessDefinitionVersions(processDefinitionName).size());
    assertEquals(2, graphSession.findLatestProcessDefinition(processDefinitionName)
      .getVersion());
  }

  private static void addEntry(ZipOutputStream zipOutputStream, String entryName,
    String resource) throws IOException {
    InputStream inputStream = ClassLoaderUtil.getStream(resource);
    byte[] bytes = IoUtil.readBytes(inputStream);
    addEntry(zipOutputStream, entryName, bytes);
    inputStream.close();
  }

  private static void addEntry(ZipOutputStream zipOutputStream, String entryName, byte[] content)
    throws IOException {
    ZipEntry zipEntry = new ZipEntry(entryName);
    zipOutputStream.putNextEntry(zipEntry);
    zipOutputStream.write(content);
  }
}
