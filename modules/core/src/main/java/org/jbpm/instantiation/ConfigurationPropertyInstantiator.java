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
package org.jbpm.instantiation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigurationPropertyInstantiator implements Instantiator {

  private static final Class<?>[] parameterTypes = { String.class };

  public <T> T instantiate(Class<T> type, String configuration) {
    // create a new instance with the default constructor
    T instance = InstantiatorUtil.instantiate(type);

    try {
      // set the configuration with the bean-style setter
      Method setter = type.getDeclaredMethod("setConfiguration", parameterTypes);
      setter.setAccessible(true);
      setter.invoke(instance, configuration);
    }
    catch (NoSuchMethodException e) {
      log.error("configuration setter does not exist", e);
    }
    catch (IllegalAccessException e) {
      log.error("configuration setter is inaccesible", e);
    }
    catch (IllegalArgumentException e) {
      log.error("configuration cannot be set to value " + configuration, e);
    }
    catch (InvocationTargetException e) {
      log.error("configuration setter threw exception", e.getCause());
    }

    return instance;
  }

  private static final Log log = LogFactory.getLog(ConfigurationPropertyInstantiator.class);
}
