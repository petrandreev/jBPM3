package org.jbpm.examples.businesstrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class BusinessTripRequestTest extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    deployProcess();
  }

  void deployProcess() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("businesstrip/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  TaskInstance createNewProcessInstance() {
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("business trip request");
    return processInstance.getTaskMgmtInstance().createStartTaskInstance();
  }

  public void testTaskParameters() {
    TaskInstance taskInstance = createNewProcessInstance();
    assertEquals("Submit trip request", taskInstance.getName());
    assertEquals(0, taskInstance.getVariables().size());

    newTransaction();
  }

  public void testSubmitRaiseRequestTask() {
    jbpmContext.setActorId("employee");
    TaskInstance taskInstance = createNewProcessInstance();

    Map taskVariables = new HashMap();

    taskVariables.put("purpose", "Conference in MIT");
    taskVariables.put("description", "Highlight the impact of ESB technologies");
    taskVariables.put("allocated budget", "3000");
    taskVariables.put("start date", "8/12/2009");
    taskVariables.put("end date", "8/21/2009");
    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    newTransaction();
    ProcessInstance processInstance = jbpmContext.loadProcessInstance(taskInstance.getProcessInstance()
        .getId());
    ContextInstance contextInstance = processInstance.getContextInstance();
    assertEquals("Conference in MIT", contextInstance.getVariable("purpose"));
    assertEquals("Highlight the impact of ESB technologies", contextInstance.getVariable("description"));
    assertEquals("3000", contextInstance.getVariable("allocated budget"));
    assertEquals("8/12/2009", contextInstance.getVariable("start date"));
    assertEquals("8/21/2009", contextInstance.getVariable("end date"));

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals("employee", taskMgmtInstance.getSwimlaneInstance("employee").getActorId());
  }

  public void testRejectBusinessTripRequest() {
    // Employee submits a business trip request
    jbpmContext.setActorId("employee");
    TaskInstance taskInstance = createNewProcessInstance();

    Map taskVariables = new HashMap();
    taskVariables.put("purpose", "Conference in MIT");
    taskVariables.put("description", "Highlight the impact of ESB technologies");
    taskVariables.put("allocated budget", "3000");
    taskVariables.put("start date", "8/12/2009");
    taskVariables.put("end date", "8/21/2009");

    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    // Manager rejects the raise request
    newTransaction();
    List managerTasksList = jbpmContext.getTaskList("manager");
    assertEquals(1, managerTasksList.size());

    TaskInstance managerTask = (TaskInstance) managerTasksList.get(0);
    managerTask.addComment("Conference theme doesn't align with company's current focus");
    managerTask.end("reject");
    assertEquals("manager", managerTask.getActorId());
  }

  public void testAcceptBusinessTripRequest() {
    // Employee submits a raise request
    jbpmContext.setActorId("employee");
    TaskInstance taskInstance = createNewProcessInstance();

    Map taskVariables = new HashMap();
    taskVariables.put("purpose", "Conference in MIT");
    taskVariables.put("description", "Highlight the impact of ESB technologies");
    taskVariables.put("allocated budget", "3000");
    taskVariables.put("start date", "8/12/2009");
    taskVariables.put("end date", "8/21/2009");
    taskVariables.put("country", "USA");
    taskVariables.put("city", "Kansas");
    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    // Manager rejects the raise request
    newTransaction();
    List managerTasksList = jbpmContext.getTaskList("manager");
    assertEquals(1, managerTasksList.size());

    TaskInstance managerTask = (TaskInstance) managerTasksList.get(0);
    managerTask.addComment("Business trip approved");
    managerTask.end("approve");
    assertEquals("manager", managerTask.getActorId());
    jbpmContext.save(managerTask);

    newTransaction();
    List accountantTasksList = jbpmContext.getTaskList("admin");
    assertEquals(1, accountantTasksList.size());
    TaskInstance accountantTask = (TaskInstance) accountantTasksList.get(0);
    accountantTask.end();
    jbpmContext.save(accountantTask);
  }
}
