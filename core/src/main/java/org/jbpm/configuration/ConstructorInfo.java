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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.w3c.dom.Element;

import org.jbpm.JbpmException;
import org.jbpm.util.ArrayUtil;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.XmlUtil;

public class ConstructorInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String className;
  private String factoryRefName;
  private String factoryClassName;
  private String factoryMethodName;
  private final String[] parameterClassNames;
  private final ObjectInfo[] parameterInfos;

  public ConstructorInfo(Element constructorElement, ObjectFactoryParser configParser) {
    // factory
    if (constructorElement.hasAttribute("factory")) {
      factoryRefName = constructorElement.getAttribute("factory");
      if (!constructorElement.hasAttribute("method")) {
        throw new JbpmException("missing method attribute in constructor");
      }
      factoryMethodName = constructorElement.getAttribute("method");
    }
    // factory-class
    else if (constructorElement.hasAttribute("factory-class")) {
      factoryClassName = constructorElement.getAttribute("factory-class");
      if (!constructorElement.hasAttribute("method")) {
        throw new JbpmException("missing method attribute in constructor");
      }
      factoryMethodName = constructorElement.getAttribute("method");
    }
    else {
      if (constructorElement.hasAttribute("method")) {
        throw new JbpmException("missing factory or factory-class attribute in constructor");
      }
      // class
      if (constructorElement.hasAttribute("class")) {
        className = constructorElement.getAttribute("class");
      }
      else {
        throw new JbpmException("missing class, factory or factory-class attribute in constructor");
      }
    }

    // parameterTypesNames and parameterInfos
    List parameterElements = XmlUtil.elements(constructorElement, "parameter");
    parameterClassNames = new String[parameterElements.size()];
    parameterInfos = new ObjectInfo[parameterElements.size()];
    for (int i = 0; i < parameterElements.size(); i++) {
      Element parameterElement = (Element) parameterElements.get(i);
      if (!parameterElement.hasAttribute("class")) {
        throw new JbpmException("missing class attribute in constructor parameter");
      }
      parameterClassNames[i] = parameterElement.getAttribute("class");

      Element parameterInfoElement = XmlUtil.element(parameterElement);
      if (parameterInfoElement == null) {
        throw new JbpmException("missing subelement in constructor parameter");
      }
      parameterInfos[i] = configParser.parse(parameterInfoElement);
    }
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    Object[] args = getArgs(objectFactory);
    Class[] parameterTypes = getParameterTypes(objectFactory);

    if (factoryRefName != null || factoryClassName != null) {
      Object factory;
      Class factoryClass;

      if (factoryRefName != null) {
        factory = objectFactory.getObject(factoryRefName);
        factoryClass = factory.getClass();
      }
      else {
        factory = null;
        try {
          factoryClass = ClassLoaderUtil.classForName(factoryClassName);
        }
        catch (ClassNotFoundException e) {
          throw new JbpmException("factory class not found: " + factoryClassName, e);
        }
      }

      Method factoryMethod = findMethod(factoryClass, parameterTypes);
      try {
        return factoryMethod.invoke(factory, args);
      }
      catch (IllegalAccessException e) {
        throw new JbpmException(getClass() + " has no access to " + factoryMethod, e);
      }
      catch (InvocationTargetException e) {
        throw new JbpmException(factoryMethod + " threw exception", e.getCause());
      }
    }
    else {
      try {
        Class clazz = objectFactory.classForName(className);
        Constructor constructor = clazz.getDeclaredConstructor(parameterTypes);
        try {
          return constructor.newInstance(args);
        }
        catch (InstantiationException e) {
          throw new JbpmException("failed to instantiate " + clazz, e);
        }
        catch (IllegalAccessException e) {
          throw new JbpmException(getClass() + " has no access to " + constructor, e);
        }
        catch (InvocationTargetException e) {
          throw new JbpmException(constructor + " threw exception", e.getCause());
        }
      }
      catch (ClassNotFoundException e) {
        throw new JbpmException("class not found: " + className, e);
      }
      catch (NoSuchMethodException e) {
        throw new JbpmException("constructor not found: " + className
          + ArrayUtil.toString(parameterTypes), e);
      }
    }
  }

  protected Class[] getParameterTypes(ObjectFactoryImpl objectFactory) {
    int nbrOfParameters = parameterClassNames != null ? parameterClassNames.length : 0;
    Class[] parameterTypes = new Class[nbrOfParameters];
    for (int i = 0; i < nbrOfParameters; i++) {
      String parameterClassName = parameterClassNames[i];
      try {
        parameterTypes[i] = objectFactory.classForName(parameterClassName);
      }
      catch (ClassNotFoundException e) {
        throw new JbpmException("class not found: " + parameterClassName, e);
      }
    }
    return parameterTypes;
  }

  protected Object[] getArgs(ObjectFactoryImpl objectFactory) {
    int nbrOfParameters = (parameterClassNames != null ? parameterClassNames.length : 0);
    Object[] args = new Object[nbrOfParameters];
    for (int i = 0; i < nbrOfParameters; i++) {
      args[i] = objectFactory.getObject(parameterInfos[i]);
    }
    return args;
  }

  public Method findMethod(Class clazz, Class[] parameterTypes) {
    try {
      // look for public method
      return clazz.getMethod(factoryMethodName, parameterTypes);
    }
    catch (NoSuchMethodException e) {
      // look for any method up the class hierarchy
      do {
        try {
          return clazz.getDeclaredMethod(factoryMethodName, parameterTypes);
        }
        catch (NoSuchMethodException e2) {
          clazz = clazz.getSuperclass();
        }
      } while (clazz != null);
      // give up
      throw new JbpmException("no such method: " + factoryMethodName, e);
    }
  }
}
