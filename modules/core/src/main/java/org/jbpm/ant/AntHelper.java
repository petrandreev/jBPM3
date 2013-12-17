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
package org.jbpm.ant;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;

/**
 * common strategy for jbpm ant tasks to obtain a hibernate SessionFactory.
 */
public class AntHelper
{

  final static Map configurations = new HashMap();
  final static Map jbpmConfigurations = new HashMap();

  private AntHelper() {
    // prevent instantiation
  }

  public static Configuration getConfiguration(String hibernateCfgResource, String hibernatePropertiesResource)
  {
    Object key = getKey(hibernateCfgResource, hibernatePropertiesResource);
    Configuration configuration = (Configuration)configurations.get(key);
    if (configuration == null)
    {
      log.debug("creating hibernate configuration from cfg '" + hibernateCfgResource + "' and properties '" + hibernatePropertiesResource + "'");
      configuration = new Configuration();
      configuration.configure(hibernateCfgResource);
      if (hibernatePropertiesResource != null)
      {
        try
        {
          InputStream propertiesInputStream = AntHelper.class.getResourceAsStream(hibernatePropertiesResource);
          if (propertiesInputStream == null)
            throw new IllegalArgumentException("Cannot read properties: " + hibernatePropertiesResource);
          
          Properties properties = new Properties();
          properties.load(propertiesInputStream);
          configuration.setProperties(properties);
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
          throw new JbpmException("couldn't set properties '" + hibernatePropertiesResource + "'", ex);
        }
      }
      configurations.put(key, configuration);
    }
    else
    {
      log.debug("got hibernate configuration from cfg '" + hibernateCfgResource + "' and properties '" + hibernatePropertiesResource + "' from the cache");
    }
    return configuration;
  }

  public static JbpmConfiguration getJbpmConfiguration(String jbpmCfg)
  {
    JbpmConfiguration jbpmConfiguration = (JbpmConfiguration)jbpmConfigurations.get(jbpmCfg);
    if (jbpmConfiguration == null)
    {
      if (jbpmCfg == null)
      {
        jbpmConfiguration = JbpmConfiguration.getInstance();

      }
      else
      {
        jbpmConfiguration = JbpmConfiguration.getInstance(jbpmCfg);
      }

      jbpmConfigurations.put(jbpmCfg, jbpmConfiguration);
    }
    return jbpmConfiguration;
  }

  static Object getKey(String cfg, String properties)
  {
    List key = new ArrayList();
    key.add(cfg);
    key.add(properties);
    return key;
  }

  private static final Log log = LogFactory.getLog(AntHelper.class);
}
