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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.CollectionUtil;

public class FieldInstantiator implements Instantiator {

  public <T> T instantiate(Class<T> type, String configuration) {
    // create a new instance with the default constructor
    T instance = InstantiatorUtil.instantiate(type);

    if (configuration != null && configuration.length() > 0) {
      // parse the bean configuration
      Element configurationElement = InstantiatorUtil.parseConfiguration(configuration);

      // loop over the configured properties
      for (Object i : configurationElement.elements()) {
        Element propertyElement = (Element) i;
        String propertyName = propertyElement.getName();
        setPropertyValue(instance, propertyName, propertyElement);
      }
    }
    return instance;
  }

  protected void setPropertyValue(Object instance, String propertyName, Element propertyElement) {
    Field field = findField(instance.getClass(), propertyName);
    if (field != null) {
      field.setAccessible(true);
      Object value = getValue(field.getType(), propertyElement);
      try {
        field.set(instance, value);
      }
      catch (IllegalArgumentException e) {
        log.error("field '" + propertyName + "' cannot be set to value " + value, e);
      }
      catch (IllegalAccessException e) {
        log.error(" field '" + propertyName + "' is inaccessible", e);
      }
    }
  }

  private Field findField(Class<?> clazz, String propertyName) {
    try {
      return clazz.getDeclaredField(propertyName);
    }
    catch (NoSuchFieldException e) {
      Class<?> superclass = clazz.getSuperclass();
      return superclass != null ? findField(superclass, propertyName) : null;
    }
  }

  public static Object getValue(Class<?> type, Element propertyElement) {
    Object value = null;
    if (type == String.class) {
      value = propertyElement.getText();
    }
    else if ((type == Integer.class) || (type == int.class)) {
      value = new Integer(propertyElement.getTextTrim());
    }
    else if ((type == Long.class) || (type == long.class)) {
      value = new Long(propertyElement.getTextTrim());
    }
    else if ((type == Float.class) || (type == float.class)) {
      value = new Float(propertyElement.getTextTrim());
    }
    else if ((type == Double.class) || (type == double.class)) {
      value = new Double(propertyElement.getTextTrim());
    }
    else if ((type == Boolean.class) || (type == boolean.class)) {
      value = Boolean.valueOf(propertyElement.getTextTrim());
    }
    else if ((type == Character.class) || (type == char.class)) {
      value = new Character(propertyElement.getTextTrim().charAt(0));
    }
    else if ((type == Short.class) || (type == short.class)) {
      value = new Short(propertyElement.getTextTrim());
    }
    else if ((type == Byte.class) || (type == byte.class)) {
      value = new Byte(propertyElement.getTextTrim());
    }
    else if (type == List.class || type == Collection.class) {
      value = getCollection(propertyElement, new ArrayList<Object>());
    }
    else if (type == Queue.class) {
      value = getCollection(propertyElement, new LinkedList<Object>());
    }
    else if (type == Set.class) {
      value = getCollection(propertyElement, new HashSet<Object>());
    }
    else if (type == SortedSet.class) {
      value = getCollection(propertyElement, new TreeSet<Object>());
    }
    else if (type == Map.class) {
      value = getMap(propertyElement, new HashMap<Object, Object>());
    }
    else if (type == SortedMap.class) {
      value = getMap(propertyElement, new TreeMap<Object, Object>());
    }
    else if (Element.class.isAssignableFrom(type)) {
      value = propertyElement;
    }
    else {
      try {
        if (Collection.class.isAssignableFrom(type)) {
          value = getCollection(propertyElement, CollectionUtil.checkCollection(
              (Collection<?>) type.newInstance(), Object.class));
        }
        else if (Map.class.isAssignableFrom(type)) {
          value = getMap(propertyElement, CollectionUtil.checkMap((Map<?, ?>) type.newInstance(),
              Object.class, Object.class));
        }
        else {
          try {
            Constructor<?> constructor = type.getConstructor(String.class);
            if (propertyElement.isTextOnly()) {
              value = constructor.newInstance(propertyElement.getTextTrim());
            }
            else {
              log.error("element '" + propertyElement.getName() + "' has non-text content");
            }
          }
          catch (NoSuchMethodException e) {
            log.error(type + " does not have a string constructor", e);
          }
          catch (IllegalArgumentException e) {
            log.error(type + " cannot be constructed with value " + propertyElement.getTextTrim(),
                e);
          }
        }
      }
      catch (InstantiationException e) {
        log.error("could not instantiate " + type, e);
      }
      catch (IllegalAccessException e) {
        log.error(type + " is inaccessible", e);
      }
      catch (InvocationTargetException e) {
        log.error("constructor for " + type + " threw exception", e.getCause());
      }
    }
    return value;
  }

  private static Map<Object, Object> getMap(Element mapElement, Map<Object, Object> map) {
    Class<?> keyClass = classForAttributeValue(mapElement, "key-type");
    Class<?> valueClass = classForAttributeValue(mapElement, "value-type");

    for (Object o : mapElement.elements()) {
      Element element = (Element) o;
      Element keyElement = element.element("key");
      Element valueElement = element.element("value");

      map.put(getValue(keyClass, keyElement), getValue(valueClass, valueElement));
    }
    return map;
  }

  private static Collection<Object> getCollection(Element collectionElement,
      Collection<Object> collection) {
    Class<?> elementClass = classForAttributeValue(collectionElement, "element-type");

    for (Object element : collectionElement.elements()) {
      collection.add(getValue(elementClass, (Element) element));
    }
    return collection;
  }

  /**
   * Returns the <code>Class</code> associated with the value for the attribute with the given name.
   */
  private static Class<?> classForAttributeValue(Element element, String attributeName) {
    Class<?> type = String.class;
    String attributeValue = element.attributeValue(attributeName);
    if (attributeValue != null) {
      type = ClassLoaderUtil.classForName(attributeValue);
    }
    return type;
  }

  private static final Log log = LogFactory.getLog(FieldInstantiator.class);
}
