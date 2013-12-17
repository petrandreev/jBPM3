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
package org.jbpm.instantiation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jbpm.JbpmException;

/**
 * @author Alejandro Guizar
 */
class InstantiatorUtil {

  private InstantiatorUtil() {
    // hide default constructor to prevent instantiation
  }

  public static <T> T instantiate(Class<T> type) {
    try {
      return type.newInstance();
    }
    catch (InstantiationException e) {
      throw new JbpmException("could not instantiate " + type, e);
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(type + " is inaccessible", e);
    }
  }

  public static <T, C> T instantiate(Class<T> type, Class<C> configType, C config) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor(configType);
      constructor.setAccessible(true);
      return constructor.newInstance(config);
    }
    catch (NoSuchMethodException e) {
      throw new JbpmException(type
          + " does not have a "
          + config.getClass().getSimpleName()
          + " constructor", e);
    }
    catch (InstantiationException e) {
      throw new JbpmException("could not instantiate " + type, e);
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(type + " is inaccessible", e);
    }
    catch (IllegalArgumentException e) {
      throw new JbpmException(type + " cannot be constructed with value " + config, e);
    }
    catch (InvocationTargetException e) {
      throw new JbpmException("constructor for " + type + " threw exception", e.getCause());
    }
  }

  public static Element parseConfiguration(String config) {
    Element element = null;
    try {
      element = DocumentHelper.parseText("<action>" + config + "</action>").getRootElement();
    }
    catch (DocumentException e) {
      throw new JbpmException("failed to parse configuration: " + config, e);
    }
    return element;
  }
}
