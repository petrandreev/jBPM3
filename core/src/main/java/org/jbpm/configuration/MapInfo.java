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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class MapInfo extends AbstractObjectInfo {

  private static final long serialVersionUID = 1L;

  private final ObjectInfo[] keyInfos;
  private final ObjectInfo[] valueInfos;

  public MapInfo(Element mapElement, ObjectFactoryParser configParser) {
    super(mapElement, configParser);

    List entryElements = XmlUtil.elements(mapElement);
    keyInfos = new ObjectInfo[entryElements.size()];
    valueInfos = new ObjectInfo[entryElements.size()];
    for (int i = 0; i < entryElements.size(); i++) {
      Element entryElement = (Element) entryElements.get(i);
      Element keyElement = XmlUtil.element(entryElement, "key");
      Element valueElement = XmlUtil.element(entryElement, "value");
      keyInfos[i] = configParser.parse(XmlUtil.element(keyElement));
      valueInfos[i] = configParser.parse(XmlUtil.element(valueElement));
    }
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    Map map = new HashMap();
    if (keyInfos != null) {
      for (int i = 0; i < keyInfos.length; i++) {
        Object key = objectFactory.getObject(keyInfos[i]);
        Object value = objectFactory.getObject(valueInfos[i]);
        map.put(key, value);
      }
    }
    return map;
  }
}
