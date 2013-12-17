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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.SQLExec.DelimiterType;
import org.hibernate.cfg.Configuration;

import org.jbpm.db.JbpmSchema;
import org.jbpm.util.IoUtil;

public class JbpmSchemaTask extends Task {

  String config = "hibernate.cfg.xml";
  String properties;
  String action = "create";
  String output;
  String delimiter = ";";
  String delimiterType = DelimiterType.NORMAL;

  public void execute() throws BuildException {
    // Create schema tool
    JbpmSchema jbpmSchema = getJbpmSchema();

    // Generate script
    String[] script;
    if ("create".equalsIgnoreCase(action)) {
      script = jbpmSchema.getCreateSql();
    }
    else if ("update".equalsIgnoreCase(action)) {
      try {
        script = jbpmSchema.getUpdateSql();
      }
      catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }
    else if ("drop".equalsIgnoreCase(action)) {
      script = jbpmSchema.getDropSql();
    }
    else if ("clean".equalsIgnoreCase(action)) {
      script = jbpmSchema.getCleanSql();
    }
    else {
      throw new BuildException("Unsupported action: " + action);
    }

    // Print exceptions, if any
    List exceptions = jbpmSchema.getExceptions();
    if (!exceptions.isEmpty()) {
      for (Iterator i = exceptions.iterator(); i.hasNext();) {
        Object exception = i.next();
        log(exception.toString(), Project.MSG_ERR);
      }
    }

    // Save script to output file
    try {
      saveScript(script, jbpmSchema);
    }
    catch (IOException e) {
      log(e.toString(), Project.MSG_ERR);
      throw new BuildException("Failed to write script to " + output);
    }
  }

  private JbpmSchema getJbpmSchema() {
    Configuration configuration = AntHelper.getConfiguration(config, properties);

    JbpmSchema jbpmSchema = new JbpmSchema(configuration);
    jbpmSchema.setDelimiter(DelimiterType.ROW.equals(delimiterType) ? IoUtil.lineSeparator
      + delimiter : delimiter);
    return jbpmSchema;
  }

  private void saveScript(String[] script, JbpmSchema jbpmSchema) throws IOException {
    Writer writer = new FileWriter(output);
    try {
      jbpmSchema.writeSql(writer, script);
    }
    finally {
      writer.close();
    }
  }

  public void setAction(String action) {
    this.action = action;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public void setDelimiterType(DelimiterType delimiterType) {
    this.delimiterType = delimiterType.getValue();
  }

  public void setOutput(String output) {
    this.output = output;
  }
}
