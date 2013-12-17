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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.jcr.JcrService;
import org.jbpm.svc.Services;

public class JcrNodeInstance extends VariableInstance {

  private static final long serialVersionUID = 1L;
  
  String repository = null;
  String workspace = null;
  String path = null;

  public boolean isStorable(Object value) {
    if (value==null) return true;
    return Node.class.isAssignableFrom(value.getClass());
  }

  protected Object getObject() {
    if (path==null) return null;
    
    // THE NODE REPOSITORY AND WORKSPACE NAME GOT TO CORRESPOND WITH A JBPM SERVICE NAME
    JcrService jcrService = findService();
    if (jcrService==null) {
      throw new JbpmException("couldn't find jBPM service for JCR repository '"+repository+"', workspace '"+workspace+"'");
    }
    Session session = jcrService.getSession();
    Item item;
    try {
      item = session.getItem(path);
    } catch (Exception e) {
      // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
      throw new JbpmException("can't fetch JCR node '"+path+"' from repository '"+repository+"', workspace '"+workspace+"'", e);
    }
    return item;
  }

  /**
   * find the service that matches the stored repository and workspace.
   * The matching is done as follows:
   *  - a service name that is equal to the repository name matches
   *  - a service that starts with the repository name and ends with the workspace name matches 
   *    and takes preference over a service with the repository name
   * @throws JbpmException if no matching jBPM context service is found. 
   */
  private JcrService findService() {
    String serviceName = null;
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    Services services = jbpmContext.getServices();

    // if there is a service called jcr
    if (services.hasService("jcr")) {
      // use that one
      serviceName = "jcr";

    } else { // otherwise
      // start matching the JCR workspace name with the jbpm service names
      Iterator serviceNames = services.getServiceFactories().keySet().iterator();
      while (serviceNames.hasNext()) {
        String candidate = (String) serviceNames.next();
        if (candidate.startsWith(workspace)) {
          if (candidate.length()==workspace.length()) {
            if (serviceName==null) {
              serviceName = candidate;
            }
          } else if (candidate.endsWith(workspace)) {
            serviceName = candidate;
          }
        }
      }
    }
    
    if (serviceName==null) {
      throw new JbpmException("couldn't find service for JCR repository '"+repository+"', workspace '"+workspace+"'");
    }
    
    return (JcrService) services.getService(serviceName);
  }

  protected void setObject(Object value) {
    Node node = (Node) value;
    if (value==null) {
      repository = null;
      workspace = null;
      path = null;
    } else {
      try {
        Session session = node.getSession();
        Repository repo = session.getRepository();
        Workspace wspace = session.getWorkspace();
        
        // THE NODE REPOSITORY AND WORKSPACE NAME GOT TO CORRESPOND WITH A JBPM SERVICE NAME
        repository = repo.getDescriptor(Repository.REP_NAME_DESC);
        workspace = wspace.getName();
        path = node.getPath();
  
        log.debug("stored jcr node repository("+repository+"), workspace("+workspace+") and path("+path+")");
      } catch (RepositoryException e) {
        throw new JbpmException("problem storing JCR node '"+node+"' in the process variable '"+name+"'", e);
      }
    }
  }
  
  private static Log log = LogFactory.getLog(JcrNodeInstance.class);
}
