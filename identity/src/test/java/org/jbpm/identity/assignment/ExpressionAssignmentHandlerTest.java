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

import java.util.Iterator;
import java.util.Set;

import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.context.log.VariableLog;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.identity.Group;
import org.jbpm.identity.IdentityDbTestCase;
import org.jbpm.identity.Membership;
import org.jbpm.identity.User;
import org.jbpm.logging.exe.LoggingInstance;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.taskmgmt.def.Swimlane;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class ExpressionAssignmentHandlerTest extends IdentityDbTestCase {

  ExpressionAssignmentHandler expressionAssignmentHandler;
  TaskInstance assignable = new TaskInstance();
  ProcessInstance processInstance;
  ExecutionContext executionContext;

  Membership membershipJohn, membershipBill;
  Group hellsangels;
  User john, bill;

  public void setUp() throws Exception {
    super.setUp();

    expressionAssignmentHandler = new ExpressionAssignmentHandler();
    setUpProcessInstance();
    setUpUserData();
    jbpmContext.setActorId("bill");
  }

  protected void tearDown() throws Exception {
    identitySession.deleteEntity(hellsangels);
    identitySession.deleteEntity(john);
    identitySession.deleteEntity(bill);

    super.tearDown();
  }

  private void setUpUserData() {
    john = new User("john");
    bill = new User("bill");
    hellsangels = new Group("hellsangels", "hierarchy");
    membershipJohn = Membership.create(john, "leaderofthegang", hellsangels);
    membershipBill = Membership.create(bill, hellsangels);

    identitySession.saveEntity(john);
    identitySession.saveEntity(bill);
    identitySession.saveEntity(hellsangels);
  }

  private void setUpProcessInstance() {
    TaskMgmtDefinition taskMgmtDefinition = new TaskMgmtDefinition();
    taskMgmtDefinition.addSwimlane(new Swimlane("boss"));

    ProcessDefinition processDefinition = new ProcessDefinition("exp");
    processDefinition.addDefinition(taskMgmtDefinition);
    processDefinition.addDefinition(new ContextDefinition());
    deployProcessDefinition(processDefinition);

    processInstance = jbpmContext.newProcessInstanceForUpdate("exp");
    executionContext = new ExecutionContext(processInstance.getRootToken());
  }

  public void testFirstTermPrevious() {
    expressionAssignmentHandler.expression = "previous";
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("bill", assignable.getActorId());
  }

  public void testFirstTermSwimlane() {
    SwimlaneInstance swimlaneInstance = processInstance.getTaskMgmtInstance()
      .createSwimlaneInstance("boss");
    swimlaneInstance.setActorId("john");

    expressionAssignmentHandler.expression = "swimlane(boss)";
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
    processInstance.getContextInstance().setVariable("actoridstringvariable", "john");

    expressionAssignmentHandler.expression = "variable(actoridstringvariable)";
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("john", assignable.getActorId());
  }

  public void testFirstTermVariableUser() {
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("uservariable", john);

    expressionAssignmentHandler.expression = "variable(uservariable)";
    expressionAssignmentHandler.assign(assignable, executionContext);
    assertEquals("john", assignable.getActorId());

    contextInstance.deleteVariable("uservariable");
    deleteLogs(processInstance.getLoggingInstance(), VariableLog.class);
  }

  public void testFirstTermVariableGroup() {
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("groupvariable", hellsangels);

    expressionAssignmentHandler.expression = "variable(groupvariable)";
    expressionAssignmentHandler.assign(assignable, executionContext);

    Set pooledActors = assignable.getPooledActors();
    PooledActor pooledActor = (PooledActor) pooledActors.iterator().next();
    assertEquals("hellsangels", pooledActor.getActorId());

    contextInstance.deleteVariable("groupvariable");
    deleteLogs(processInstance.getLoggingInstance(), VariableLog.class);
  }

  private static void deleteLogs(LoggingInstance loggingInstance, Class logClass) {
    for (Iterator i = loggingInstance.getLogs().iterator(); i.hasNext();) {
      ProcessLog processLog = (ProcessLog) i.next();
      if (logClass.isInstance(processLog)) i.remove();
    }
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

    Set pooledActors = assignable.getPooledActors();
    PooledActor pooledActor = (PooledActor) pooledActors.iterator().next();
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

    Set pooledActors = assignable.getPooledActors();
    PooledActor pooledActor = (PooledActor) pooledActors.iterator().next();
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
