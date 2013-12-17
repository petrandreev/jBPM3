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
package org.jbpm.db.hibernate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.Converter;
import org.jbpm.util.ClassLoaderUtil;

/**
 * provides access to the list of converters and ensures that the converter objects are unique.
 */
public class Converters {

  private static final Map convertersByResource = new HashMap();

  private Converters() {
    // prevent instantiation
  }

  // public methods

  public static Converter getConverterByClassName(String className) {
    for (Iterator iter = getConverters().values().iterator(); iter.hasNext();) {
      Converter converter = (Converter) iter.next();
      if (className.equals(converter.getClass().getName())) return converter;
    }
    throw new JbpmException(className + " is not registered as a converter");
  }

  public static Converter getConverterByDatabaseId(String converterId) {
    return (Converter) getConverters().get(converterId);
  }

  public static String getConverterId(Converter converter) {
    for (Iterator iter = getConverters().entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      if (converter == entry.getValue()) return (String) entry.getKey();
    }
    return null;
  }

  private static Map getConverters() {
    String resource = Configs.getString("resource.converter");
    synchronized (convertersByResource) {
      Map converters = (Map) convertersByResource.get(resource);
      if (converters == null) {
        Properties properties = ClassLoaderUtil.getProperties(resource);
        converters = createConverters(properties);
        convertersByResource.put(resource, converters);
      }
      return converters;
    }
  }

  private static Map createConverters(Properties properties) {
    Map converters = new HashMap();
    boolean debug = log.isDebugEnabled();
    for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      // validate converter id
      String converterId = (String) entry.getKey();
      if (converterId.length() != 1) {
        throw new JbpmException("converter-id must be a single char");
      }
      // load converter class
      String converterClassName = (String) entry.getValue();
      try {
        Class converterClass = ClassLoaderUtil.classForName(converterClassName);
        // instantiate converter
        try {
          Converter converter = (Converter) converterClass.newInstance();
          converters.put(converterId, converter);
          if (debug) log.debug("registered " + converterClassName);
        }
        catch (InstantiationException e) {
          if (debug) log.debug("failed to instantiate " + converterClass, e);
        }
        catch (IllegalAccessException e) {
          if (debug) log.debug(Converters.class + " has no access to " + converterClass, e);
        }
      }
      catch (ClassNotFoundException e) {
        if (debug) log.debug("converter class not found: " + converterClassName, e);
      }
    }
    return converters;
  }

  private static final Log log = LogFactory.getLog(Converters.class);
}
