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
package org.jbpm.context.exe;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmException;
import org.jbpm.configuration.ObjectFactory;
import org.jbpm.configuration.ObjectFactoryParser;
import org.jbpm.util.ClassLoaderUtil;

/**
 * specifies how jbpm can persist objects of a given type in the database.
 */
public class JbpmType {

  private static Map typesByResource = new HashMap();

  private JbpmTypeMatcher jbpmTypeMatcher;
  private Converter converter;
  private Class variableInstanceClass;

  public JbpmType(JbpmTypeMatcher jbpmTypeMatcher, Converter converter,
    Class variableInstanceClass) {
    this.jbpmTypeMatcher = jbpmTypeMatcher;
    this.converter = converter;
    this.variableInstanceClass = variableInstanceClass;
  }

  public boolean matches(Object value) {
    return jbpmTypeMatcher.matches(value);
  }

  public VariableInstance newVariableInstance() {
    try {
      VariableInstance variableInstance = (VariableInstance) variableInstanceClass.newInstance();
      variableInstance.converter = converter;
      return variableInstance;
    }
    catch (InstantiationException e) {
      throw new JbpmException("failed to instantiate " + variableInstanceClass, e);
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(getClass() + " has no access to " + variableInstanceClass, e);
    }
  }

  public static List getJbpmTypes() {
    if (Configs.hasObject("jbpm.types")) {
      return (List) Configs.getObject("jbpm.types");
    }

    String resource = Configs.getString("resource.varmapping");
    synchronized (typesByResource) {
      List types = (List) typesByResource.get(resource);
      if (types == null) {
        InputStream resourceStream = ClassLoaderUtil.getStream(resource);
        ObjectFactory objectFactory = ObjectFactoryParser.parseInputStream(resourceStream);
        types = (List) objectFactory.createObject("jbpm.types");
        typesByResource.put(resource, types);
      }
      return types;
    }
  }
}
