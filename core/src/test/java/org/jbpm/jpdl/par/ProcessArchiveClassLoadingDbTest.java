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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.IoUtil;

public class ProcessArchiveClassLoadingDbTest extends AbstractDbTestCase {

  String getTestClassesDir() {
    return ProcessArchiveDeploymentDbTest.class.getProtectionDomain()
      .getCodeSource()
      .getLocation()
      .getFile();
  }

  public void testExecuteResourceUsingProcess() throws Exception {
    // create a process archive
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
    addEntry(zipOutputStream, "processdefinition.xml", "org/jbpm/jpdl/par/resourceprocess.xml");
    addEntry(zipOutputStream, "classes/org/jbpm/jpdl/par/ResourceAction.class",
      "org/jbpm/jpdl/par/ResourceAction.class");
    addEntry(zipOutputStream, "classes/org/jbpm/jpdl/par/classresource.txt",
      "org/jbpm/jpdl/par/classresource.txt");
    addEntry(zipOutputStream, "archiveresource.txt", "org/jbpm/jpdl/par/archiveresource.txt");
    zipOutputStream.close();
    byte[] zipBytes = baos.toByteArray();

    // move the files
    String classOriginalName = getTestClassesDir() + "org/jbpm/jpdl/par/ResourceAction.class";
    String classTmpName = classOriginalName + ".hiddenFromTestClasspath";
    assertTrue(new File(classOriginalName).renameTo(new File(classTmpName)));

    String resourceOriginalName = getTestClassesDir() + "org/jbpm/jpdl/par/classresource.txt";
    String resourceTmpName = resourceOriginalName + ".hiddenFromTestClasspath";
    assertTrue(new File(resourceOriginalName).renameTo(new File(resourceTmpName)));

    String archiveResourceOriginalName = getTestClassesDir()
      + "org/jbpm/jpdl/par/archiveresource.txt";
    String archiveResourceTmpName = archiveResourceOriginalName + ".hiddenFromTestClasspath";
    assertTrue(new File(archiveResourceOriginalName).renameTo(new File(archiveResourceTmpName)));

    try {
      ClassLoader testClassLoader = ProcessArchiveClassLoadingDbTest.class.getClassLoader();
      assertNull(testClassLoader.getResource("org/jbpm/jpdl/par/classresource.txt"));
      assertNull(testClassLoader.getResource("org/jbpm/jpdl/par/archiveresource.txt"));
      try {
        Class.forName("org.jbpm.jpdl.par.ResourceAction", false, testClassLoader);
        fail("expected exception");
      }
      catch (ClassNotFoundException e) {
        // OK
      }

      // deploy the process archive
      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
      ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(zipInputStream);
      deployProcessDefinition(processDefinition);

      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("resourceprocess");
      processInstance.signal();
    }
    finally {
      // put the files back into original position
      new File(classTmpName).renameTo(new File(classOriginalName));
      new File(resourceTmpName).renameTo(new File(resourceOriginalName));
      new File(archiveResourceTmpName).renameTo(new File(archiveResourceOriginalName));
    }
  }

  public void testInstantiateClassInArchive() throws Exception {
    // create a process archive
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
    addEntry(zipOutputStream, "processdefinition.xml",
      "org/jbpm/jpdl/par/instantiateprocess.xml");
    addEntry(zipOutputStream, "classes/org/jbpm/jpdl/par/InstantiateAction.class",
      "org/jbpm/jpdl/par/InstantiateAction.class");
    addEntry(zipOutputStream, "classes/org/jbpm/jpdl/par/InstantiateClass.class",
      "org/jbpm/jpdl/par/InstantiateClass.class");
    zipOutputStream.close();
    byte[] zipBytes = baos.toByteArray();

    // move the files
    String instantiateActionOriginalName = getTestClassesDir()
      + "org/jbpm/jpdl/par/InstantiateAction.class";
    String instantiateActionTmpName = instantiateActionOriginalName
      + ".hiddenFromTestClasspath";
    assertTrue(new File(instantiateActionOriginalName).renameTo(new File(
      instantiateActionTmpName)));

    String instantiateClassOriginalName = getTestClassesDir()
      + "org/jbpm/jpdl/par/InstantiateClass.class";
    String instantiateClassTmpName = instantiateClassOriginalName + ".hiddenFromTestClasspath";
    assertTrue(new File(instantiateClassOriginalName).renameTo(new File(instantiateClassTmpName)));

    try {
      ClassLoader testClassLoader = ProcessArchiveClassLoadingDbTest.class.getClassLoader();
      try {
        Class.forName("org.jbpm.jpdl.par.InstantiateAction", false, testClassLoader);
        fail("expected exception");
      }
      catch (ClassNotFoundException e) {
        // OK
      }

      try {
        Class.forName("org.jbpm.jpdl.par.InstantiateClass", false, testClassLoader);
        fail("expected exception");
      }
      catch (ClassNotFoundException e) {
        // OK
      }

      // deploy the process archive
      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
      ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(zipInputStream);
      deployProcessDefinition(processDefinition);

      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("instantiateprocess");
      processInstance.signal();
    }
    finally {
      // put the files back into original position
      new File(instantiateActionTmpName).renameTo(new File(instantiateActionOriginalName));
      new File(instantiateClassTmpName).renameTo(new File(instantiateClassOriginalName));
    }
  }

  public void testInstantiateClassOutsideArchive() throws Exception {
    // create a process archive
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
    addEntry(zipOutputStream, "processdefinition.xml",
      "org/jbpm/jpdl/par/instantiateprocess.xml");
    addEntry(zipOutputStream, "classes/org/jbpm/jpdl/par/InstantiateAction.class",
      "org/jbpm/jpdl/par/InstantiateAction.class");
    zipOutputStream.close();
    byte[] zipBytes = baos.toByteArray();

    // move the files
    String instantiateActionOriginalName = getTestClassesDir()
      + "org/jbpm/jpdl/par/InstantiateAction.class";
    String instantiateActionTmpName = instantiateActionOriginalName
      + ".hiddenFromTestClasspath";
    assertTrue(new File(instantiateActionOriginalName).renameTo(new File(
      instantiateActionTmpName)));

    try {
      ClassLoader testClassLoader = ProcessArchiveClassLoadingDbTest.class.getClassLoader();
      try {
        Class.forName("org.jbpm.jpdl.par.InstantiateAction", false, testClassLoader);
        fail("expected exception");
      }
      catch (ClassNotFoundException e) {
        // OK
      }
      // InstantiateClass should be visible on the test classpath
      Class.forName("org.jbpm.jpdl.par.InstantiateClass", false, testClassLoader);

      // deploy the process archive
      ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
      ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(zipInputStream);
      deployProcessDefinition(processDefinition);

      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("instantiateprocess");
      processInstance.signal();
    }
    finally {
      // put the files back into original position
      new File(instantiateActionTmpName).renameTo(new File(instantiateActionOriginalName));
    }
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
