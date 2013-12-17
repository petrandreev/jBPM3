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
package org.jbpm.identity.security;

import java.security.*;
import java.util.*;
import org.jbpm.identity.Entity;

/**
 * a java.security.Policy implementation that in combination with the 
 * IdentityLoginModule enforces the secirity permissions modelled as
 * in the package org.jbpm.identity.
 */
public class IdentityPolicy extends Policy {
  
  public static final PermissionCollection ALL_PERMISSIONSCOLLECTION = new Permissions();
  static {
    ALL_PERMISSIONSCOLLECTION.add(new AllPermission());
    ALL_PERMISSIONSCOLLECTION.setReadOnly();
  }
  
  public void refresh() {
  }

  public PermissionCollection getPermissions(CodeSource codesource) {
    // no checks are done based on the origin of the code
    // checks are only based on *who* is running the code.
    return ALL_PERMISSIONSCOLLECTION;
  }
  
  public PermissionCollection getPermissions(ProtectionDomain domain) {
    PermissionCollection permissionCollection = new Permissions();
    
    Principal[] principals = domain.getPrincipals();
    // if there are principals
    if (principals!=null) {
      // loop over the principals
      for (int i=0; i<principals.length; i++) {
        // if the principal is a org.jbpm.identity.Entity
        if (Entity.class.isAssignableFrom(principals[i].getClass())) {
          // add all the identity's permissions to the set of permissions.
          Iterator iter = ((Entity)principals[i]).getPermissions().iterator();
          while (iter.hasNext()) {
            permissionCollection.add((Permission) iter.next());
          }
        }
      }
    }
    
    return super.getPermissions(domain);
  }
  
  public boolean implies(ProtectionDomain domain, Permission permission) {
    return getPermissions(domain).implies(permission);
  }
}
