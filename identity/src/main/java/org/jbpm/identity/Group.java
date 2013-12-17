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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * group of users.  
 * 
 * <p>The group type allows for the distinction of 
 * hierarchical groups, security roles and others.
 * </p>
 * 
 * <p>
 * Following name convention is recommended for 
 * assigning group types :
 * <ul>
 *   <li><b>hierarchy</b>: for hierarchical groups
 *   like teams, business units and companies.</li>
 *   <li><b>security-role</b>: for j2ee and servlet 
 *   security roles like admin, user, ...</li>
 * </ul>
 * </p> 
 */
public class Group extends Entity {

  private static final long serialVersionUID = 1L;

  protected String type;
  protected Group parent;
  protected Set children;
  protected Set memberships;

  public Group() {
  }

  public Group(String name) {
    super(name);
  }

  public Group(String name, String type) {
    super(name);
    this.type = type;
  }

  public void addMembership(Membership membership) {
    if (memberships==null) memberships = new HashSet();
    memberships.add(membership);
    membership.setGroup(this);
  }
  
  public void addChild(Group child) {
    if (children==null) children = new HashSet();
    children.add(child);
    child.setParent(this);
  }
  
  public Set getUsers() {
    Set users = new HashSet();
    if(memberships!=null) {
      Iterator iter = memberships.iterator();
      while (iter.hasNext()) {
        Membership membership = (Membership) iter.next();
        users.add(membership.getUser());
      }
    }
    return users;
  }

  public Set getUsersForMembershipRole(String membershipRole) {
    Set users = new HashSet();
    if(memberships!=null) {
      Iterator iter = memberships.iterator();
      while (iter.hasNext()) {
        Membership membership = (Membership) iter.next();
        if (membershipRole.equals(membership.getRole())){
          users.add(membership.getUser());
        }
      }
    }
    return users;
  }

  public Set getMemberships() {
    return memberships;
  }
  public Set getChildren() {
    return children;
  }
  public Group getParent() {
    return parent;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public void setChildren(Set children) {
    this.children = children;
  }
  public void setMemberships(Set memberships) {
    this.memberships = memberships;
  }
  public void setParent(Group parent) {
    this.parent = parent;
  }
}
