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
import java.lang.reflect.Method;
import java.util.List;

import org.jbpm.JbpmException;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class ConstructorInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  BeanInfo beanInfo = null;
  String className = null;
  String factoryRefName = null;
  String factoryClassName = null;
  String factoryMethodName = null;
  String[] parameterClassNames = null;
  ObjectInfo[] parameterInfos = null;
  
  public ConstructorInfo(Element constructorElement, ObjectFactoryParser configParser) {
    // beanInfo is set by the beanInfo itself
    
    // className
    if (constructorElement.hasAttribute("class")) {
      className = constructorElement.getAttribute("class");
    }
    
    // factoryInfo
    if (constructorElement.hasAttribute("factory")) {
      factoryRefName = constructorElement.getAttribute("factory");
      if (!constructorElement.hasAttribute("method")) {
        throw new JbpmException("factory element in constructor requires method attribute in constructor: "+XmlUtil.toString(constructorElement));
      }
      factoryMethodName = constructorElement.getAttribute("method");

    } else if (constructorElement.hasAttribute("factory-class")) {
      factoryClassName = constructorElement.getAttribute("factory-class");
      if (!constructorElement.hasAttribute("method")) {
        throw new JbpmException("factory-class element in constructor requires method attribute in constructor: "+XmlUtil.toString(constructorElement));
      }
      factoryMethodName = constructorElement.getAttribute("method");

    } else {
      if (constructorElement.hasAttribute("method")) {
        throw new JbpmException("'method' element in constructor requires 'factory' of 'factory-class' attribute in constructor: "+XmlUtil.toString(constructorElement));
      }
    }

    // parameterTypesNames and parameterInfos 
    List parameterElements = XmlUtil.elements(constructorElement, "parameter");
    parameterClassNames = new String[parameterElements.size()];    
    parameterInfos = new ObjectInfo[parameterElements.size()];    
    for (int i=0; i<parameterElements.size(); i++) {
      Element parameterElement = (Element) parameterElements.get(i);
      if (!parameterElement.hasAttribute("class")) {
        throw new JbpmException("parameter element must have a class attribute: "+XmlUtil.toString(parameterElement));
      }
      parameterClassNames[i] = parameterElement.getAttribute("class");
      Element parameterInfoElement = XmlUtil.element(parameterElement);
      if (parameterInfoElement==null) {
        throw new JbpmException("parameter element must have exactly 1 child element: "+XmlUtil.toString(parameterElement));
      }
      parameterInfos[i] = configParser.parse(parameterInfoElement);
    }
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    Object newObject = null;

    Object[] args = getArgs(objectFactory);
    Class[] parameterTypes = getParameterTypes(objectFactory);

    if ( (factoryRefName!=null)
         || (factoryClassName!=null)
       ) {
      Object factory = null;
      Class factoryClass = null;

      if (factoryRefName!=null) {
        factory = objectFactory.getObject(factoryRefName);
        factoryClass = factory.getClass();
      } else {
        factoryClass = ClassLoaderUtil.classForName(factoryClassName);
      }

      try {
        Method factoryMethod = findMethod(factoryClass, parameterTypes);
        newObject = factoryMethod.invoke(factory, args);
      } catch (Exception e) {
        throw new JbpmException("couldn't create new bean with factory method '"+factoryClass.getName()+"."+factoryMethodName, e);
      }

    } else {
      String className = (this.className!=null ? this.className : beanInfo.getClassName());
      Class clazz = objectFactory.classForName(className);
      
      try {
        Constructor constructor = clazz.getDeclaredConstructor(parameterTypes);
        newObject = constructor.newInstance(args);
      } catch (Exception e) {
        throw new JbpmException("couldn't instantiate new '"+className+"' with constructor", e);
      }
      
    }
    
    return newObject;
  }

  protected Class[] getParameterTypes(ObjectFactoryImpl objectFactory) {
    int nbrOfParameters = (parameterClassNames!=null ? parameterClassNames.length : 0);
    Class[] parameterTypes = new Class[nbrOfParameters];
    for (int i=0; i<nbrOfParameters; i++) {
      parameterTypes[i] = objectFactory.classForName(parameterClassNames[i]);
    }
    return parameterTypes;
  }

  protected Object[] getArgs(ObjectFactoryImpl objectFactory) {
    int nbrOfParameters = (parameterClassNames!=null ? parameterClassNames.length : 0);
    Object[] args = new Object[nbrOfParameters];
    for (int i=0; i<nbrOfParameters; i++) {
      args[i] = objectFactory.getObject(parameterInfos[i]);
    }
    return args;
  }

  public Method findMethod(Class clazz, Class[] parameterTypes) {
    Method method = null;
    
    Class candidateClass = clazz;
    while ( (candidateClass!=null)
            && (method==null)
          ) {
      try {
        method = clazz.getDeclaredMethod(factoryMethodName, parameterTypes);
      } catch (NoSuchMethodException e1) {
        candidateClass = candidateClass.getSuperclass();
      }
    }

    if (method==null) {
      throw new JbpmException("couldn't find factory method '"+factoryMethodName+"' in class '"+clazz.getName()+"'");
    }
    
    return method;
  }
}
