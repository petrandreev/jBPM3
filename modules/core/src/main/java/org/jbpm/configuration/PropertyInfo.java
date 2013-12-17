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
import java.lang.reflect.Method;

import org.jbpm.JbpmException;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class PropertyInfo implements Serializable {

  private static final long serialVersionUID = 1L;
 
  String propertyName = null;
  String setterMethodName = null;
  ObjectInfo propertyValueInfo = null;

  public PropertyInfo(Element propertyElement, ObjectFactoryParser configParser) {
    // propertyName or setterMethodName
    if (propertyElement.hasAttribute("name")) {
      propertyName = propertyElement.getAttribute("name"); 
    } else if (propertyElement.hasAttribute("setter")) {
      setterMethodName = propertyElement.getAttribute("setter"); 
    } else {
      throw new JbpmException("property must have a 'name' or 'setter' attribute: "+XmlUtil.toString(propertyElement));
    }
    
    // propertyValueInfo
    Element propertyValueElement = XmlUtil.element(propertyElement);
    propertyValueInfo = configParser.parse(propertyValueElement);
  }

  public void injectProperty(Object object, ObjectFactoryImpl objectFactory) {
    Object propertyValue = objectFactory.getObject(propertyValueInfo);
    Method setterMethod = findSetter(object.getClass());
    setterMethod.setAccessible(true);
    try {
      setterMethod.invoke(object, new Object[]{propertyValue});
    } catch (Exception e) {
      throw new JbpmException("couldn't set property '"+propertyName+"' on class '"+object.getClass()+"' to value '"+propertyValue+"'", e);
    }
  }

  public Method findSetter(Class clazz) {
    Method method = null;

    if (setterMethodName==null) {
      if ( (propertyName.startsWith("is"))
           && (propertyName.length()>3) 
           && (Character.isUpperCase(propertyName.charAt(2)))
         ) {
        setterMethodName = "set"+propertyName.substring(2);
      } else {
        setterMethodName = "set"+propertyName.substring(0,1).toUpperCase()+propertyName.substring(1);
      }
    }
    
    Class candidateClass = clazz;
    while ( (candidateClass!=null)
            && (method==null)
          ) {

      Method[] methods = candidateClass.getDeclaredMethods();
      if (methods!=null) {
        for (int i=0; ( (i<methods.length) && (method==null) ); i++) {
          if ( (methods[i].getName().equals(setterMethodName))
               && (methods[i].getParameterTypes()!=null)
               && (methods[i].getParameterTypes().length==1)
             ) {
            method = methods[i];
          }
        }
      }

      if (method==null) {
        candidateClass = candidateClass.getSuperclass();
      }
    }

    if (method==null) {
      throw new JbpmException("couldn't find setter '"+setterMethodName+"' in class '"+clazz.getName()+"'");
    }
    
    return method;
  }
}
