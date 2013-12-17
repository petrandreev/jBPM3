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
package org.jbpm.jcr.jndi;

import javax.jcr.Repository;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.jbpm.JbpmException;
import org.jbpm.jcr.impl.AbstractJcrServiceFactory;

public class JndiJcrServiceFactory extends AbstractJcrServiceFactory {

  private static final long serialVersionUID = 1L;
  
  String jndiName = null;

  protected Repository getRepository() {
    Repository jcrRepository;
    try {
      InitialContext initialContext = new InitialContext();
      Object o = initialContext.lookup(jndiName);
      jcrRepository = (Repository) PortableRemoteObject.narrow(o, Repository.class);
    } catch (Exception e) {
      // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
      throw new JbpmException("couldn't fetch JCR repository from JNDI with name '"+jndiName+"'", e);
    }
    return jcrRepository;
  }

}
