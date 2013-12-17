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

  private String name;
  private boolean singleton;

  public AbstractObjectInfo() {
  }

  public AbstractObjectInfo(Element element, ObjectFactoryParser objectFactoryParser) {
    if (element.hasAttribute("name")) {
      name = element.getAttribute("name");
    }
    if (element.hasAttribute("singleton")) {
      singleton = Boolean.valueOf(element.getAttribute("singleton")).booleanValue();
    }
  }

  protected String getValueString(Element element) {
    return element.hasAttribute("value") ? element.getAttribute("value")
      : XmlUtil.getContentText(element);
  }

  public boolean hasName() {
    return name != null;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isSingleton() {
    return singleton;
  }

  public void setSingleton(boolean singleton) {
    this.singleton = singleton;
  }
}
