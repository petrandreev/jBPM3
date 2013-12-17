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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmException;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class BeanInfo extends AbstractObjectInfo {
  
  private static final long serialVersionUID = 1L;

  String className = null;
  ConstructorInfo constructorInfo = null;
  PropertyInfo[] propertyInfos = null;

  public BeanInfo() {
  }

  public BeanInfo(Element beanElement, ObjectFactoryParser objectFactoryParser) {
    super(beanElement, objectFactoryParser);
    
    // parse constructor or factory
    Element constructorElement = XmlUtil.element(beanElement, "constructor");
    if (constructorElement!=null) {
      constructorInfo = new ConstructorInfo(constructorElement, objectFactoryParser);
      constructorInfo.beanInfo = this;
    }
    
    if (beanElement.hasAttribute("class")) {
      className = beanElement.getAttribute("class");
    } else if ( (constructorInfo.factoryRefName==null)
                && (constructorInfo.factoryClassName==null )
              ) {
      throw new JbpmException("bean element must have a class attribute: "+XmlUtil.toString(beanElement));
    }

    // parse fields
    List propertyInfoList = new ArrayList();
    Iterator iter = XmlUtil.elementIterator(beanElement, "field");
    while (iter.hasNext()) {
      Element fieldElement = (Element) iter.next();
      propertyInfoList.add(new FieldInfo(fieldElement, objectFactoryParser));
    }
    
    // parse properties
    iter = XmlUtil.elementIterator(beanElement, "property");
    while (iter.hasNext()) {
      Element propertyElement = (Element) iter.next();
      propertyInfoList.add(new PropertyInfo(propertyElement, objectFactoryParser));
    }
    
    propertyInfos = (PropertyInfo[]) propertyInfoList.toArray(new PropertyInfo[propertyInfoList.size()]);
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    Object object = null;
    
    if (constructorInfo==null) {
      if (className==null) throw new JbpmException("bean '"+getName()+"' doesn't have a class or constructor specified");
      try {
        Class clazz = objectFactory.classForName(className);
        object = clazz.newInstance();
      } catch (Exception e) {
        throw new JbpmException("couldn't instantiate bean '"+getName()+"' of type '"+className+"'", e);
      }
    } else {
      object = constructorInfo.createObject(objectFactory);
    }
    
    if (className==null) className = object.getClass().getName();

    if (propertyInfos!=null) {
      for (int i=0; i<propertyInfos.length; i++) {
        propertyInfos[i].injectProperty(object, objectFactory);
      }
    }
    
    return object;
  }

  public String getClassName() {
    return className;
  }
  public void setClassName(String className) {
    this.className = className;
  }
}
