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

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.jbpm.JbpmConfiguration.Configs;

public class JndiUtil {

  private JndiUtil() {
    // hide default constructor to prevent instantiation
  }

  public static Object lookup(String jndiName, Class type) {
    try {
      return PortableRemoteObject.narrow(lookup(jndiName), type);
    }
    catch (NamingException e) {
      throw new JndiLookupException("could not retrieve: " + jndiName, e);
    }
  }

  private static Object lookup(String jndiName) throws NamingException {
    Context initialContext;
    if (Configs.hasObject("resource.jndi.properties")) {
      String resource = Configs.getString("resource.jndi.properties");
      Properties properties = ClassLoaderUtil.getProperties(resource);
      initialContext = new InitialContext(properties);
    }
    else {
      initialContext = new InitialContext();
    }

    try {
      return initialContext.lookup(jndiName);
    }
    finally {
      initialContext.close();
    }
  }
}
