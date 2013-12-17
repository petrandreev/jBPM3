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
package org.jbpm.graph.node;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.def.Node;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class NodeTypes {

  public static Set<String> getNodeTypes() {
    return nodeTypes.keySet();
  }
  
  public static Set<Class<? extends Node>> getNodeNames() {
    return nodeNames.keySet();
  }
  
  public static Class<? extends Node> getNodeType(String name) {
    return nodeTypes.get(name);
  }
  
  public static String getNodeName(Class<? extends Node> type) {
    return nodeNames.get(type);
  }
  
  private static final Log log = LogFactory.getLog(NodeTypes.class);

  static Map<String, Class<? extends Node>> nodeTypes = initialiseNodeTypes();
  static Map<Class<? extends Node>, String> nodeNames = createInverseMapping(nodeTypes);
  
  static Map<String, Class<? extends Node>> initialiseNodeTypes() {
    Map<String, Class<? extends Node>> types = new HashMap<String, Class<? extends Node>>();

    String resource = JbpmConfiguration.Configs.getString("resource.node.types");
    InputStream actionTypesStream = ClassLoaderUtil.getStream(resource);
    Element nodeTypesElement = XmlUtil.parseXmlInputStream(actionTypesStream).getDocumentElement();
    Iterator<?> nodeTypeIterator = XmlUtil.elementIterator(nodeTypesElement, "node-type");
    while(nodeTypeIterator.hasNext()) {
      Element nodeTypeElement = (Element) nodeTypeIterator.next();

      String elementTag = nodeTypeElement.getAttribute("element");
      String className = nodeTypeElement.getAttribute("class");
      try {
        Class<?> nodeClass = ClassLoaderUtil.classForName(className);
        types.put(elementTag, nodeClass.asSubclass(Node.class));
        
      } catch (Exception e) {
        if (!"org.jboss.seam.jbpm.Page".equals(className)) {
          log.debug("node '"+elementTag+"' will not be available. class '"+className+"' couldn't be loaded");
        }
      }
    }

    return types; 
  }

  public static <K, V> Map<V, K> createInverseMapping(Map<K, V> map) {
    Map<V, K> inverse = new HashMap<V, K>();
    for (Map.Entry<K, V> entry : map.entrySet()) {
      inverse.put(entry.getValue(), entry.getKey());      
    }
    return inverse;
  }
}
