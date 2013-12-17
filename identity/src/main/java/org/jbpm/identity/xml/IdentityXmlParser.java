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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jbpm.JbpmException;
import org.jbpm.identity.Entity;
import org.jbpm.identity.Group;
import org.jbpm.identity.Membership;
import org.jbpm.identity.User;
import org.jbpm.util.ClassLoaderUtil;

public class IdentityXmlParser {

  private List entities = new ArrayList();
  private Map users = new HashMap();
  private Map groups = new HashMap();

  public static Entity[] parseEntitiesResource(String resource) {
    return new IdentityXmlParser().parse(resource);
  }

  public static Entity[] parseEntitiesResource(InputStream inputStream) {
    return new IdentityXmlParser().parse(inputStream);
  }

  public Entity[] parse(String resource) {
    try {
      URL resourceUrl = ClassLoaderUtil.getClassLoader().getResource(resource);
      return parse(new SAXReader().read(resourceUrl));
    }
    catch (DocumentException e) {
      throw new JbpmException("could not read identity resource: " + resource, e);
    }
  }

  public Entity[] parse(InputStream inputStream) {
    try {
      return parse(new SAXReader().read(inputStream));
    }
    catch (DocumentException e) {
      throw new JbpmException("could not read identity document", e);
    }
  }

  private Entity[] parse(Document document) {
    Element identitiesElement = document.getRootElement();

    readUsers(identitiesElement.elements("user"));
    readGroups(identitiesElement.elements("group"));
    readParentGroups(identitiesElement.elements("group"));
    readMemberships(identitiesElement.elements("membership"));

    return (Entity[]) entities.toArray(new Entity[entities.size()]);
  }

  private void readUsers(List userElements) {
    for (Iterator iter = userElements.iterator(); iter.hasNext();) {
      Element userElement = (Element) iter.next();
      String name = userElement.attributeValue("name");
      String email = userElement.attributeValue("email");
      String password = userElement.attributeValue("password");

      User user = new User(name);
      user.setEmail(email);
      user.setPassword(password);
      users.put(name, user);
      entities.add(user);
    }
  }

  private void readGroups(List groupElements) {
    for (Iterator iter = groupElements.iterator(); iter.hasNext();) {
      Element groupElement = (Element) iter.next();
      String name = groupElement.attributeValue("name");
      String type = groupElement.attributeValue("type");

      Group group = new Group(name);
      group.setType(type);
      entities.add(group);
      groups.put(name, group);
    }
  }

  private void readParentGroups(List groupElements) {
    for (Iterator iter = groupElements.iterator(); iter.hasNext();) {
      Element groupElement = (Element) iter.next();
      String childName = groupElement.attributeValue("name");
      String parentName = groupElement.attributeValue("parent");

      if (parentName != null) {
        Group parent = (Group) groups.get(parentName);
        if (parent == null) {
          throw new JbpmException("no such group: " + parentName);
        }

        Group child = (Group) groups.get(childName);
        parent.addChild(child);
      }
    }
  }

  private void readMemberships(List membershipElements) {
    for (Iterator iter = membershipElements.iterator(); iter.hasNext();) {
      Element membershipElement = (Element) iter.next();

      String role = membershipElement.attributeValue("role");
      Membership membership = new Membership();
      membership.setRole(role);

      String userName = membershipElement.attributeValue("user");
      User user = (User) users.get(userName);
      if (user == null) {
        throw new JbpmException("no such user: " + userName);
      }
      user.addMembership(membership);

      String groupName = membershipElement.attributeValue("group");
      Group group = (Group) groups.get(groupName);
      if (group == null) {
        throw new JbpmException("no such group: " + groupName);
      }
      group.addMembership(membership);

      entities.add(membership);
    }
  }
}
