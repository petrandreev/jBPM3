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
import java.util.ArrayList;
import java.util.HashMap;
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
import org.jbpm.util.CollectionUtil;

public class IdentityXmlParser {

  List<Entity> entities = new ArrayList<Entity>();
  Map<String, User> users = new HashMap<String, User>();
  Map<String, Group> groups = new HashMap<String, Group>();

  public static Entity[] parseEntitiesResource(String resource) {
    return new IdentityXmlParser().parse(resource);
  }

  public static Entity[] parseEntitiesResource(InputStream inputStream) {
    return new IdentityXmlParser().parse(inputStream);
  }

  public Entity[] parse(String resource) {
    return parse(ClassLoaderUtil.getStream(resource));
  }

  public Entity[] parse(InputStream inputStream) {
    Document document;
    try {
      document = new SAXReader().read(inputStream);
    }
    catch (DocumentException e) {
      throw new JbpmException("could not parse identities", e);
    }
    Element identitiesRootElement = document.getRootElement();

    readUsers(checkElements(identitiesRootElement.elements("user")));
    readGroups(checkElements(identitiesRootElement.elements("group")));
    readParentGroups(checkElements(identitiesRootElement.elements("group")));
    readMemberships(checkElements(identitiesRootElement.elements("membership")));

    return entities.toArray(new Entity[entities.size()]);
  }

  private static List<Element> checkElements(List<?> elements) {
    return CollectionUtil.checkList(elements, Element.class);
  }

  private void readUsers(List<Element> userElements) {
    for (Element userElement : userElements) {
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

  private void readGroups(List<Element> groupElements) {
    for (Element groupElement : groupElements) {
      String name = groupElement.attributeValue("name");
      String type = groupElement.attributeValue("type");

      Group group = new Group(name);
      group.setType(type);
      entities.add(group);
      groups.put(name, group);
    }
  }

  private void readParentGroups(List<Element> groupElements) {
    for (Element groupElement : groupElements) {
      String childName = groupElement.attributeValue("name");
      String parentName = groupElement.attributeValue("parent");

      if (parentName != null) {
        Group parent = groups.get(parentName);
        if (parent == null)
          throw new JbpmException("unexisting parent group '" + parentName + "'");

        Group child = groups.get(childName);
        parent.addChild(child);
      }
    }
  }

  private void readMemberships(List<Element> membershipElements) {
    for (Element membershipElement : membershipElements) {
      String userName = membershipElement.attributeValue("user");
      User user = users.get(userName);
      if (user == null)
        throw new JbpmException("unexisting membership user '" + userName + "'");

      String groupName = membershipElement.attributeValue("group");
      Group group = groups.get(groupName);
      if (group == null)
        throw new JbpmException("unexisting membership group '" + groupName + "'");

      Membership membership = new Membership();
      membership.setRole(membershipElement.attributeValue("role"));
      group.addMembership(membership);
      user.addMembership(membership);

      entities.add(membership);
    }
  }
}
