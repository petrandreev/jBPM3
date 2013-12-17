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

import org.jbpm.svc.Services;

/**
 * provides helper methods to access the authentication and authorization services.
 */
public class SecurityHelper {

  private SecurityHelper() {
    // hide default constructor to prevent instantiation
  }

  /**
   * helper method to look up the authenticated actorId in the current jbpm context.
   */
  public static String getAuthenticatedActorId() {
    AuthenticationService authService = (AuthenticationService)
      Services.getCurrentService(Services.SERVICENAME_AUTHENTICATION, false);
    return authService != null ? authService.getActorId() : null;
  }

  /**
   * helper method to check the permissions of a jbpm secured operation.
   */
  public static void checkPermission(Permission permission) {
    AuthorizationService authService = (AuthorizationService)
      Services.getCurrentService(Services.SERVICENAME_AUTHORIZATION, false);
    if (authService != null) authService.checkPermission(permission);
  }
}
