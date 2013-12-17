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
package org.jbpm.identity;

import java.io.Serializable;
import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

/**
 * common supertype for users, groups and memberships 
 * that manages the name and permissions.
 */
public class Entity implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private long id;
  protected String name;
  /* permissions is a set of java.security.Permission */
  protected Set permissions;
  
  // constructors /////////////////////////////////////////////////////////////
  
  public Entity() {
  }

  public Entity(String name) {
    this.name = name;
  }

  public Entity(String name, Set permissions) {
    this.name = name;
    this.permissions = permissions;
  }
  
  // permissions //////////////////////////////////////////////////////////////
  
  public void addPermission(Permission permission) {
    if (permissions==null) permissions = new HashSet();
    permissions.add(permission);
  }
  public Set getPermissions() {
    return permissions;
  }
  public void removePermission(Permission permission) {
    if (permissions!=null) {
      permissions.remove(permission);
    }
  }

  // getters //////////////////////////////////////////////////////////////////
  
  public long getId() {
    return id;
  }
  public String getName() {
    return name;
  }
}
