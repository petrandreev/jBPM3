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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hibernate.cfg.Configuration;

/**
 * @author Alejandro Guizar
 */
public class HibernatePropertiesTask extends Task {

  private String config;
  private String prefix;
  private Pattern includes;
  private Pattern excludes;

  public void execute() throws BuildException {
    log("loading hibernate properties from " + config);

    Configuration configuration = AntHelper.getConfiguration(config, null);
    Properties properties = configuration.getProperties();
    if (properties.isEmpty()) return;

    StringBuffer nameBuf = new StringBuffer(prefix);
    int prefixLength = prefix.length();

    Project project = getProject();
    for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
      Map.Entry property = (Entry) i.next();

      String name = (String) property.getKey();
      if (include(name) && !exclude(name)) {
        name = nameBuf.append(name).toString();

        String value = (String) property.getValue();
        log("setting '" + name + "' to: " + value);
        project.setNewProperty(name, value);

        // drop key from prefix
        nameBuf.setLength(prefixLength);
      }
    }
  }

  private boolean include(String name) {
    return includes == null || includes.matcher(name).matches();
  }

  private boolean exclude(String name) {
    return excludes != null && excludes.matcher(name).matches();
  }

  public void setConfig(String config) {
    this.config = config;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setIncludes(String regex) {
    includes = Pattern.compile(regex);
  }

  public void setExcludes(String regex) {
    excludes = Pattern.compile(regex);
  }

}
