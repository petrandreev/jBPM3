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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.util.ConfigHelper;
import org.jbpm.db.compatibility.JbpmSchemaUpdate;
import org.jbpm.util.CollectionUtil;

public class JbpmSchemaTask extends Task {
  String config;
  String properties;
  String action;
  String output;
  String delimiter;

  public void execute() throws BuildException {
    if (action == null)
      action = "create";

    if (config == null)
      config = "hibernate.cfg.xml";

    List<? extends Exception> exceptions;
    try {
      Configuration configuration = getConfiguration();
      if ("drop".equalsIgnoreCase(action)) {
        SchemaExport schemaExport = getSchemaExport(configuration);
        schemaExport.drop(false, false);
        exceptions = CollectionUtil.checkList(schemaExport.getExceptions(), Exception.class);
      }
      else if ("create".equalsIgnoreCase(action)) {
        SchemaExport schemaExport = getSchemaExport(configuration);
        schemaExport.execute(false, false, false, true);
        exceptions = CollectionUtil.checkList(schemaExport.getExceptions(), Exception.class);
      }
      else if ("update".equalsIgnoreCase(action)) {
        JbpmSchemaUpdate schemaUpdate = getSchemaUpdate(configuration);
        schemaUpdate.execute(false, false);
        exceptions = schemaUpdate.getExceptions();
      }
      else {
        throw new BuildException("Unsupported action: " + action);
      }
    }
    catch (IOException ex) {
      exceptions = Collections.singletonList(ex);
    }

    // Print the exceptions if there are any
    for (Exception exception : exceptions) {
      log(exception.getMessage(), Project.MSG_ERR);
    }
  }

  private Configuration getConfiguration() throws IOException {
    log("Action '" + action + "' using " + config + ", " + properties);

    Configuration configuration = new Configuration();
    configuration.configure(config);

    if (properties != null) {
      InputStream inStream = ConfigHelper.getResourceAsStream(properties);
      if (inStream == null) {
        throw new FileNotFoundException("Could not find properties: " + properties);
      }
      try {
        Properties properties = new Properties();
        properties.load(inStream);
        configuration.addProperties(properties);
      }
      finally {
        inStream.close();
      }
    }
    return configuration;
  }

  private SchemaExport getSchemaExport(Configuration configuration) {
    SchemaExport schemaExport = new SchemaExport(configuration);

    if (output != null)
      schemaExport.setOutputFile(output);

    if (delimiter != null)
      schemaExport.setDelimiter(delimiter);

    schemaExport.setFormat(false);
    return schemaExport;
  }

  private JbpmSchemaUpdate getSchemaUpdate(Configuration configuration) {
    JbpmSchemaUpdate schemaUpdate = new JbpmSchemaUpdate(configuration);

    if (output != null)
      schemaUpdate.setOutputFile(new File(output));

    if (delimiter != null)
      schemaUpdate.setDelimiter(delimiter);

    return schemaUpdate;
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

  public void setOutput(String output) {
    this.output = output;
  }
}
