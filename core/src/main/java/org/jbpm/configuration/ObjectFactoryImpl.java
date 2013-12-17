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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.util.ClassLoaderUtil;

public class ObjectFactoryImpl implements ObjectFactory {

  private static final long serialVersionUID = 2L;

  private final Map namedObjectInfos;
  private final Map singletons = new HashMap();
  private transient final Map objects = new HashMap();
  private transient final Collection objectsUnderConstruction = new HashSet();
  private transient ClassLoader classLoader;

  public ObjectFactoryImpl() {
    namedObjectInfos = new HashMap();
  }

  ObjectFactoryImpl(Map namedObjectInfos) {
    this.namedObjectInfos = namedObjectInfos;
  }

  /** @deprecated creating objects by index is no longer supported */
  public ObjectFactoryImpl(Map namedObjectInfos, List objectInfos) {
    this.namedObjectInfos = namedObjectInfos;
  }

  public void addObjectInfo(ObjectInfo objectInfo) {
    if (!objectInfo.hasName()) {
      throw new ConfigurationException(objectInfo + " has no name");
    }

    String name = objectInfo.getName();
    if (log.isDebugEnabled()) log.debug("adding object info: " + name);
    synchronized (namedObjectInfos) {
      namedObjectInfos.put(name, objectInfo);
    }
  }

  private ObjectInfo getObjectInfo(String name) {
    synchronized (namedObjectInfos) {
      ObjectInfo objectInfo = (ObjectInfo) namedObjectInfos.get(name);
      if (objectInfo == null) {
        throw new ConfigurationException("no info for object '" + name + "'; defined objects: "
          + namedObjectInfos.keySet());
      }
      return objectInfo;
    }
  }

  /**
   * create a new object of the given name. Before creation starts, non-singleton objects will
   * be cleared from the registry. Singletons will remain.
   */
  public Object createObject(String name) {
    ObjectInfo objectInfo = getObjectInfo(name);
    return createObject(objectInfo);
  }

  public boolean hasObject(String name) {
    synchronized (namedObjectInfos) {
      return namedObjectInfos.containsKey(name);
    }
  }

  /**
   * create a new object for the given index. Before creation starts, non-singleton objects will
   * be cleared from the registry. Singletons will remain.
   * 
   * @deprecated creating objects by index is no longer supported
   */
  public Object createObject(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * create a new object for the given {@link ObjectInfo}. Before creation starts, non-singleton
   * objects will be cleared from the registry. Singletons will remain.
   */
  public Object createObject(ObjectInfo objectInfo) {
    synchronized (objects) {
      objects.clear();
      objectsUnderConstruction.clear();
      return getObject(objectInfo);
    }
  }

  /**
   * create an object of the given name. If the object was created before, that object is
   * returned from the registry.
   */
  Object getObject(String name) {
    ObjectInfo objectInfo = getObjectInfo(name);
    return getObject(objectInfo);
  }

  /**
   * create an object for the given {@link ObjectInfo}. If the object was created before, that
   * object is returned from the registry.
   */
  Object getObject(ObjectInfo objectInfo) {
    // use object name as registry key
    String registryKey = objectInfo.hasName() ? objectInfo.getName() : null;
    // if name is not specified, just create object without registering it
    if (registryKey == null) return objectInfo.createObject(this);

    // select appropriate registry based on singleton property
    Map registry = objectInfo.isSingleton() ? singletons : objects;
    // if object is already registered, use existing object
    if (registry.containsKey(registryKey)) return registry.get(registryKey);

    // prevent circular references
    if (objectsUnderConstruction.contains(registryKey)) {
      throw new ConfigurationException("circular reference to object '" + registryKey + "'");
    }

    objectsUnderConstruction.add(registryKey);
    try {
      // create and register object
      Object object = objectInfo.createObject(this);
      registry.put(registryKey, object);
      return object;
    }
    finally {
      objectsUnderConstruction.remove(registryKey);
    }
  }

  Class classForName(String className) throws ClassNotFoundException {
    if (classLoader == null) classLoader = ClassLoaderUtil.getClassLoader();
    return Class.forName(className, false, classLoader);
  }

  private static final Log log = LogFactory.getLog(ObjectFactoryImpl.class);
}
