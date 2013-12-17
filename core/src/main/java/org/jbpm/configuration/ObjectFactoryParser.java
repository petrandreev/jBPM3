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
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jbpm.JbpmException;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ObjectFactoryParser implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final Class[] constructorParameterTypes = { Element.class,
    ObjectFactoryParser.class };

  private static final Map defaultMappings = getDefaultMappings();

  public static Map getDefaultMappings() {
    Map mappings = new HashMap();
    addMapping(mappings, "bean", BeanInfo.class);
    addMapping(mappings, "ref", RefInfo.class);
    addMapping(mappings, "list", ListInfo.class);
    addMapping(mappings, "set", SetInfo.class);
    addMapping(mappings, "map", MapInfo.class);
    addMapping(mappings, "string", StringInfo.class);
    addMapping(mappings, "int", IntegerInfo.class);
    addMapping(mappings, "integer", IntegerInfo.class);
    addMapping(mappings, "long", LongInfo.class);
    addMapping(mappings, "float", FloatInfo.class);
    addMapping(mappings, "double", DoubleInfo.class);
    addMapping(mappings, "char", CharacterInfo.class);
    addMapping(mappings, "character", CharacterInfo.class);
    addMapping(mappings, "boolean", BooleanInfo.class);
    addMapping(mappings, "true", BooleanInfo.class);
    addMapping(mappings, "false", BooleanInfo.class);
    addMapping(mappings, "null", NullInfo.class);
    addMapping(mappings, "jbpm-context", JbpmContextInfo.class);
    addMapping(mappings, "jbpm-type", JbpmTypeObjectInfo.class);
    return mappings;
  }

  private static void addMapping(Map mappings, String elementTagName, Class objectInfoClass) {
    try {
      Constructor constructor = objectInfoClass.getDeclaredConstructor(constructorParameterTypes);
      mappings.put(elementTagName, constructor);
    }
    catch (NoSuchMethodException e) {
      throw new JbpmException("could not add mapping for element '" + elementTagName
        + "', constructor(" + Element.class.getName() + ","
        + ObjectFactoryParser.class.getName() + ") missing from " + objectInfoClass, e);
    }
  }

  public static ObjectFactoryImpl parseXmlString(String xml) {
    return createObjectFactory(XmlUtil.parseXmlText(xml));
  }

  public static ObjectFactoryImpl parseInputStream(InputStream inputStream) {
    return createObjectFactory(XmlUtil.parseXmlInputStream(inputStream));
  }

  public static ObjectFactoryImpl parseResource(String resource) {
    return createObjectFactory(XmlUtil.parseXmlResource(resource, false));
  }

  private static ObjectFactoryImpl createObjectFactory(Document document) {
    return createObjectFactory(document.getDocumentElement());
  }

  public static ObjectFactoryImpl createObjectFactory(Element infosElement) {
    ObjectFactoryParser parser = new ObjectFactoryParser();
    for (Iterator iter = XmlUtil.elementIterator(infosElement); iter.hasNext();) {
      Element infoElement = (Element) iter.next();
      parser.parse(infoElement);
    }
    return new ObjectFactoryImpl(parser.namedObjectInfos);
  }

  public void parseElementsFromResource(String resource, ObjectFactoryImpl objectFactory) {
    parseElements(XmlUtil.parseXmlResource(resource, false), objectFactory);
  }

  public void parseElementsStream(InputStream inputStream, ObjectFactoryImpl objectFactory) {
    parseElements(XmlUtil.parseXmlInputStream(inputStream), objectFactory);
  }

  private void parseElements(Document document, ObjectFactoryImpl objectFactory) {
    parseElements(document.getDocumentElement(), objectFactory);
  }

  public void parseElements(Element infosElement, ObjectFactoryImpl objectFactory) {
    for (Iterator iter = XmlUtil.elementIterator(infosElement); iter.hasNext();) {
      Element infoElement = (Element) iter.next();
      ObjectInfo objectInfo = parse(infoElement);
      objectFactory.addObjectInfo(objectInfo);
    }
  }

  private Map mappings;
  private final Map namedObjectInfos = new HashMap();

  public ObjectFactoryParser() {
    this(getDefaultMappings());
  }

  public ObjectFactoryParser(Map mappings) {
    this.mappings = mappings;
  }

  public ObjectInfo parse(Element element) {
    String tagName = element.getTagName().toLowerCase();
    Constructor constructor = (Constructor) mappings.get(tagName);
    if (constructor == null) {
      throw new JbpmException("no ObjectInfo class specified for element: " + tagName);
    }
    try {
      ObjectInfo objectInfo = (ObjectInfo) constructor.newInstance(new Object[] { element, this });
      if (objectInfo.hasName()) addNamedObjectInfo(objectInfo.getName(), objectInfo);
      return objectInfo;
    }
    catch (InstantiationException e) {
      throw new JbpmException("failed to instantiate " + constructor.getDeclaringClass(), e);
    }
    catch (IllegalAccessException e) {
      throw new JbpmException(getClass() + " has no access to " + constructor, e);
    }
    catch (InvocationTargetException e) {
      throw new JbpmException(constructor + " threw exception", e);
    }
  }

  public void addNamedObjectInfo(String name, ObjectInfo objectInfo) {
    namedObjectInfos.put(name, objectInfo);
  }

  public void addMapping(String elementName, Class objectInfoClass) {
    if (mappings == defaultMappings) mappings = new HashMap(defaultMappings);
    addMapping(mappings, elementName, objectInfoClass);
  }
}
