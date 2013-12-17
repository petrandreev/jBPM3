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

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * user or a system.
 */
public class User extends Entity implements Principal {

  private static final long serialVersionUID = 1L;

  protected String password = null;
  protected String email = null;
  protected Set<Membership> memberships = null;

  public User() {
  }

  public User(String name) {
    super(name);
  }

  public void addMembership(Membership membership) {
    if (memberships == null)
      memberships = new HashSet<Membership>();
    memberships.add(membership);
    membership.setUser(this);
  }

  public Set<Group> getGroupsForGroupType(String groupType) {
    Set<Group> groups;
    if (memberships != null) {
      groups = new HashSet<Group>();
      for (Membership membership : memberships) {
        if (groupType.equals(membership.getGroup().getType())) {
          groups.add(membership.getGroup());
        }
      }
    }
    else {
      groups = Collections.emptySet();
    }
    return groups;
  }

  public Set<Group> getGroupsForMembershipRole(String membershipRole) {
    Set<Group> groups;
    if (memberships != null) {
      groups = new HashSet<Group>();
      for (Membership membership : memberships) {
        if (membershipRole.equals(membership.getRole())) {
          groups.add(membership.getGroup());
        }
      }
    }
    else {
      groups = Collections.emptySet();
    }
    return groups;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPassword() {
    return password;
  }

  public Set<Membership> getMemberships() {
    return memberships;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
