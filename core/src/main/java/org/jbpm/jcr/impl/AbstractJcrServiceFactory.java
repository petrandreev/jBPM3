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
package org.jbpm.jcr.impl;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.jbpm.JbpmException;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

public abstract class AbstractJcrServiceFactory implements ServiceFactory {

  private static final long serialVersionUID = 1L;

  String username;
  String password;
  String workspace;

  public Service openService() {
    try {
      Repository jcrRepository = getRepository();
      Session session;
      if (username == null && workspace == null) {
        session = jcrRepository.login();
      }
      else if (username == null) {
        session = jcrRepository.login(workspace);
      }
      else if (workspace == null) {
        session = jcrRepository.login(getCredentials());
      }
      else {
        session = jcrRepository.login(getCredentials(), workspace);
      }
      return new JcrServiceImpl(session);
    }
    catch (Exception e) {
      // NOTE that Errors are not caught because that might halt the JVM
      // and mask the original Error
      throw new JbpmException("could not login to jcr repository as user " + username
        + " in workspace " + workspace, e);
    }
  }

  Credentials getCredentials() {
    char[] pwdChars = password != null ? password.toCharArray() : null;
    return new SimpleCredentials(username, pwdChars);
  }

  protected abstract Repository getRepository();

  public void close() {
  }

}
