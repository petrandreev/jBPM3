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
package org.jbpm.web;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

public class JbpmContextFilter implements Filter, Serializable {

  private static final long serialVersionUID = 1L;

  String jbpmConfigurationResource = null;
  String jbpmContextName = null;
  boolean isAuthenticationEnabled = true;

  public void init(FilterConfig filterConfig) throws ServletException {
    // get the jbpm configuration resource
    this.jbpmConfigurationResource = filterConfig.getInitParameter("jbpm.configuration.resource");
    
    // get the jbpm context to be used from the jbpm configuration
    this.jbpmContextName = filterConfig.getInitParameter("jbpm.context.name");
    if (jbpmContextName==null) {
      jbpmContextName = JbpmContext.DEFAULT_JBPM_CONTEXT_NAME;
    }
    
    // see if authentication is turned off
    String isAuthenticationEnabledText = filterConfig.getInitParameter("authentication");
    if ( (isAuthenticationEnabledText!=null)
         && ("disabled".equalsIgnoreCase(isAuthenticationEnabledText))
       ) {
      isAuthenticationEnabled = false;
    }
  }

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    String actorId = null;

    // see if we can get the authenticated swimlaneActorId
    if (servletRequest instanceof HttpServletRequest) {
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      Principal userPrincipal = httpServletRequest.getUserPrincipal();
      if (userPrincipal != null) {
        actorId = userPrincipal.getName();
      }
    }

    JbpmContext jbpmContext = getJbpmConfiguration().createJbpmContext(jbpmContextName);
    try {
      if (isAuthenticationEnabled) {
        jbpmContext.setActorId(actorId);
      }
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      jbpmContext.close();
    }
  }

  protected JbpmConfiguration getJbpmConfiguration() {
    return JbpmConfiguration.getInstance(jbpmConfigurationResource);
  }

  public void destroy() {
  }
}
