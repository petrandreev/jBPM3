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
package org.jbpm.security;

import java.security.Permission;

import org.jbpm.JbpmContext;

/**
 * provides helper methods to access the authentication and authorization
 * services.
 */
public abstract class SecurityHelper {
  
  /**
   * helper method to look up the authenticated actorId in the current 
   * jbpm context.
   */
  public static String getAuthenticatedActorId() {
    String actorId = null;
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext!=null) {
      AuthenticationService authenticationService = jbpmContext.getServices().getAuthenticationService();
      if (authenticationService!=null) {
        actorId = authenticationService.getActorId();
      }
    }
    return actorId;
  }
  
  /**
   * helper method to check the permissions of a jbpm secured operation.
   */
  public static void checkPermission(Permission permission) {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext!=null) {
      AuthorizationService authorizationService = jbpmContext.getServices().getAuthorizationService();
      if (authorizationService!=null) {
        authorizationService.checkPermission(permission);
      }
    }
  }
}
