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

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;

/**
 * provides centralized classloader lookup.
 */
public class ClassLoaderUtil
{

  private ClassLoaderUtil()
  {
    // hide default constructor to prevent instantiation
  }

  /**
   * Bad usage of ClassLoader.loadClass() under JDK 6
   * https://jira.jboss.org/jira/browse/JBPM-1976
   * 
   * @deprecated Use ClassLoaderUtil.classForName
   */
  public static Class<?> loadClass(String className)
  {
    try
    {
      return getClassLoader().loadClass(className);
    }
    catch (ClassNotFoundException e)
    {
      throw new JbpmException("class not found '" + className + "'", e);
    }
  }

  public static Class<?> classForName(String className)
  {
    try
    {
      return Class.forName(className, false, getClassLoader());
    }
    catch (ClassNotFoundException e)
    {
      throw new JbpmException("class not found '" + className + "'", e);
    }
  }

  /**
   * returns the {@link ClassLoader} which is used in jbpm. Can be configured in jbpm.cfg.xml by the
   * property <code>jbpm.classLoader</code>.
   * <ul>
   * <li><code>jbpm</code> (default) uses the {@link ClassLoaderUtil#getClassLoader()}. This
   * was the only behavior available before <a
   * href="https://jira.jboss.org/jira/browse/JBPM-1148">JBPM-1148</a>.</li>
   * <li><code>context</code> uses the {@link Thread#getContextClassLoader()}.</li>
   * <li><code>custom</code> means that a ClassLoader class has to be provided in the property
   * <code>jbpm.classLoader.className</code></li>
   * </ul>
   */
  public static ClassLoader getClassLoader()
  {
    if (JbpmConfiguration.Configs.hasObject("jbpm.classLoader"))
    {
      String jbpmClassloader = JbpmConfiguration.Configs.getString("jbpm.classLoader");

      if (jbpmClassloader.equals("jbpm"))
      {
        return ClassLoaderUtil.class.getClassLoader();
      }
      else if (jbpmClassloader.equals("context"))
      {
        return Thread.currentThread().getContextClassLoader();
      }
      else if (jbpmClassloader.equals("custom"))
      {
        String classloaderClassname = null;
        try
        {
          if (!JbpmConfiguration.Configs.hasObject("jbpm.customClassLoader.className"))
          {
            throw new JbpmException(
                "'jbpm.classLoader' property set to 'custom' but 'jbpm.customClassLoader.className' is absent!");
          }
          classloaderClassname = JbpmConfiguration.Configs
              .getString("jbpm.customClassLoader.className");
          if (classloaderClassname == null)
          {
            throw new JbpmException(
                "'jbpm.classloader' property set to 'custom' but 'jbpm.customClassLoader.className' is null!");
          }

          Class<?> clazz = Class.forName(classloaderClassname, false, ClassLoaderUtil.class.getClassLoader());
          if (clazz == null) {
            clazz = Class.forName(classloaderClassname, false, Thread.currentThread().getContextClassLoader());
          }

          return (ClassLoader) clazz.newInstance();
        }
        catch (InstantiationException e)
        {
          throw new JbpmException("Error instantiating custom classloader "
              + classloaderClassname, e);
        }
        catch (IllegalAccessException e)
        {
          throw new JbpmException("Error accessing custom classloader " + classloaderClassname,
              e);
        }
        catch (ClassNotFoundException e)
        {
          throw new JbpmException("Custom classloader " + classloaderClassname + " not found ",
              e);
        }
      }
      else
      {
        throw new JbpmException("'jbpm.classloader' property set to '"
            + jbpmClassloader
            + "' but only the values 'jbpm'/'context'/'custom' are supported!");
      }
    }
    else
    {
      // default behavior like before https://jira.jboss.org/jira/browse/JBPM-1148
      return ClassLoaderUtil.class.getClassLoader();
    }
  }

  public static InputStream getStream(String resource)
  {
    return getClassLoader().getResourceAsStream(resource);
  }

  /*
   * Load jbpm configuration related resources as stream (normally jbpm.cfg.xml). This method first
   * tries to load the resource from the {@link ClassLoaderUtil} class loader, if not found it tries
   * the context class loader. If this doesn't return any ressource the call is delegated to the
   * class loader configured by calling getClassLoader(). This is a special method because the class
   * loader which has to be used for loading the jbpm.cfg.xml cannot be configured in the
   * jbpm.cfg.xml itself.
   */
  public static InputStream getJbpmConfigurationStream(String resource)
  {
    InputStream jbpmCfgStream = ClassLoaderUtil.class.getClassLoader()
        .getResourceAsStream(resource);
    if (jbpmCfgStream == null)
    {
      jbpmCfgStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }
    return jbpmCfgStream;
  }

  public static Properties getProperties(String resource)
  {
    Properties properties = new Properties();
    try
    {
      InputStream inStream = getStream(resource);
      properties.load(inStream);
      inStream.close();
    }
    catch (IOException e)
    {
      throw new JbpmException("couldn't load properties file '" + resource + "'", e);
    }
    return properties;
  }
}
