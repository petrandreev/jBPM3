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

/**
 * one association between user and a group. The name of the membership
 * represents the role-name that the user fulfills in the group. A membership
 * can be a position in an organisation, therefore permissions can be associated
 * with a membership. The name of the membership can be used as the role name.
 * Meaning which role does the user play in the group.
 */
public class Membership extends Entity {

  private static final long serialVersionUID = 1L;

  protected String role;
  protected User user;
  protected Group group;

  // constructors /////////////////////////////////////////////////////////////

  public Membership() {
  }

  public static Membership create(User user, String role, Group group) {
    Membership membership = create(user, group);
    membership.role = role;
    return membership;
  }

  public static Membership create(User user, Group group) {
    Membership membership = new Membership();
    user.addMembership(membership);
    group.addMembership(membership);
    return membership;
  }

  // setters //////////////////////////////////////////////////////////////////
  public void setUser(User user) {
    this.user = user;
  }

  public void setGroup(Group group) {
    this.group = group;
  }

  // getters //////////////////////////////////////////////////////////////////

  public Group getGroup() {
    return group;
  }

  public User getUser() {
    return user;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
