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
package org.jbpm.identity.assignment;

import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.identity.Entity;
import org.jbpm.identity.Group;
import org.jbpm.identity.User;
import org.jbpm.identity.hibernate.IdentitySession;
import org.jbpm.security.SecurityHelper;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;

/**
 * implements an expression language for assigning actors to tasks based on this identity
 * component.
 * 
 * <pre>
 * syntax : first-term --> next-term --> next-term --> ... --> next-term
 * 
 * first-term ::= previous |
 *                swimlane(swimlane-name) |
 *                variable(variable-name) |
 *                user(user-name) |
 *                group(group-name)
 * 
 * next-term ::= group(group-type) |
 *               member(role-name)
 * </pre>
 */
public class ExpressionAssignmentHandler implements AssignmentHandler {

  private static final long serialVersionUID = 1L;

  protected String expression;
  protected ExecutionContext executionContext;
  protected ExpressionSession expressionSession;
  protected TermTokenizer tokenizer;
  protected Entity entity;

  public void assign(Assignable assignable, ExecutionContext executionContext) {
    expressionSession = getExpressionSession();
    if (expressionSession == null) {
      throw new ExpressionAssignmentException("no expression session");
    }
    this.tokenizer = new TermTokenizer(expression);
    this.executionContext = executionContext;
    entity = resolveFirstTerm(tokenizer.nextTerm());
    while (tokenizer.hasMoreTerms() && entity != null) {
      entity = resolveNextTerm(tokenizer.nextTerm());
    }
    // if the expression did not resolve to an actor
    if (entity == null) {
      // complain!
      throw new ExpressionAssignmentException("could not resolve assignment expression: "
        + expression);
    }
    // else if the expression evaluated to a user
    else if (entity instanceof User) {
      // do direct assignment
      assignable.setActorId(entity.getName());
    }
    // else if the expression evaluated to a group
    else if (entity instanceof Group) {
      // put the group in the pool
      assignable.setPooledActors(new String[] {
        entity.getName()
      });
    }
  }

  /**
   * serves as a hook for customizing the way the identity session is retrieved. overload this
   * method to reuse this expression assignment handler for your user data store.
   */
  protected ExpressionSession getExpressionSession() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext == null) {
      throw new ExpressionAssignmentException("no current jbpm context");
    }
    return (IdentitySession) jbpmContext.getServices()
      .getPersistenceService()
      .getCustomSession(IdentitySession.class);
  }

  protected Entity resolveFirstTerm(String term) {
    Entity entity;
    if (term.equalsIgnoreCase("previous")) {
      String userName = SecurityHelper.getAuthenticatedActorId();
      entity = getUserByName(userName);
    }
    else if (term.startsWith("swimlane(") && term.endsWith(")")) {
      String swimlaneName = term.substring(9, term.length() - 1).trim();
      String userName = getSwimlaneActorId(swimlaneName);
      entity = getUserByName(userName);
    }
    else if (term.startsWith("variable(") && term.endsWith(")")) {
      String variableName = term.substring(9, term.length() - 1).trim();
      Object value = getVariable(variableName);

      if (value instanceof String) {
        entity = getUserByName((String) value);
      }
      else if (value instanceof Entity) {
        entity = (Entity) value;
      }
      else {
        throw new ExpressionAssignmentException(value + " is neither user name nor entity");
      }
    }
    else if (term.startsWith("user(") && term.endsWith(")")) {
      String userName = term.substring(5, term.length() - 1).trim();
      entity = getUserByName(userName);
    }
    else if (term.startsWith("group(") && term.endsWith(")")) {
      String groupName = term.substring(6, term.length() - 1).trim();
      entity = getGroupByName(groupName);
    }
    else {
      throw new ExpressionAssignmentException("could not interpret first term: " + term);
    }

    return entity;
  }

  protected Entity resolveNextTerm(String term) {
    if (term.startsWith("group(") && term.endsWith(")")) {
      String groupType = term.substring(6, term.length() - 1).trim();
      User user = (User) entity;
      Set groups = user.getGroupsForGroupType(groupType);
      if (groups.isEmpty()) {
        throw new ExpressionAssignmentException("no groups for type: " + groupType);
      }
      entity = (Entity) groups.iterator().next();
    }
    else if (term.startsWith("member(") && term.endsWith(")")) {
      String role = term.substring(7, term.length() - 1).trim();
      Group group = (Group) entity;
      entity = expressionSession.getUserByGroupAndRole(group.getName(), role);
      if (entity == null) {
        throw new ExpressionAssignmentException("no users in role: " + role);
      }
    }
    else {
      throw new ExpressionAssignmentException("could not interpret term: " + term);
    }
    return entity;
  }

  protected Object getVariable(String variableName) {
    Token token = executionContext.getToken();
    return executionContext.getContextInstance().getVariable(variableName, token);
  }

  protected Entity getGroupByName(String groupName) {
    Group group = expressionSession.getGroupByName(groupName);
    if (group == null) {
      throw new ExpressionAssignmentException("no such group: " + groupName);
    }
    return group;
  }

  protected Entity getUserByName(String userName) {
    User user = expressionSession.getUserByName(userName);
    if (user == null) {
      throw new ExpressionAssignmentException("no such user: " + user);
    }
    return user;
  }

  protected String getSwimlaneActorId(String swimlaneName) {
    SwimlaneInstance swimlaneInstance = executionContext.getTaskMgmtInstance()
      .getSwimlaneInstance(swimlaneName);
    if (swimlaneInstance == null) {
      throw new ExpressionAssignmentException("no such swimlane: " + swimlaneName);
    }
    return swimlaneInstance.getActorId();
  }
}
