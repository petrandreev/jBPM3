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

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmException;
import org.w3c.dom.Element;

public class FieldInfo extends PropertyInfo {

  private static final long serialVersionUID = 1L;

  public FieldInfo(Element fieldElement, ObjectFactoryParser configParser) {
    super(fieldElement, configParser);
  }

  public void injectProperty(Object object, ObjectFactoryImpl objectFactory) {

    Object propertyValue = objectFactory.getObject(propertyValueInfo);
    Field propertyField = findField(object.getClass());
    propertyField.setAccessible(true);
    try {
      propertyField.set(object, propertyValue);
    } catch (Exception e) {
      throw new JbpmException("couldn't set field '"+propertyName+"' on class '"+object.getClass()+"' to value '"+propertyValue+"'", e);
    }
  }

  Field findField(Class clazz) {
    Field field = null;
    
    Class candidateClass = clazz;
    while ( (candidateClass!=null)
            && (field==null)
          ) {

      try {
        field = candidateClass.getDeclaredField(propertyName);
      } catch (Exception e) {
        candidateClass = candidateClass.getSuperclass();
      }
    }

    if (field==null) {
      JbpmException e = new JbpmException("couldn't find field '"+propertyName+"' in class '"+clazz.getName()+"'");
      log.error(e);
      throw e;
    }
    
    return field;
  }

  private static Log log = LogFactory.getLog(FieldInfo.class);
}
