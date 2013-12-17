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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.Converter;
import org.jbpm.context.exe.JbpmType;
import org.jbpm.util.ClassLoaderUtil;

/**
 * provides access to the list of converters and ensures that the converter objects are unique.
 */
public class Converters {

  // maps class names to unique converter objects
  private static final Map<String, Converter<?, ?>> convertersByClassNames = new HashMap<String, Converter<?, ?>>();
  // maps converter database-id-strings to unique converter objects
  private static final Map<String, Converter<?, ?>> convertersByDatabaseId = new HashMap<String, Converter<?, ?>>();
  // maps unique converter objects to their database-id-string
  private static final Map<Converter<?, ?>, String> convertersIds = new HashMap<Converter<?, ?>, String>();

  private Converters() {
    // hide default constructor to prevent instantiation
  }

  // public methods

  public static Converter<?, ?> getConverterByClassName(String className) {
    Converter<?, ?> converter = convertersByClassNames.get(className);
    if (converter == null) {
      Class<? extends Converter<?, ?>> converterClass = getConverterClass(className);
      try {
        // the converter will register itself
        converter = converterClass.newInstance();
      }
      catch (InstantiationException e) {
        log.debug("could not instantiate converter '" + className + "': " + e);
      }
      catch (IllegalAccessException e) {
        log.debug("could not access converter: '" + className + "': " + e);
      }
    }
    return converter;
  }

  public static Converter<?, ?> getConverterByDatabaseId(String databaseId) {
    if (convertersByDatabaseId.isEmpty()) {
      JbpmType.getJbpmTypes();
    }
    return convertersByDatabaseId.get(databaseId);
  }

  public static String getConverterId(Converter<?, ?> converter) {
    // the converter has registered itself during instantiation
    // hence there is no need to check whether the map is populated already
    return convertersIds.get(converter);
  }

  public static void registerConverter(String databaseId, Converter<?, ?> converter) {
    if (databaseId.length() != 1)
      throw new JbpmException("converter-ids must be of length 1 (to be stored in a char)");
    if (convertersByDatabaseId.containsKey(databaseId))
      throw new JbpmException("duplicate converter id: '" + databaseId + "'");

    log.debug("adding converter '" + databaseId + "', '" + converter + "'");
    convertersByClassNames.put(converter.getClass().getName(), converter);
    convertersByDatabaseId.put(databaseId, converter);
    convertersIds.put(converter, databaseId);
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Converter<?, ?>> getConverterClass(String className) {
    return (Class<? extends Converter<?, ?>>) ClassLoaderUtil.classForName(className).asSubclass(
        Converter.class);
  }

  private static Log log = LogFactory.getLog(Converters.class);
}
