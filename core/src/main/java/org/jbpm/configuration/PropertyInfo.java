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
package org.jbpm.configuration;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jbpm.JbpmException;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class PropertyInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String setterMethodName;
  private final ObjectInfo propertyValueInfo;

  public PropertyInfo(Element propertyElement, ObjectFactoryParser configParser) {
    // property name
    if (propertyElement.hasAttribute("name")) {
      String propertyName = propertyElement.getAttribute("name");
      if (propertyName.startsWith("is") && propertyName.length() >= 3
        && Character.isUpperCase(propertyName.charAt(2))) {
        setterMethodName = "set" + propertyName.substring(2);
      }
      else {
        setterMethodName = "set" + Character.toUpperCase(propertyName.charAt(0))
          + propertyName.substring(1);
      }
    }
    // setter method
    else if (propertyElement.hasAttribute("setter")) {
      setterMethodName = propertyElement.getAttribute("setter");
    }
    else {
      throw new JbpmException("missing name or setter attribute in property");
    }

    // value info
    Element propertyValueElement = XmlUtil.element(propertyElement);
    propertyValueInfo = configParser.parse(propertyValueElement);
  }

  public void injectProperty(Object object, ObjectFactoryImpl objectFactory) {
    Object value = objectFactory.getObject(propertyValueInfo);
    Method setterMethod = findSetter(object.getClass(), value.getClass());
    try {
      setterMethod.invoke(object, new Object[] { value });
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(getClass() + " has no access to " + setterMethod, e);
    }
    catch (InvocationTargetException e) {
      throw new JbpmException(setterMethod + " threw exception", e.getCause());
    }
  }

  private Method findSetter(Class type, Class propertyType) {
    Class[] parameterTypes = { propertyType };
    try {
      return type.getMethod(setterMethodName, parameterTypes);
    }
    catch (NoSuchMethodException e) {
      for (Class ancestor = type; ancestor != Object.class; ancestor = ancestor.getSuperclass()) {
        try {
          Method method = type.getDeclaredMethod(setterMethodName, parameterTypes);
          method.setAccessible(true);
          return method;
        }
        catch (NoSuchMethodException e2) {
          // keep looking
        }
      }
      // could not find setter by name and type, search by name alone
      return findSetter(type);
    }
  }

  public Method findSetter(Class type) {
    for (Class ancestor = type; ancestor != Object.class; ancestor = ancestor.getSuperclass()) {
      Method[] methods = ancestor.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];
        if (method.getName().equals(setterMethodName) && method.getParameterTypes().length == 1) {
          if (!Modifier.isPublic(method.getModifiers())) method.setAccessible(true);
          return method;
        }
      }
    }

    throw new JbpmException("missing setter '" + setterMethodName + "' in " + type);
  }
}
