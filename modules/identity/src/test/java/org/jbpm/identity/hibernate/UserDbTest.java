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
package org.jbpm.identity.hibernate;

import java.net.NetPermission;
import java.security.AllPermission;
import java.security.Permission;
import java.util.Set;

import org.jbpm.identity.Group;
import org.jbpm.identity.IdentityDbTestCase;
import org.jbpm.identity.Membership;
import org.jbpm.identity.User;

public class UserDbTest extends IdentityDbTestCase {

  public void testUser() {
    User user = new User("johndoe");
    user = saveAndReload(user);
    assertEquals("johndoe", user.getName());

    deleteUser(user.getId());
  }

  public void testUserMemberships() {
    User john = new User("johndoe");
    Group qaTeam = new Group("qa team", "hierarchy");
    Group marketingTeam = new Group("marketing team", "hierarchy");
    Membership.create(john, "mgr", qaTeam);
    Membership.create(john, "mgr", marketingTeam);

    john = saveAndReload(john);

    Set<Group> groups = john.getGroupsForGroupType("hierarchy");

    assertEquals(2, groups.size());
    assertEquals(2, groups.size());
    assertTrue(containsGroup(groups, "qa team"));
    assertTrue(containsGroup(groups, "marketing team"));

    deleteGroup(marketingTeam.getId());
    deleteGroup(qaTeam.getId());
    deleteUser(john.getId());
  }

  public void testUserPermissions() {
    User user = new User("johndoe");
    user.addPermission(new NetPermission("connect", "9001"));
    user.addPermission(new AllPermission("all", "everything"));

    user = saveAndReload(user);

    Set<Permission> permissions = user.getPermissions();
    assertNotNull(permissions);
    assertEquals(2, permissions.size());

    deleteUser(user.getId());
  }

  public void testVerifyWrongUser() {
    assertNull(identitySession.verify("unexisting-user", "wrong password"));
  }

  public void testVerifyValidPwd() {
    User user = new User("johndoe");
    user.setPassword("johnspwd");
    user = saveAndReload(user);
    try {
      assertEquals(user.getId(), identitySession.verify("johndoe", "johnspwd").longValue());
    }
    finally {
      deleteUser(user.getId());
    }
  }

  private boolean containsGroup(Set<Group> groups, String groupName) {
    for (Group group : groups) {
      if (groupName.equals(group.getName())) {
        return true;
      }
    }
    return false;
  }
}
