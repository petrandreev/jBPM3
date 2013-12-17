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
package org.jbpm.context.exe.variableinstance;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.jcr.JcrService;
import org.jbpm.svc.Services;

public class JcrNodeInstance extends VariableInstance {

  private static final long serialVersionUID = 1L;

  private String repository;
  private String workspace;
  private String path;

  public boolean isStorable(Object value) {
    return value instanceof Node || value == null;
  }

  protected Object getObject() {
    if (path == null) return null;

    JcrService jcrService = findService();
    if (jcrService == null) {
      throw new JbpmException("could not find service for JCR repository '" + repository
        + "' and workspace '" + workspace + "'");
    }
    try {
      return jcrService.getSession().getItem(path);
    }
    catch (PathNotFoundException e) {
      throw new JbpmException("failed to get item from path: " + path, e);
    }
    catch (RepositoryException e) {
      throw new JbpmException("failed to get item from path: " + path, e);
    }
  }

  /**
   * find the service that matches the stored repository and workspace. The search is done as
   * follows:
   * <ol>
   * <li>a service whose name is <code>jcr</code></li>
   * <li>a service whose name equals the repository name</li>
   * <li>a service whose name starts with the repository name and ends with the workspace name
   * matches and takes preference over a service with the repository name</li>
   * </ol>
   * 
   * @throws JbpmException if no matching service is found
   */
  private JcrService findService() {
    Services services = JbpmContext.getCurrentJbpmContext().getServices();
    Map serviceFactories = services.getServiceFactories();

    // if there is a service called jcr
    if (serviceFactories.containsKey("jcr")) {
      // use that one
      return (JcrService) services.getService("jcr");
    }

    // compare the repository and workspace names with the service names
    String serviceName = null;
    for (Iterator iter = serviceFactories.keySet().iterator(); iter.hasNext();) {
      String candidate = (String) iter.next();
      if (candidate.startsWith(repository)) {
        if (candidate.length() == repository.length()) {
          if (serviceName == null) serviceName = candidate;
        }
        else if (candidate.endsWith(workspace)) {
          serviceName = candidate;
          break;
        }
      }
    }
    return (JcrService) services.getService(serviceName);
  }

  protected void setObject(Object value) {
    Node node = (Node) value;
    if (value == null) {
      repository = null;
      workspace = null;
      path = null;
    }
    else {
      try {
        // repository and workspace have to correspond with a service name,
        // as described in findService, unless there is a global "jcr" service
        Session session = node.getSession();
        repository = session.getRepository().getDescriptor(Repository.REP_NAME_DESC);
        workspace = session.getWorkspace().getName();
        path = node.getPath();

        if (log.isDebugEnabled()) {
          log.debug("stored jcr node, repository '" + repository + "', workspace '" + workspace
            + "' and path'" + path + '\'');
        }
      }
      catch (RepositoryException e) {
        throw new JbpmException("problem storing JCR node '" + node
          + "' in the process variable '" + name + "'", e);
      }
    }
  }

  private static final Log log = LogFactory.getLog(JcrNodeInstance.class);
}
