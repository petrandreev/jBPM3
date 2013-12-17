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
package org.jbpm.graph.action;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.XmlUtil;

public class ActionTypes {

  private static Map typesByResource = new HashMap();
  private static final Log log = LogFactory.getLog(ActionTypes.class);

  public static Collection getActionTypes() {
    return getTypes().values();
  }

  public static Set getActionNames() {
    return getTypes().keySet();
  }

  public static Class getActionType(String name) {
    return (Class) getTypes().get(name);
  }

  public static String getActionName(Class type) {
    for (Iterator iter = getTypes().entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      if (type == entry.getValue()) return (String) entry.getKey();
    }
    return null;
  }

  public static boolean hasActionName(String name) {
    return getTypes().containsKey(name);
  }

  private static Map getTypes() {
    String resource = Configs.getString("resource.action.types");
    synchronized (typesByResource) {
      Map types = (Map) typesByResource.get(resource);
      if (types == null) {
        types = initialiseActionTypes(resource);
        typesByResource.put(resource, types);
      }
      return types;
    }
  }

  private static Map initialiseActionTypes(String resource) {
    Map types = new HashMap();

    InputStream actionTypesStream = ClassLoaderUtil.getStream(resource);
    Element actionTypesElement = XmlUtil.parseXmlInputStream(actionTypesStream)
      .getDocumentElement();

    for (Iterator iter = XmlUtil.elementIterator(actionTypesElement, "action-type"); iter.hasNext();) {
      Element actionTypeElement = (Element) iter.next();
      String elementTag = actionTypeElement.getAttribute("element");

      String className = actionTypeElement.getAttribute("class");
      try {
        Class actionClass = ClassLoaderUtil.classForName(className);
        types.put(elementTag, actionClass);
      }
      catch (ClassNotFoundException e) {
        if (log.isDebugEnabled()) {
          log.debug("action '" + elementTag + "' will not be available, class not found: "
            + className);
        }
      }
    }
    return types;
  }
}
