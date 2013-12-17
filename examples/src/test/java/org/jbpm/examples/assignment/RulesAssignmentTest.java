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
package org.jbpm.examples.assignment;

import java.util.List;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.identity.Entity;
import org.jbpm.identity.Membership;
import org.jbpm.identity.hibernate.IdentitySession;
import org.jbpm.identity.xml.IdentityXmlParser;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * This example shows how to invoke JBoss Rules from an AssignmentHandler.
 */
public class RulesAssignmentTest extends AbstractDbTestCase {

  private Entity[] entities;

  protected void setUp() throws Exception {
    super.setUp();
    deployProcess();
    loadIdentities();
  }

  protected void tearDown() throws Exception {
    deleteIdentities();
    super.tearDown();
  }

  void deployProcess() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("assignment/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  void loadIdentities() {
    IdentitySession identitySession = (IdentitySession) jbpmContext.getServices()
      .getPersistenceService()
      .getCustomSession(IdentitySession.class);
    entities = IdentityXmlParser.parseEntitiesResource("assignment/identity.db.xml");
    for (int i = 0; i < entities.length; i++) {
      Entity entity = entities[i];
      if (!(entity instanceof Membership)) identitySession.saveEntity(entity);
    }
  }

  void deleteIdentities() {
    IdentitySession identitySession = (IdentitySession) jbpmContext.getServices()
      .getPersistenceService()
      .getCustomSession(IdentitySession.class);
    for (int i = 0; i < entities.length; i++) {
      Entity entity = entities[i];
      if (!(entity instanceof Membership)) identitySession.deleteEntity(entity);
    }
  }

  public void testRulesAssignment() {
    // start process
    long processInstanceId = createNewProcessInstance();
    assertFalse("ProcessInstanceId is 0", processInstanceId == 0);

    // perform task
    newTransaction();
    long taskInstanceId = acquireTask("tom");
    assertFalse("TaskInstanceId is 0", taskInstanceId == 0);

    newTransaction();
    completeTask(taskInstanceId);

    newTransaction();
    TaskInstance taskInstance = jbpmContext.loadTaskInstance(taskInstanceId);
    assertTrue("TaskInstance has not ended", taskInstance.hasEnded());

    // complete process
    newTransaction();
    ProcessInstance processInstance = jbpmContext.loadProcessInstance(processInstanceId);
    assertTrue("ProcessInstance has not ended", processInstance.hasEnded());
  }

  long createNewProcessInstance() {
    String processDefinitionName = "RulesAssignment";
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(processDefinitionName);

    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("processDefinitionName", processDefinitionName);
    contextInstance.setVariable("order", new Order(300));

    processInstance.signal();
    return processInstance.getId();
  }

  long acquireTask(String actorId) {
    List taskList = jbpmContext.getTaskList(actorId);
    assertEquals(1, taskList.size());

    TaskInstance taskInstance = (TaskInstance) taskList.get(0);
    taskInstance.start();
    return taskInstance.getId();
  }

  void completeTask(long taskInstanceId) {
    TaskInstance taskInstance = jbpmContext.getTaskInstance(taskInstanceId);
    taskInstance.end();
  }

}
