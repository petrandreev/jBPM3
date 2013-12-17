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

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.identity.Group;
import org.jbpm.identity.IdentityDbTestCase;
import org.jbpm.identity.Membership;
import org.jbpm.identity.User;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class ExpressionAssignmentHandlerTest extends IdentityDbTestCase {

  ExpressionAssignmentHandler expressionAssignmentHandler;
  TaskInstance assignable = new TaskInstance();
  ProcessInstance processInstance;
  ExecutionContext executionContext;

  ProcessDefinition processDefinition;
  Membership membershipJohn, membershipBill;
  Group hellsangels;
  User john, bill;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    expressionAssignmentHandler = new ExpressionAssignmentHandler();
    setUpProcessInstance();
    setUpUserData();
    newTransaction();
    jbpmContext.setActorId("bill");
  }

  @Override
  protected void tearDown() throws Exception {
    deleteUser(john.getId());
    deleteUser(bill.getId());
    deleteGroup(hellsangels.getId());
    graphSession.deleteProcessDefinition(processDefinition.getId());
    super.tearDown();
  }

  private void setUpUserData() {
    john = new User("john");
    bill = new User("bill");
    hellsangels = new Group("hellsangels", "hierarchy");
    membershipJohn = Membership.create(john, "leaderofthegang", hellsangels);
    membershipBill = Membership.create(bill, hellsangels);

    identitySession.saveUser(john);
    identitySession.saveUser(bill);
    identitySession.saveGroup(hellsangels);
    identitySession.saveMembership(membershipJohn);
    identitySession.saveMembership(membershipBill);
  }

  private void setUpProcessInstance() {
    processDefinition = ProcessDefinition.parseXmlString("<process-definition/>");
    graphSession.saveProcessDefinition(processDefinition);
    processInstance = new ProcessInstance(processDefinition);
    jbpmContext.save(processInstance);
  }

  @Override
  protected void newTransaction() {
    super.newTransaction();
    executionContext = new ExecutionContext(processInstance.getRootToken());
  }

  public void testFirstTermPrevious() {
    expressionAssignmentHandler.expression = "previous";
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("bill", assignable.getActorId());
  }

  public void testFirstTermSwimlane() {
    expressionAssignmentHandler.expression = "swimlane(boss)";
    SwimlaneInstance swimlaneInstance = new SwimlaneInstance(new Swimlane("boss"));
    swimlaneInstance.setActorId("john");
    processInstance.getTaskMgmtInstance().addSwimlaneInstance(swimlaneInstance);
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("john", assignable.getActorId());
  }

  public void testFirstTermSwimlaneUnexisting() {
    expressionAssignmentHandler.expression = "swimlane(sillywoman)";
    try {
      expressionAssignmentHandler.assign(assignable, executionContext);
      fail("expected exception");
    }
    catch (ExpressionAssignmentException e) {
      // OK
    }
  }

  public void testFirstTermVariableString() {
    expressionAssignmentHandler.expression = "variable(actoridstringvariable)";
    processInstance.getContextInstance().setVariable("actoridstringvariable", "john");
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("john", assignable.getActorId());
  }

  public void testFirstTermVariableUser() {
    expressionAssignmentHandler.expression = "variable(uservariable)";
    User john = identitySession.getUserByName("john");
    processInstance.getContextInstance().setVariable("uservariable", john);
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("john", assignable.getActorId());
  }

  public void testFirstTermVariableGroup() {
    expressionAssignmentHandler.expression = "variable(groupvariable)";
    Group hellsangels = identitySession.getGroupByName("hellsangels");
    processInstance.getContextInstance().setVariable("groupvariable", hellsangels);
    expressionAssignmentHandler.assign(assignable, executionContext);
    Set<PooledActor> pooledActors = assignable.getPooledActors();
    PooledActor pooledActor = pooledActors.iterator().next();
    assertEquals("hellsangels", pooledActor.getActorId());
  }

  public void testFirstTermVariableUnexisting() {
    expressionAssignmentHandler.expression = "variable(unexistingvariablename)";
    try {
      expressionAssignmentHandler.assign(assignable, executionContext);
      fail("expected exception");
    }
    catch (ExpressionAssignmentException e) {
      // OK
    }
  }

  public void testFirstTermUser() {
    expressionAssignmentHandler.expression = "user(john)";
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("john", assignable.getActorId());
  }

  public void testFirstTermUserUnexisting() {
    expressionAssignmentHandler.expression = "user(idontexist)";
    try {
      expressionAssignmentHandler.assign(assignable, executionContext);
      fail("expected exception");
    }
    catch (ExpressionAssignmentException e) {
      // OK
    }
  }

  public void testFirstTermGroup() {
    expressionAssignmentHandler.expression = "group(hellsangels)";
    expressionAssignmentHandler.assign(assignable, executionContext);
    Set<PooledActor> pooledActors = assignable.getPooledActors();
    PooledActor pooledActor = pooledActors.iterator().next();
    assertEquals("hellsangels", pooledActor.getActorId());
  }

  public void testFirstTermGroupUnexisting() {
    expressionAssignmentHandler.expression = "group(wedontexist)";
    try {
      expressionAssignmentHandler.assign(assignable, executionContext);
      fail("expected exception");
    }
    catch (ExpressionAssignmentException e) {
      // OK
    }
  }

  public void testWrongFirstTerm() {
    expressionAssignmentHandler.expression = "wrong-first-term";
    try {
      expressionAssignmentHandler.assign(assignable, executionContext);
      fail("expected exception");
    }
    catch (ExpressionAssignmentException e) {
      // OK
    }
  }

  public void testNextTermGroup() {
    expressionAssignmentHandler.expression = "user(john) --> group(hierarchy)";
    expressionAssignmentHandler.assign(assignable, executionContext);
    Set<PooledActor> pooledActors = assignable.getPooledActors();
    PooledActor pooledActor = pooledActors.iterator().next();
    assertEquals("hellsangels", pooledActor.getActorId());
  }

  public void testNextTermMember() {
    expressionAssignmentHandler.expression = "group(hellsangels) --> member(leaderofthegang)";
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("john", assignable.getActorId());
  }

  public void testWrongNextTerm() {
    expressionAssignmentHandler.expression = "user(john) --> wrong-second-term";
    try {
      expressionAssignmentHandler.assign(assignable, executionContext);
      fail("expected exception");
    }
    catch (ExpressionAssignmentException e) {
      // OK
    }
  }
}
