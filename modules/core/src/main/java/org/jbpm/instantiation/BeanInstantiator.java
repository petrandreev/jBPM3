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

  protected void setPropertyValue(Object instance, String propertyName, Element propertyElement) {
    // create the setter method name from the property name
    String setterName = "set"
        + Character.toUpperCase(propertyName.charAt(0))
        + propertyName.substring(1);

    // find the setter method
    Method method = findSetter(instance.getClass(), setterName);

    // if the setter method was found
    if (method != null) {
      // make it accessible
      method.setAccessible(true);
      // invoke it
      Class<?> propertyType = method.getParameterTypes()[0];
      Object value = getValue(propertyType, propertyElement);
      try {
        method.invoke(instance, value);
      }
      catch (IllegalAccessException e) {
        log.error("property '" + propertyName + "' is inaccesible", e);
      }
      catch (IllegalArgumentException e) {
        log.error("property '" + propertyName + "' cannot be set to value " + value, e);
      }
      catch (InvocationTargetException e) {
        log.error("write method for property '" + propertyName + "' threw exception", e.getCause());
      }
    }
    else {
      log.error("property '" + propertyName + "' does not exist or is not writeable");
    }
  }

  private Method findSetter(Class<?> clazz, String setterName) {
    for (Method method : clazz.getDeclaredMethods()) {
      if (setterName.equals(method.getName()) && method.getParameterTypes().length == 1) {
        return method;
      }
    }
    Class<?> superclass = clazz.getSuperclass();
    return superclass != null ? findSetter(superclass, setterName) : null;
  }

  private static final Log log = LogFactory.getLog(BeanInstantiator.class);
}
