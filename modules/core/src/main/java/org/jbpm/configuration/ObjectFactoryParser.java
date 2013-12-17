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

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmException;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class ObjectFactoryParser implements Serializable {

  private static final long serialVersionUID = 1L;
  
  static Map defaultMappings = null;
  public static Map getDefaultMappings() {
    if (defaultMappings==null) {
      defaultMappings = new HashMap();
      addMapping(defaultMappings, "bean",         BeanInfo.class);
      addMapping(defaultMappings, "ref",          RefInfo.class);
      addMapping(defaultMappings, "list",         ListInfo.class);
      addMapping(defaultMappings, "map",          MapInfo.class);
      addMapping(defaultMappings, "string",       StringInfo.class);
      addMapping(defaultMappings, "int",          IntegerInfo.class);
      addMapping(defaultMappings, "integer",      IntegerInfo.class);
      addMapping(defaultMappings, "long",         LongInfo.class);
      addMapping(defaultMappings, "float",        FloatInfo.class);
      addMapping(defaultMappings, "double",       DoubleInfo.class);
      addMapping(defaultMappings, "char",         CharacterInfo.class);
      addMapping(defaultMappings, "character",    CharacterInfo.class);
      addMapping(defaultMappings, "boolean",      BooleanInfo.class);
      addMapping(defaultMappings, "true",         BooleanInfo.class);
      addMapping(defaultMappings, "false",        BooleanInfo.class);
      addMapping(defaultMappings, "null",         NullInfo.class);
      addMapping(defaultMappings, "jbpm-context", JbpmContextInfo.class);
      addMapping(defaultMappings, "jbpm-type",    JbpmTypeObjectInfo.class);
    }
    return defaultMappings;
  }

  static final Class[] constructorParameterTypes = new Class[]{Element.class, ObjectFactoryParser.class};
  static void addMapping(Map mappings, String elementTagName, Class objectInfoClass) {
    try {
      Constructor constructor = objectInfoClass.getDeclaredConstructor(constructorParameterTypes);
      mappings.put(elementTagName, constructor);
    } catch (Exception e) {
      throw new JbpmException("couldn't add mapping for element '"+elementTagName+"': constructor("+Element.class.getName()+","+ObjectFactoryParser.class.getName()+") was missing for class '"+objectInfoClass.getName()+"'", e);
    }
  }
  
  public static ObjectFactoryImpl parseXmlString(String xml) {
    Element rootElement = XmlUtil.parseXmlText(xml).getDocumentElement();
    return createObjectFactory(rootElement);
  }

  public static ObjectFactoryImpl parseInputStream(InputStream xmlInputStream) {
    Element rootElement = XmlUtil.parseXmlInputStream(xmlInputStream).getDocumentElement();
    return createObjectFactory(rootElement);
  }

  public static ObjectFactoryImpl parseResource(String resource) {
    Element rootElement = XmlUtil.parseXmlResource(resource, true).getDocumentElement();
    return createObjectFactory(rootElement);
  }
  
  public static ObjectFactoryImpl createObjectFactory(Element rootElement) {
    ObjectFactoryParser objectFactoryParser = new ObjectFactoryParser();
    List objectInfos = new ArrayList();
    List topLevelElements = XmlUtil.elements(rootElement);
    for (int i = 0; i<topLevelElements.size(); i++) {
      Element topLevelElement = (Element) topLevelElements.get(i);
      ObjectInfo objectInfo = objectFactoryParser.parse(topLevelElement);
      objectInfos.add(objectInfo);
    }
    return new ObjectFactoryImpl(objectFactoryParser.namedObjectInfos, objectInfos);
  }

  public void parseElementsFromResource(String resource, ObjectFactoryImpl objectFactoryImpl) {
    Element rootElement = XmlUtil.parseXmlResource(resource, true).getDocumentElement();
    parseElements(rootElement, objectFactoryImpl);
  }

  public void parseElementsStream(InputStream inputStream, ObjectFactoryImpl objectFactoryImpl) {
    Element rootElement = XmlUtil.parseXmlInputStream(inputStream).getDocumentElement();
    parseElements(rootElement, objectFactoryImpl);
  }

  public void parseElements(Element element, ObjectFactoryImpl objectFactoryImpl) {
    List objectInfoElements = XmlUtil.elements(element);
    for (int i = 0; i<objectInfoElements.size(); i++) {
      Element objectInfoElement = (Element) objectInfoElements.get(i);
      ObjectInfo objectInfo = parse(objectInfoElement);
      objectFactoryImpl.addObjectInfo(objectInfo);
    }
  }

  Map mappings = null;
  Map namedObjectInfos = null;

  public ObjectFactoryParser() {
    this(getDefaultMappings());
  }

  public ObjectFactoryParser(Map mappings) {
    this.mappings = mappings;
    this.namedObjectInfos = new HashMap();
  }

  public ObjectInfo parse(Element element) {
    ObjectInfo objectInfo = null;
    String elementTagName = element.getTagName().toLowerCase();
    Constructor constructor = (Constructor) mappings.get(elementTagName);
    if (constructor==null) {
      throw new JbpmException("no ObjectInfo class specified for element '"+elementTagName+"'");
    }
    try {
      objectInfo = (ObjectInfo) constructor.newInstance(new Object[]{element,this});
    } catch (Exception e) {
      throw new JbpmException("couldn't parse '"+elementTagName+"' into a '"+constructor.getDeclaringClass().getName()+"': "+XmlUtil.toString(element), e);
    }
    return objectInfo;
  }

  public void addNamedObjectInfo(String name, ObjectInfo objectInfo) {
    namedObjectInfos.put(name, objectInfo);
  }
  
  public void addMapping(String elementName, Class objectInfoClass) {
    if (mappings==getDefaultMappings()) {
      mappings = new HashMap(getDefaultMappings());
    }
    addMapping(mappings, elementName, objectInfoClass);
  }
}
