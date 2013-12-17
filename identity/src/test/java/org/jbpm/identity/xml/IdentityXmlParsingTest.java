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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.identity.Entity;
import org.jbpm.identity.Group;
import org.jbpm.identity.User;

public class IdentityXmlParsingTest extends AbstractJbpmTestCase {
  
  private Map users = new HashMap();
  private Map groups = new HashMap();

  public void setUp() {
    IdentityXmlParser identityXmlParser = new IdentityXmlParser();
    Entity[] entities = identityXmlParser.parse("org/jbpm/identity/xml/identity.xml");
    for (int i = 0; i < entities.length; i++) {
      Entity entity = entities[i];
      if (entity instanceof User) {
        users.put(entity.getName(), entity);
      }
      else if (entity instanceof Group) {
        groups.put(entity.getName(), entity);
      }
    }
  }

  public void testUser() {
    User sampleManager = (User) users.get("manager");
    assertEquals("manager", sampleManager.getName());
    assertEquals("sample.manager@sample.domain", sampleManager.getEmail());
    assertEquals("manager", sampleManager.getPassword());
  }

  public void testGroup() {
    Group bananalovers = (Group) groups.get("bananalovers");
    assertEquals("bananalovers", bananalovers.getName());
    assertEquals("fruitpreference", bananalovers.getType());
  }

  public void testGroupParent() {
    Group residents = (Group) groups.get("residents");
    Group bananalovers = (Group) groups.get("bananalovers");
    assertSame(residents, bananalovers.getParent());
    assertEquals(1, residents.getChildren().size());
    assertSame(bananalovers, residents.getChildren().iterator().next());
  }

  public void testUserMembership() {
    User sampleManager = (User) users.get("manager");
    Group bananalovers = (Group) groups.get("bananalovers");
    Set sampleManagersMemberships = sampleManager.getMemberships();
    assertEquals(1, sampleManagersMemberships.size());
    Set bananaloversMemberships = bananalovers.getMemberships();
    assertEquals(1, bananaloversMemberships.size());
    assertSame(bananaloversMemberships.iterator().next(), sampleManagersMemberships.iterator()
      .next());
  }
}
