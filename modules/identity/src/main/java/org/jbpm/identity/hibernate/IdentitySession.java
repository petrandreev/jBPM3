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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.identity.Entity;
import org.jbpm.identity.Group;
import org.jbpm.identity.Membership;
import org.jbpm.identity.User;
import org.jbpm.identity.assignment.ExpressionSession;
import org.jbpm.identity.security.IdentityService;
import org.jbpm.util.CollectionUtil;

public class IdentitySession implements IdentityService, ExpressionSession {
  private static final Log log = LogFactory.getLog(IdentitySession.class);

  Session session;
  private Transaction transaction;

  public IdentitySession(Session session) {
    this.session = session;
  }

  public IdentitySession() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if ((jbpmContext == null)
        || (jbpmContext.getSession() == null)
        || (!jbpmContext.getSession().isOpen())) {
      throw new RuntimeException("no active JbpmContext to create an identity session");
    }
    session = jbpmContext.getSession();
  }

  // IdentityService methods //////////////////////////////////////////////////

  public Long verify(String userName, String pwd) {
    Query query = session.createQuery("select user.id "
        + "from org.jbpm.identity.User as user where user.name = :userName and user.password = :password");
    query.setString("userName", userName);
    query.setString("password", pwd);
    return (Long) query.uniqueResult();
  }

  public User getUserById(Object userId) {
    return (User) session.load(User.class, (Long) userId);
  }

  // transaction convenience methods //////////////////////////////////////////

  public Session getSession() {
    return session;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public void beginTransaction() {
    try {
      transaction = session.beginTransaction();
    }
    catch (HibernateException e) {
      log.error(e);
      throw new JbpmException("could not begin transaction", e);
    }
  }

  public void commitTransaction() {
    if (transaction == null) {
      throw new RuntimeException("cannot commit : no transaction started");
    }
    try {
      session.flush();
      transaction.commit();
    }
    catch (HibernateException e) {
      log.error(e);
      throw new JbpmException("could not commit transaction", e);
    }
    transaction = null;
  }

  public void rollbackTransaction() {
    if (transaction == null) {
      throw new RuntimeException("cannot rollback : no transaction started");
    }
    try {
      transaction.rollback();
    }
    catch (HibernateException e) {
      log.error(e);
      throw new JbpmException("could not rollback transaction", e);
    }
    transaction = null;
  }

  public void commitTransactionAndClose() {
    commitTransaction();
    close();
  }

  public void rollbackTransactionAndClose() {
    rollbackTransaction();
    close();
  }

  public void close() {
    try {
      session.close();
    }
    catch (HibernateException e) {
      log.error(e);
      throw new JbpmException("could not close hibernate session", e);
    }
  }

  // identity methods /////////////////////////////////////////////////////////

  public void saveUser(User user) {
    session.save(user);
  }

  public void saveGroup(Group group) {
    session.save(group);
  }

  public void saveEntity(Entity entity) {
    session.save(entity);
  }

  public void saveMembership(Membership membership) {
    session.save(membership);
  }

  public User loadUser(long userId) {
    return (User) session.load(User.class, new Long(userId));
  }

  public Group loadGroup(long groupId) {
    return (Group) session.load(Group.class, new Long(groupId));
  }

  public void deleteGroup(Group group) {
    session.delete(group);
  }

  public void deleteUser(User user) {
    session.delete(user);
  }

  public User getUserByName(String userName) {
    Criteria criteria = session.createCriteria(User.class).add(Restrictions.eq("name", userName));
    List<User> users = CollectionUtil.checkList(criteria.list(), User.class);
    return users.isEmpty() ? null : users.get(0);
  }

  public Group getGroupByName(String groupName) {
    Criteria criteria = session.createCriteria(Group.class).add(Restrictions.eq("name", groupName));
    List<Group> groups = CollectionUtil.checkList(criteria.list(), Group.class);
    return groups.isEmpty() ? null : groups.get(0);
  }

  public List<User> getUsers() {
    Criteria criteria = session.createCriteria(User.class);
    return CollectionUtil.checkList(criteria.list(), User.class);
  }

  public List<String> getGroupNamesByUserAndGroupType(String userName, String groupType) {
    Criteria criteria = session.createCriteria(Membership.class)
        .createAlias("user", "u")
        .createAlias("group", "g")
        .add(Restrictions.eq("u.name", userName))
        .add(Restrictions.eq("g.type", groupType))
        .setProjection(Projections.property("g.name"));
    return CollectionUtil.checkList(criteria.list(), String.class);
  }

  public User getUserByGroupAndRole(String groupName, String role) {
    Criteria criteria = session.createCriteria(Membership.class)
        .add(Restrictions.eq("role", role))
        .createAlias("group", "g")
        .add(Restrictions.eq("g.name", groupName))
        .setProjection(Projections.property("user"));
    return (User) criteria.uniqueResult();
  }
}
