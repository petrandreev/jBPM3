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
package org.jbpm.examples.taskinstance;

import java.util.Iterator;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.jbpm.JbpmConfiguration;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JbpmSchema;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * This example shows how to extend the TaskInstance by adding a custom
 * property.
 */
public class CustomTaskInstanceTest extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    deployProcess();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      // the jbpm.cfg.xml file is modified to add the CustomTaskInstanceFactory
      // so we will read in the file from the config directory of this example
      jbpmConfiguration = JbpmConfiguration.parseResource("taskinstance/jbpm.cfg.xml");
      DbPersistenceServiceFactory factory = (DbPersistenceServiceFactory) jbpmConfiguration.getServiceFactory(Services.SERVICENAME_PERSISTENCE);

      Configuration configuration = factory.getConfiguration();
      configuration.addResource("taskinstance/CustomTaskInstance.hbm.xml");

      JbpmSchema jbpmSchema = new JbpmSchema(configuration);
      jbpmSchema.updateTable("JBPM_TASKINSTANCE");
    }
    return jbpmConfiguration;
  }

  void deployProcess() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("taskinstance/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  public void testCustomTaskInstance() {
    // create processInstance
    newTransaction();
    long processInstanceId = createNewProcessInstance();
    assertFalse("ProcessInstanceId is 0", processInstanceId == 0);

    // perform the task
    newTransaction();
    long taskInstanceId = acquireTask();
    assertFalse("TaskInstanceId is 0", taskInstanceId == 0);
    newTransaction();

    completeTask(taskInstanceId);

    newTransaction();
    TaskInstance taskInstance = jbpmContext.loadTaskInstance(taskInstanceId);
    assertTrue("TaskInstance has not ended", taskInstance.hasEnded());

    // check process is completed
    newTransaction();
    ProcessInstance processInstance = jbpmContext.getProcessInstance(processInstanceId);
    assertTrue("ProcessInstance has not ended", processInstance.hasEnded());
  }

  long createNewProcessInstance() {
    String processDefinitionName = "CustomTaskInstance";
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(processDefinitionName);

    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("processDefinitionName", processDefinitionName);
    contextInstance.setVariable("customId", "abc");

    processInstance.signal();
    return processInstance.getId();
  }

  long acquireTask() {
    List taskList = findPooledTaskListByCustomId("reviewers", "abc");
    long taskInstanceId = 0;
    for (Iterator i = taskList.iterator(); i.hasNext();) {
      CustomTaskInstance taskInstance = (CustomTaskInstance) i.next();
      taskInstanceId = taskInstance.getId();
      taskInstance.start();
      taskInstance.setActorId("tom");
      String customId = taskInstance.getCustomId();
      assertEquals("abc", customId);
    }
    return taskInstanceId;
  }

  void completeTask(long taskInstanceId) {
    CustomTaskInstance taskInstance = (CustomTaskInstance) jbpmContext.getSession()
      .load(CustomTaskInstance.class, new Long(taskInstanceId));
    taskInstance.end();
  }

  List findPooledTaskListByCustomId(String actorId, String customId) {
    return jbpmContext.getSession()
      .createCriteria(CustomTaskInstance.class)
      .add(Restrictions.isNull("actorId"))
      .add(Restrictions.isNull("end"))
      .add(Restrictions.eq("isCancelled", Boolean.FALSE))
      .add(Restrictions.eq("customId", customId))
      .createCriteria("pooledActors")
      .add(Restrictions.eq("actorId", actorId))
      .list();
  }

}
