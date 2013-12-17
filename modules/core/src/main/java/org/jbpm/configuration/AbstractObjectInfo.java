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

import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public abstract class AbstractObjectInfo implements ObjectInfo {

  private static final long serialVersionUID = 1L;
  
  String name = null;
  boolean isSingleton = false;

  public AbstractObjectInfo() {
  }

  public AbstractObjectInfo(Element element, ObjectFactoryParser objectFactoryParser) {
    if (element.hasAttribute("name")) {
      name = element.getAttribute("name");
      objectFactoryParser.addNamedObjectInfo(name, this);
    }
    if ("true".equalsIgnoreCase(element.getAttribute("singleton"))) {
      isSingleton = true;
    }
  }
  
  protected String getValueString(Element element) {
    String value = null;
    if (element.hasAttribute("value")) {
      value = element.getAttribute("value");
    } else {
      value = XmlUtil.getContentText(element);
    }
    return value;
  }

  public boolean hasName() {
    return (name!=null);
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public boolean isSingleton() {
    return isSingleton;
  }
  public void setSingleton(boolean isSingleton) {
    this.isSingleton = isSingleton;
  }
}
