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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import org.jbpm.JbpmException;
import org.jbpm.util.ClassLoaderUtil;

public class FieldInstantiator implements Instantiator {

  public Object instantiate(Class clazz, String configuration) {
    // create a new instance with the default constructor
    Object newInstance = newInstance(clazz);

    if (configuration != null && configuration.length() > 0) {
      // parse the bean configuration
      Element configurationElement = parseConfiguration(configuration);

      // loop over the configured properties
      for (Iterator iter = configurationElement.elementIterator(); iter.hasNext();) {
        Element propertyElement = (Element) iter.next();
        String propertyName = propertyElement.getName();
        setPropertyValue(clazz, newInstance, propertyName, propertyElement);
      }
    }
    return newInstance;
  }

  protected void setPropertyValue(Class clazz, Object instance, String propertyName,
    Element propertyElement) {
    Field field = findField(clazz, propertyName);
    if (field != null) {
      field.setAccessible(true);
      Object value = getValue(field.getType(), propertyElement);
      try {
        field.set(instance, value);
      }
      catch (IllegalArgumentException e) {
        log.error("could not set '" + propertyName + "' to: " + value, e);
      }
      catch (IllegalAccessException e) {
        log.error(getClass() + " has no access to " + field, e);
      }
    }
    else {
      log.error("field not found: " + propertyName);
    }
  }

  private Field findField(Class clazz, String propertyName) {
    do {
      try {
        return clazz.getDeclaredField(propertyName);
      }
      catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    } while (clazz != null);

    return null;
  }

  protected Element parseConfiguration(String configuration) {
    try {
      return DocumentHelper.parseText("<action>" + configuration + "</action>")
        .getRootElement();
    }
    catch (DocumentException e) {
      throw new JbpmException("failed to parse configuration", e);
    }
  }

  protected Object newInstance(Class clazz) {
    try {
      return clazz.newInstance();
    }
    catch (InstantiationException e) {
      throw new JbpmException("failed to instantiate " + clazz, e);
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(getClass() + " has no access to " + clazz, e);
    }
  }

  public static Object getValue(Class type, Element propertyElement) {
    // parse the value
    Object value = null;
    if (type == String.class) {
      value = propertyElement.getText();
    }
    else if (type == Integer.class || type == int.class) {
      value = new Integer(propertyElement.getTextTrim());
    }
    else if (type == Long.class || type == long.class) {
      value = new Long(propertyElement.getTextTrim());
    }
    else if (type == Float.class || type == float.class) {
      value = new Float(propertyElement.getTextTrim());
    }
    else if (type == Double.class || type == double.class) {
      value = new Double(propertyElement.getTextTrim());
    }
    else if (type == Boolean.class || type == boolean.class) {
      value = Boolean.valueOf(propertyElement.getTextTrim());
    }
    else if (type == Character.class || type == char.class) {
      value = new Character(propertyElement.getTextTrim().charAt(0));
    }
    else if (type == Short.class || type == short.class) {
      value = new Short(propertyElement.getTextTrim());
    }
    else if (type == Byte.class || type == byte.class) {
      value = new Byte(propertyElement.getTextTrim());
    }
    else if (type == List.class || type == Collection.class) {
      value = getCollection(propertyElement, new ArrayList());
    }
    else if (type == Set.class) {
      value = getCollection(propertyElement, new HashSet());
    }
    else if (type == SortedSet.class) {
      value = getCollection(propertyElement, new TreeSet());
    }
    else if (type == Map.class) {
      value = getMap(propertyElement, new HashMap());
    }
    else if (type == SortedMap.class) {
      value = getMap(propertyElement, new TreeMap());
    }
    else if (Element.class.isAssignableFrom(type)) {
      value = propertyElement;
    }
    else {
      try {
        if (Collection.class.isAssignableFrom(type)) {
          value = getCollection(propertyElement, (Collection) type.newInstance());
        }
        else if (Map.class.isAssignableFrom(type)) {
          value = getMap(propertyElement, (Map) type.newInstance());
        }
        else if (propertyElement.isTextOnly()) {
          Constructor constructor = type.getConstructor(new Class[] {
            String.class
          });
          try {
            value = constructor.newInstance(new Object[] {
              propertyElement.getTextTrim()
            });
          }
          catch (IllegalAccessException e) {
            log.error(FieldInstantiator.class + " has no access to " + constructor);
          }
          catch (InvocationTargetException e) {
            log.error(constructor + " threw exception", e.getCause());
          }
        }
        else {
          log.error("element '" + propertyElement.getName() + "' has non-text content");
        }
      }
      catch (InstantiationException e) {
        log.error("failed to instantiate " + type, e);
      }
      catch (IllegalAccessException e) {
        log.error(FieldInstantiator.class + " has no access to " + type, e);
      }
      catch (NoSuchMethodException e) {
        log.error("constructor not found: " + type.getName() + "(String)", e);
      }
    }
    return value;
  }

  private static Map getMap(Element mapElement, Map map) {
    Class keyClass = classForAttributeValue(mapElement, "key-type");
    Class valueClass = classForAttributeValue(mapElement, "value-type");

    for (Iterator iter = mapElement.elementIterator(); iter.hasNext();) {
      Element element = (Element) iter.next();
      Element keyElement = element.element("key");
      Element valueElement = element.element("value");

      map.put(getValue(keyClass, keyElement), getValue(valueClass, valueElement));
    }
    return map;
  }

  private static Object getCollection(Element collectionElement, Collection collection) {
    Class elementClass = classForAttributeValue(collectionElement, "element-type");

    for (Iterator iter = collectionElement.elementIterator(); iter.hasNext();) {
      Element element = (Element) iter.next();
      collection.add(getValue(elementClass, element));
    }
    return collection;
  }

  /**
   * Returns the <code>Class</code> associated with the value for the attribute with the given
   * name.
   */
  private static Class classForAttributeValue(Element element, String attributeName) {
    String className = element.attributeValue(attributeName);
    if (className != null) {
      try {
        return ClassLoaderUtil.classForName(className);
      }
      catch (ClassNotFoundException e) {
        throw new JbpmException("no such class " + className, e);
      }
    }
    return String.class;
  }

  private static final Log log = LogFactory.getLog(FieldInstantiator.class);
}
