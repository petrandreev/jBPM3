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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

public class BeanInstantiator extends FieldInstantiator {

  protected void setPropertyValue(Class clazz, Object newInstance, String propertyName,
      Element propertyElement) {
    // create the setter method name from the property name
    String setterMethodName = "set"
        + Character.toUpperCase(propertyName.charAt(0))
        + propertyName.substring(1);

    // find the setter method
    Method method = findSetter(clazz, setterMethodName);

    // if the setter method was found
    if (method != null) {
      // make it accessible
      method.setAccessible(true);
      // invoke it
      Class propertyType = method.getParameterTypes()[0];
      Object value = getValue(propertyType, propertyElement);
      try {
        method.invoke(newInstance, new Object[] { value });
      }
      catch (IllegalArgumentException e) {
        log.error("could not set '" + propertyName + "' to: " + value, e);
      }
      catch (IllegalAccessException e) {
        log.error(getClass() + " has no access to " + method, e);
      }
      catch (InvocationTargetException e) {
        log.error(method + " threw exception", e.getCause());
      }
    }
    else {
      log.error("property '" + propertyName + "' has no setter method");
    }
  }

  private Method findSetter(Class clazz, String setterMethodName) {
    do {
      Method[] methods = clazz.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];
        if (method.getName().equals(setterMethodName) && method.getParameterTypes().length == 1)
          return method;
      }
      clazz = clazz.getSuperclass();
    } while (clazz != null);

    return null;
  }

  private static final Log log = LogFactory.getLog(BeanInstantiator.class);
}
