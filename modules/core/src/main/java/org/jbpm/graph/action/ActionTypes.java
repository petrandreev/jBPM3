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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.node.NodeTypes;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;


public class ActionTypes {

  public static Set<Class<? extends Action>> getActionTypes() {
    return actionNames.keySet();
  }
  
  public static Set<String> getActionNames() {
    return actionTypes.keySet();
  }
  
  public static Class<? extends Action> getActionType(String name) {
    return actionTypes.get(name);
  }
  
  public static String getActionName(Class<? extends Action> type) {
    return actionNames.get(type);
  }

  public static boolean hasActionName(String name) {
    return actionTypes.containsKey(name);
  }

  static final Log log = LogFactory.getLog(ActionTypes.class);
  static Map<String, Class<? extends Action>> actionTypes = initialiseActionTypes();
  static Map<Class<? extends Action>, String> actionNames = NodeTypes.createInverseMapping(actionTypes);
  
  static Map<String, Class<? extends Action>> initialiseActionTypes() {
    Map<String, Class<? extends Action>> types = new HashMap<String, Class<? extends Action >>();
    
    String resource = JbpmConfiguration.Configs.getString("resource.action.types");
    InputStream actionTypesStream = ClassLoaderUtil.getStream(resource);
    Element actionTypesElement = XmlUtil.parseXmlInputStream(actionTypesStream).getDocumentElement();
    Iterator<?> actionTypeIterator = XmlUtil.elementIterator(actionTypesElement, "action-type");
    while(actionTypeIterator.hasNext()) {
      Element actionTypeElement = (Element) actionTypeIterator.next();

      String elementTag = actionTypeElement.getAttribute("element");
      String className = actionTypeElement.getAttribute("class");
      try {
        Class<?> actionClass = ClassLoaderUtil.classForName(className);
        types.put(elementTag, actionClass.asSubclass(Action.class));
        
      } catch (Exception e) {
        // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
        log.debug("action '"+elementTag+"' will not be available. class '"+className+"' couldn't be loaded");
      }
    }
    return types; 
  }
}
