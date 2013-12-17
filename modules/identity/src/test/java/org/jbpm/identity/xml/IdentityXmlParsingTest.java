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
package org.jbpm.identity.xml;

import java.util.Set;

import junit.framework.TestCase;

import org.jbpm.identity.Group;
import org.jbpm.identity.Membership;
import org.jbpm.identity.User;

public class IdentityXmlParsingTest extends TestCase {

  IdentityXmlParser identityXmlParser;

  protected void setUp() {
    identityXmlParser = new IdentityXmlParser();
    identityXmlParser.parse("org/jbpm/identity/xml/identity.xml");
  }

  public void testUser() {
    User sampleManager = identityXmlParser.users.get("manager");
    assertEquals("manager", sampleManager.getName());
    assertEquals("sample.manager@sample.domain", sampleManager.getEmail());
    assertEquals("manager", sampleManager.getPassword());
  }

  public void testGroup() {
    Group bananalovers = identityXmlParser.groups.get("bananalovers");
    assertEquals("bananalovers", bananalovers.getName());
    assertEquals("fruitpreference", bananalovers.getType());
  }

  public void testGroupParent() {
    Group residents = identityXmlParser.groups.get("residents");
    Group bananalovers = identityXmlParser.groups.get("bananalovers");
    assertSame(residents, bananalovers.getParent());
    assertEquals(1, residents.getChildren().size());
    assertSame(bananalovers, residents.getChildren().iterator().next());
  }

  public void testUserMembership() {
    User sampleManager = identityXmlParser.users.get("manager");
    Group bananalovers = identityXmlParser.groups.get("bananalovers");
    Set<Membership> sampleManagersMemberships = sampleManager.getMemberships();
    assertEquals(1, sampleManagersMemberships.size());
    Set<Membership> bananaloversMemberships = bananalovers.getMemberships();
    assertEquals(1, bananaloversMemberships.size());
    assertSame(bananaloversMemberships.iterator().next(), sampleManagersMemberships.iterator()
        .next());
  }
}
