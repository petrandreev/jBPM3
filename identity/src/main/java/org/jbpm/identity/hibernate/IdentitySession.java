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

import org.hibernate.HibernateException;
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
import org.jbpm.persistence.JbpmPersistenceException;

public class IdentitySession implements IdentityService, ExpressionSession {

  private final Session session;
  private Transaction transaction;

  public IdentitySession(Session session) {
    this.session = session;
  }

  public IdentitySession() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext == null) throw new JbpmException("no active jbpm context");

    session = jbpmContext.getSession();
    if (session == null) throw new JbpmException("no active hibernate session");
  }

  // IdentityService methods //////////////////////////////////////////////////

  public Object verify(String userName, String pwd) {
    return session.createCriteria(User.class)
      .setProjection(Projections.property("id"))
      .add(Restrictions.eq("name", userName))
      .add(Restrictions.eq("password", pwd))
      .uniqueResult();
  }

  public User getUserById(Object userId) {
    return (User) session.load(User.class, (Long) userId);
  }

  // transaction convenience methods //////////////////////////////////////////

  public Session getSession() {
    return session;
  }

  /** @deprecated call {@link #getSession()}.getTransaction() instead */
  public Transaction getTransaction() {
    return transaction;
  }

  /** @deprecated call {@link #getSession()}.beginTransaction() instead */
  public void beginTransaction() {
    try {
      transaction = session.beginTransaction();
    }
    catch (HibernateException e) {
      throw new JbpmPersistenceException("could not begin transaction", e);
    }
  }

  /** @deprecated call {@link #getSession()}.getTransaction().commit() instead */
  public void commitTransaction() {
    if (transaction == null) {
      throw new JbpmException("cannot commit: no transaction started");
    }
    try {
      session.flush();
      transaction.commit();
    }
    catch (HibernateException e) {
      throw new JbpmPersistenceException("could not commit transaction", e);
    }
    transaction = null;
  }

  /** @deprecated call {@link #getSession()}.getTransaction().rollback() instead */
  public void rollbackTransaction() {
    if (transaction == null) {
      throw new JbpmException("cannot rollback: no transaction started");
    }
    try {
      transaction.rollback();
    }
    catch (HibernateException e) {
      throw new JbpmPersistenceException("could not rollback transaction", e);
    }
    transaction = null;
  }

  /** @deprecated call {@link #getSession()}.getTransaction().commit() instead */
  public void commitTransactionAndClose() {
    commitTransaction();
    close();
  }

  /** @deprecated call {@link #getSession()}.getTransaction().rollback() instead */
  public void rollbackTransactionAndClose() {
    rollbackTransaction();
    close();
  }

  /** @deprecated call {@link #getSession()}.close() instead */
  public void close() {
    try {
      session.close();
    }
    catch (HibernateException e) {
      throw new JbpmPersistenceException("could not close hibernate session", e);
    }
  }

  // identity methods /////////////////////////////////////////////////////////

  /** @deprecated call {@link #saveEntity(Entity)} instead */
  public void saveUser(User user) {
    session.save(user);
  }

  /** @deprecated call {@link #saveEntity(Entity)} instead */
  public void saveGroup(Group group) {
    session.save(group);
  }

  public void saveEntity(Entity entity) {
    session.save(entity);
  }

  /** @deprecated call {@link #saveEntity(Entity)} instead */
  public void saveMembership(Membership membership) {
    session.save(membership);
  }

  public User loadUser(long userId) {
    return (User) session.load(User.class, new Long(userId));
  }

  public Group loadGroup(long groupId) {
    return (Group) session.load(Group.class, new Long(groupId));
  }

  /** @deprecated call {@link #deleteEntity(Entity)} instead */
  public void deleteGroup(Group group) {
    session.delete(group);
  }

  /** @deprecated call {@link #deleteEntity(Entity)} instead */
  public void deleteUser(User user) {
    session.delete(user);
  }

  public void deleteEntity(Entity entity) {
    session.delete(entity);
  }

  public User getUserByName(String userName) {
    return (User) session.createCriteria(User.class)
      .add(Restrictions.eq("name", userName))
      .setMaxResults(1)
      .uniqueResult();
  }

  public Group getGroupByName(String groupName) {
    return (Group) session.createCriteria(Group.class)
      .add(Restrictions.eq("name", groupName))
      .setMaxResults(1)
      .uniqueResult();
  }

  public List getUsers() {
    return session.createCriteria(User.class).list();
  }

  public List getGroupNamesByUserAndGroupType(String userName, String groupType) {
    return session.createCriteria(Membership.class)
      .createAlias("group", "g")
      .createAlias("user", "u")
      .add(Restrictions.eq("u.name", userName))
      .add(Restrictions.eq("g.type", groupType))
      .setProjection(Projections.property("g.name"))
      .list();
  }

  public User getUserByGroupAndRole(String groupName, String role) {
    return (User) session.createCriteria(Membership.class)
      .createAlias("group", "g")
      .add(Restrictions.eq("role", role))
      .add(Restrictions.eq("g.name", groupName))
      .setProjection(Projections.property("user"))
      .uniqueResult();
  }
}
