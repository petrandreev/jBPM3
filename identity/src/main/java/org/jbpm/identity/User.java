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
import java.util.Iterator;
import java.util.Set;

/**
 * user or a system.
 */
public class User extends Entity implements Principal {

  private static final long serialVersionUID = 1L;

  protected String password;
  protected String email;
  protected Set memberships;

  public User() {
  }

  public User(String name) {
    super(name);
  }

  public void addMembership(Membership membership) {
    if (memberships == null) memberships = new HashSet();
    memberships.add(membership);
    membership.setUser(this);
  }

  public Set getGroupsForGroupType(String groupType) {
    Set groups = Collections.EMPTY_SET;
    if (memberships != null) {
      for (Iterator iter = memberships.iterator(); iter.hasNext();) {
        Membership membership = (Membership) iter.next();
        if (groupType.equals(membership.getGroup().getType())) {
          if (groups.isEmpty()) groups = new HashSet();
          groups.add(membership.getGroup());
        }
      }
    }
    return groups;
  }

  public Set getGroupsForMembershipRole(String membershipRole) {
    Set groups = Collections.EMPTY_SET;
    if (memberships != null) {
      for (Iterator iter = memberships.iterator(); iter.hasNext();) {
        Membership membership = (Membership) iter.next();
        if (membershipRole.equals(membership.getRole())) {
          if (groups.isEmpty()) groups = new HashSet();
          groups.add(membership.getGroup());
        }
      }
    }
    return groups;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPassword() {
    return password;
  }

  public Set getMemberships() {
    return memberships;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof User)) return false;

    User other = (User) obj;
    return name != null ? name.equals(other.getName()) : false;
  }

  public int hashCode() {
    return name != null ? name.hashCode() : System.identityHashCode(this);
  }

  public String toString() {
    return "User(" + name + ")";
  }

}
