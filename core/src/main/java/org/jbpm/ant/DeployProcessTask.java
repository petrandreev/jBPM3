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
import org.jbpm.util.ArrayUtil;

/**
 * ant task for deploying process archives.
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class DeployProcessTask extends MatchingTask {

  private String jbpmCfg;
  private File process;
  private List fileSets = new ArrayList();
  private boolean failOnError = true;

  public void execute() throws BuildException {
    // get the JbpmConfiguration
    JbpmConfiguration jbpmConfiguration = AntHelper.getJbpmConfiguration(jbpmCfg);

    // if attribute process is set, deploy that process file
    if (process != null) {
      handleProcessFile(jbpmConfiguration, process);
    }

    // iterate over file sets
    for (Iterator iter = fileSets.iterator(); iter.hasNext();) {
      FileSet fileSet = (FileSet) iter.next();
      DirectoryScanner dirScanner = fileSet.getDirectoryScanner(getProject());
      File baseDir = dirScanner.getBasedir();
      String[] includedFiles = dirScanner.getIncludedFiles();
      String[] excludedFiles = dirScanner.getExcludedFiles();

      for (int i = 0; i < includedFiles.length; i++) {
        String fileName = includedFiles[i];
        if (!ArrayUtil.contains(excludedFiles, fileName)) {
          handleProcessFile(jbpmConfiguration, new File(baseDir, fileName));
        }
      }
    }
  }

  private void handleProcessFile(JbpmConfiguration jbpmConfiguration, File processFile) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      ProcessDefinition processDefinition = parseProcessArchive(processFile);
      deployProcessDefinition(processDefinition, jbpmContext);
    }
    catch (IOException e) {
      if (failOnError) {
        throw new BuildException("failed to read file " + processFile, e, getLocation());
      }
      else {
        log("failed to read file " + processFile, e, Project.MSG_ERR);
      }
    }
    finally {
      jbpmContext.close();
    }
  }

  private ProcessDefinition parseProcessArchive(File processFile) throws IOException {
    log("parsing process archive " + processFile);
    ZipInputStream processStream = new ZipInputStream(new FileInputStream(processFile));
    try {
      return ProcessDefinition.parseParZipInputStream(processStream);
    }
    finally {
      processStream.close();
    }
  }

  private void deployProcessDefinition(ProcessDefinition processDefinition,
    JbpmContext jbpmContext) {
    log("deploying " + processDefinition);
    try {
      jbpmContext.deployProcessDefinition(processDefinition);
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      if (failOnError) {
        throw new BuildException("failed to deploy " + processDefinition, e, getLocation());
      }
      else {
        log("failed to deploy " + processDefinition, e, Project.MSG_ERR);
      }
    }
  }

  public void addFileset(FileSet fileSet) {
    if (fileSets == null) fileSets = new ArrayList();
    fileSets.add(fileSet);
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
