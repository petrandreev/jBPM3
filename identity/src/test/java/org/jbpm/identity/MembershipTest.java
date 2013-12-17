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

import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class MembershipTest extends TestCase {

  public void testGroupsForGroupType() {
    User john = new User("johndoe");
    Group qaTeam = new Group("qa team", "hierarchy");
    Group marketingTeam = new Group("marketing team", "hierarchy");
    Membership.create(john, "mgr", qaTeam);
    Membership.create(john, "mgr", marketingTeam);
    
    Set groups = john.getGroupsForGroupType("hierarchy");
    assertEquals(2, groups.size());
    assertEquals(2, groups.size());
    assertTrue(containsGroup(groups, "qa team"));
    assertTrue(containsGroup(groups, "marketing team"));
  }

  public void testEmptyGroupsForGroupType() {
    User john = new User("johndoe");
    Group qaTeam = new Group("qa team", "hierarchy");
    Group marketingTeam = new Group("marketing team", "hierarchy");
    Membership.create(john, "mgr", qaTeam);
    Membership.create(john, "mgr", marketingTeam);
    
    Set groups = john.getGroupsForGroupType("non-existing-group-type");
    assertNotNull(groups);
    assertEquals(0, groups.size());
  }
  
  public void testGetGroupsForRole() {
    User john = new User("johndoe");
    Group qaTeam = new Group("qa team", "hierarchy");
    Group marketingTeam = new Group("marketing team", "hierarchy");
    Group nozems = new Group("nozems");
    Membership.create(john, "mgr", qaTeam);
    Membership.create(john, "mgr", marketingTeam);
    Membership.create(john, "member", nozems);
    
    Set groups = john.getGroupsForMembershipRole("mgr");
    assertEquals(2, groups.size());
  }
  
  public void testGetUsersInRole() {
    User john = new User("johndoe");
    Group qaTeam = new Group("qa team", "hierarchy");
    Group marketingTeam = new Group("marketing team", "hierarchy");
    Membership.create(john, "mgr", qaTeam);
    Membership.create(john, "mgr", marketingTeam);
    
    assertEquals(1, qaTeam.getUsersForMembershipRole("mgr").size());
    assertSame(john, qaTeam.getUsersForMembershipRole("mgr").iterator().next());
    assertEquals(1, marketingTeam.getUsersForMembershipRole("mgr").size());
    assertSame(john, marketingTeam.getUsersForMembershipRole("mgr").iterator().next());
  }

  public void testGetNoUsersInRole() {
    Set users = new Group().getUsersForMembershipRole("buzz");
    assertNotNull(users);
    assertEquals(0, users.size());
  }

  public boolean containsGroup(Set groups, String groupName) {
    Iterator iter = groups.iterator();
    while (iter.hasNext()) {
      if( groupName.equals(((Group)iter.next()).getName())) {
        return true;
      }
    }
    return false;
  }
}
