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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class StartJBossTask extends Task {

  private static final String END_MESSAGE = " Started in ";

  String configuration = null;

  public void execute() throws BuildException {
    try {
      // get some environment variableInstances
      char fileSeparator = File.separatorChar;
      boolean isWindows = System.getProperty("os.name").startsWith("Windows");

      // build the command string
      String command = getProject().getProperty("jboss.home")
        + fileSeparator
        + "bin"
        + fileSeparator
        + (isWindows ? "run.bat" : "run.sh")
        + getConfigParameter();

      // launch the command and wait till the END_MESSAGE appears
      Thread launcher = new Launcher(this, command, END_MESSAGE);
      launcher.start();
      launcher.join();

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  String getConfigParameter() {
    if (configuration == null) return "";
    return "-c " + configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }
}
