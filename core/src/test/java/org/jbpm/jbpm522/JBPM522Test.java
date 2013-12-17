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
package org.jbpm.jbpm522;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.tools.ant.Main;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.util.IoUtil;

/**
 * Failed attempts to deploy a process leave a process definition record
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-522">JBPM-522</a>
 * @author Alejandro Guizar
 */
public class JBPM522Test extends AbstractDbTestCase {

  static String buildFilePath;

  public static Test suite() {
    return new TestSetup(new TestSuite(JBPM522Test.class)) {
      private File temporaryBuildFile;

      protected void setUp() throws Exception {
        URL buildFileLocator = JBPM522Test.class.getResource("build.xml");
        String protocol = buildFileLocator.getProtocol();

        if ("file".equals(protocol)) {
          buildFilePath = buildFileLocator.getPath();
        }
        else if ("jar".equals(protocol)) {
          // copy to temporary file
          InputStream buildFileStream = buildFileLocator.openStream();
          try {
            temporaryBuildFile = File.createTempFile("build", ".xml");

            OutputStream temporaryFileStream = new FileOutputStream(temporaryBuildFile);
            try {
              IoUtil.transfer(buildFileStream, temporaryFileStream);
            }
            finally {
              temporaryFileStream.close();
            }
          }
          finally {
            buildFileStream.close();
          }
          buildFilePath = temporaryBuildFile.getPath();
        }
        else {
          throw new AssertionError(protocol);
        }
      }

      protected void tearDown() throws Exception {
        if (temporaryBuildFile != null) temporaryBuildFile.delete();
      }
    };
  }

  protected void setUp() throws Exception {
    super.setUp();
    runTarget(getName());
  }

  public void testDeployProcess() {
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("timerProcess");
    assertEquals(1, processDefinitions.size());

    ProcessDefinition processDefinition = (ProcessDefinition) processDefinitions.get(0);
    assertEquals("timerProcess", processDefinition.getName());
    graphSession.deleteProcessDefinition(processDefinition);
  }

  public void testDeployBadProcess() {
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("sequential process");
    assertEquals(0, processDefinitions.size());
  }

  public void testDeployProcesses() {
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("classLoadingProcess");
    assertEquals(1, processDefinitions.size());

    ProcessDefinition processDefinition = (ProcessDefinition) processDefinitions.get(0);
    assertEquals("classLoadingProcess", processDefinition.getName());
    graphSession.deleteProcessDefinition(processDefinition);

    processDefinitions = graphSession.findAllProcessDefinitionVersions("timerProcess");
    assertEquals(1, processDefinitions.size());

    processDefinition = (ProcessDefinition) processDefinitions.get(0);
    assertEquals("timerProcess", processDefinition.getName());
    graphSession.deleteProcessDefinition(processDefinition);
  }

  public void testDeployBadProcesses() {
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("timerProcess");
    assertEquals(0, processDefinitions.size());

    processDefinitions = graphSession.findAllProcessDefinitionVersions("sequential process");
    assertEquals(0, processDefinitions.size());

    processDefinitions = graphSession.findAllProcessDefinitionVersions("classLoadingProces");
    assertEquals(0, processDefinitions.size());
  }

  private void runTarget(String target) {
    PrintStream stdout = System.out;
    PrintStream stderr = System.err;

    ByteArrayOutputStream memout = new ByteArrayOutputStream();
    PrintStream prnout = new PrintStream(memout);

    try {
      System.setOut(prnout);
      System.setErr(prnout);

      Main antMain = new Main() {
        protected void exit(int exitCode) {
          // prevent ant from terminating the VM
        }
      };
      String[] args = {
        "-buildfile", buildFilePath, target
      };
      antMain.startAnt(args, System.getProperties(), Thread.currentThread()
        .getContextClassLoader());
    }
    finally {
      System.setOut(stdout);
      System.setErr(stderr);

      log.info(new String(memout.toByteArray()));
    }
  }
}
