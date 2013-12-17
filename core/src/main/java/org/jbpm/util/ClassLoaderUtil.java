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
package org.jbpm.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmException;
import org.jbpm.configuration.ObjectFactory;

/**
 * centralized class loader access.
 */
public class ClassLoaderUtil {

  private ClassLoaderUtil() {
    // hide default constructor to prevent instantiation
  }

  /**
   * Bad usage of ClassLoader.loadClass() under JDK 6.
   * 
   * @deprecated Use {@linkplain #classForName(String)} instead
   * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1976">JBPM-1976</a>
   */
  public static Class loadClass(String className) {
    try {
      return getClassLoader().loadClass(className);
    }
    catch (ClassNotFoundException e) {
      throw new JbpmException("class not found: " + className, e);
    }
  }

  public static Class classForName(String className) throws ClassNotFoundException {
    return Class.forName(className, false, getClassLoader());
  }

  public static Class classForName(String className, boolean useConfiguredLoader)
    throws ClassNotFoundException {
    if (useConfiguredLoader) return classForName(className);

    // try context class loader first, so that applications can override provided classes
    try {
      return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
    }
    catch (ClassNotFoundException e) {
      // fall back on the loader of this class
      return Class.forName(className, false, ClassLoaderUtil.class.getClassLoader());
    }
  }

  /**
   * Returns the {@link ClassLoader} employed by jBPM to load classes referenced in the
   * configuration. The class loader can be changed in <code>jbpm.cfg.xml</code> by setting the
   * string property <code>jbpm.class.loader</code>. The possible values are:
   * <ul>
   * <li><code>jbpm</code> (default) indicates the class loader of the jBPM classes. Before <a
   * href="https://jira.jboss.org/jira/browse/JBPM-1148">JBPM-1148</a> no other behavior was
   * available</li>
   * <li><code>context</code> indicates the {@linkplain Thread#getContextClassLoader() context
   * class loader}</li>
   * <li>any other value is interpreted as a reference to a class loader bean described in the
   * configuration</li>
   * </ul>
   */
  public static ClassLoader getClassLoader() {
    ObjectFactory objectFactory = Configs.getObjectFactory();
    if (objectFactory.hasObject("jbpm.class.loader")) {
      String classLoader = (String) objectFactory.createObject("jbpm.class.loader");

      if ("jbpm".equals(classLoader)) {
        // use class loader that loaded the jbpm classes
        return ClassLoaderUtil.class.getClassLoader();
      }

      if (classLoader.equals("context")) {
        // use the context class loader
        return Thread.currentThread().getContextClassLoader();
      }

      // interpret value as a reference to a class loader bean
      return (ClassLoader) objectFactory.createObject(classLoader);
    }
    else {
      // behave like before JBPM-1148
      return ClassLoaderUtil.class.getClassLoader();
    }
  }

  public static InputStream getStream(String resource) {
    return getClassLoader().getResourceAsStream(resource);
  }

  /**
   * Returns a stream for reading the specified resource. This method helps bootstrap jBPM
   * because the class loader used for locating the configuration resource cannot be configured
   * in the configuration itself.
   * 
   * @param useConfiguredLoader if <code>true</code>, this method searches for the resource in
   * the context class loader, if not found it falls back on the loader of this class
   * @return a stream for reading the resource, or <code>null</code> if the resource was not
   * found
   */
  public static InputStream getStream(String resource, boolean useConfiguredLoader) {
    if (useConfiguredLoader) return getStream(resource);

    // try context class loader first, allowing applications to override built-in resources
    InputStream stream = Thread.currentThread()
      .getContextClassLoader()
      .getResourceAsStream(resource);
    if (stream == null) {
      // fall back on the loader of this class
      stream = ClassLoaderUtil.class.getClassLoader().getResourceAsStream(resource);
    }
    return stream;
  }

  public static Properties getProperties(String resource) {
    InputStream inStream = getStream(resource);
    if (inStream == null) throw new JbpmException("resource not found: " + resource);
    try {
      Properties properties = new Properties();
      properties.load(inStream);
      return properties;
    }
    catch (IOException e) {
      throw new JbpmException("could not load properties from resource: " + resource, e);
    }
    finally {
      try {
        inStream.close();
      }
      catch (IOException e) {
        Log log = LogFactory.getLog(ClassLoaderUtil.class);
        log.warn("failed to close resource: " + resource, e);
      }
    }
  }
}
