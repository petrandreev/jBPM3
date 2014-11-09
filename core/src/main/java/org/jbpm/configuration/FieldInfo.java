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
import java.lang.reflect.Field;

import org.jbpm.JbpmException;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

@SuppressWarnings({
  "rawtypes"
})
public class FieldInfo implements Serializable {

  private final String fieldName;
  private final ObjectInfo fieldValueInfo;

  private static final long serialVersionUID = 1L;

  public FieldInfo(Element fieldElement, ObjectFactoryParser configParser) {
    // field name
    if (fieldElement.hasAttribute("name")) {
      fieldName = fieldElement.getAttribute("name");
    }
    else {
      throw new JbpmException("missing name or setter attribute in property");
    }

    // value info
    Element propertyValueElement = XmlUtil.element(fieldElement);
    fieldValueInfo = configParser.parse(propertyValueElement);
  }

  public void injectProperty(Object object, ObjectFactoryImpl objectFactory) {
    Object value = objectFactory.getObject(fieldValueInfo);
    Field field = findField(object.getClass());
    try {
      field.set(object, value);
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(getClass() + " has no access to " + field, e);
    }
  }

  private Field findField(Class type) {
    try {
      return type.getField(fieldName);
    }
    catch (NoSuchFieldException e) {
      for (Class ancestor = type; ancestor != Object.class; ancestor = ancestor.getSuperclass()) {
        try {
          Field field = ancestor.getDeclaredField(fieldName);
          field.setAccessible(true);
          return field;
        }
        catch (NoSuchFieldException e2) {
          // keep looking
        }
      }
      throw new JbpmException("missing field '" + fieldName + "' in " + type);
    }
  }
}
