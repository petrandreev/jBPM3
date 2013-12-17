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
package org.jbpm.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;

/**
 * ant task for deploying process archives.
 */
public class DeployProcessTask extends MatchingTask {

  String jbpmCfg = null;
  File process = null;
  List fileSets = new ArrayList();
  boolean failOnError = true;

  public void execute() throws BuildException {
    // get the JbpmConfiguration
    JbpmConfiguration jbpmConfiguration = AntHelper.getJbpmConfiguration(jbpmCfg);
    
    // if attribute process is set, deploy that process file
    if (process!=null) {
      deployProcessArchive(jbpmConfiguration, process);
    }
    
    // loop over all files that are specified in the filesets
    Iterator iter = fileSets.iterator();
    while (iter.hasNext()) {
      FileSet fileSet = (FileSet) iter.next();
      DirectoryScanner dirScanner = fileSet.getDirectoryScanner(getProject());
      File baseDir = dirScanner.getBasedir();
      String[] includedFiles = dirScanner.getIncludedFiles();
      List excludedFiles = Arrays.asList(dirScanner.getExcludedFiles());

      for (int i = 0; i < includedFiles.length; i++) {
        String fileName = includedFiles[i];
        if (!excludedFiles.contains(fileName)) {
          File file = new File(baseDir, fileName);
          deployProcessArchive(jbpmConfiguration, file);
        }
      }
    }
  }

  private void deployProcessArchive(JbpmConfiguration jbpmConfiguration, File processFile) {
    try {
      log("deploying process from archive "+processFile.getName());
      ProcessDefinition processDefinition = parseProcessArchive(processFile);
      deployProcessDefinition(processDefinition, jbpmConfiguration);
    }
    catch (IOException e) {
      log("failed to read process archive " + processFile.getName(), e, Project.MSG_ERR);
      throw new BuildException(e.getMessage(), e);
    }
    catch (JpdlException e) {
      log("archive " + processFile.getName() + " contains invalid process", e, Project.MSG_ERR);
      throw e;
    }
  }

  private ProcessDefinition parseProcessArchive(File processFile) throws IOException {
    ZipInputStream processStream = new ZipInputStream(new FileInputStream(processFile));
    try {
      ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(processStream);
      log("created process definition " + processDefinition.getName());
      return processDefinition;
    }
    finally {
      processStream.close();
    }
  }

  private void deployProcessDefinition(ProcessDefinition processDefinition, JbpmConfiguration jbpmConfiguration) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      jbpmContext.deployProcessDefinition(processDefinition);
      log("deployed process " + processDefinition.getName() + " successfully");
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      log("failed to deploy process " + processDefinition.getName(), e, Project.MSG_ERR);
      throw e;
    }
    finally {
      jbpmContext.close();
    }
  }

  public void addFileset(FileSet fileSet) {
    this.fileSets.add(fileSet);
  }
  public void setJbpmCfg(String jbpmCfg) {
    this.jbpmCfg = jbpmCfg;
  }
  public void setProcess(File process) {
    this.process = process;
  }
  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }
}
