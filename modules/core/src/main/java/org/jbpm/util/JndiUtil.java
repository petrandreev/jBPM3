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

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JndiUtil {

  private JndiUtil() {
    // hide default constructor to prevent instantiation
  }

  public static Object lookup(String jndiName, Class type) {
    Object object = null;
    try {
      InitialContext initialContext = new InitialContext();
      object = initialContext.lookup(jndiName);
      object = PortableRemoteObject.narrow(object, type);
      // fetch from JNDI
      log.debug("fetched '"+object+"' from JNDI location '"+jndiName+"'");
    } catch (Exception e) {
      throw new JndiLookupException("couldn't fetch '"+jndiName+"' from jndi", e);
    }
    return object;
  }
  
  private static Log log = LogFactory.getLog(JndiUtil.class);
}
