package org.jbpm.examples.raise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class RaiseRequestTest extends AbstractDbTestCase {

  private long processDefinitionId;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    deployProcess();
  }

  @Override
  protected void tearDown() throws Exception {
    graphSession.deleteProcessDefinition(processDefinitionId);
    super.tearDown();
  }

  @Override
  protected String getJbpmTestConfig() {
    return null; // use default configuration
  }

  void deployProcess() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("raise/processdefinition.xml");
    jbpmContext.deployProcessDefinition(processDefinition);
    processDefinitionId = processDefinition.getId();
  }

  TaskInstance createNewProcessInstance() {
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("raise request");
    return processInstance.getTaskMgmtInstance().createStartTaskInstance();
  }

  public void testTaskParameters() {
    TaskInstance taskInstance = createNewProcessInstance();
    assertEquals("submit raise request", taskInstance.getName());
    assertEquals(0, taskInstance.getVariables().size());

    newTransaction();
  }

  public void testSubmitRaiseRequestTask() {
    jbpmContext.setActorId("employee");
    TaskInstance taskInstance = createNewProcessInstance();

    Map<String, Object> taskVariables = new HashMap<String, Object>();
    taskVariables.put("reason", "I need to buy a jet");
    taskVariables.put("amount", 600);

    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    newTransaction();
    ProcessInstance processInstance = jbpmContext.loadProcessInstance(taskInstance.getProcessInstance()
        .getId());
    ContextInstance contextInstance = processInstance.getContextInstance();
    assertEquals("I need to buy a jet", contextInstance.getVariable("reason"));
    assertEquals(600, contextInstance.getVariable("amount"));

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals("employee", taskMgmtInstance.getSwimlaneInstance("employee").getActorId());
  }

  public void testManagerEvaluationReject() {
    TaskInstance taskInstance = createNewProcessInstance();
    Map<String, Object> taskVariables = new HashMap<String, Object>();
    taskVariables.put("reason", "I need to buy a jet");
    taskVariables.put("amount", 600);
    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    newTransaction();
    List<TaskInstance> managerTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("manager");
    assertEquals(1, managerTasks.size());

    TaskInstance managerTask = managerTasks.get(0);
    managerTask.end("reject");

    List<TaskInstance> foTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("fo");
    assertEquals(0, foTasks.size());
  }

  public void testManagerEvaluationAcceptFOReject() {
    TaskInstance taskInstance = createNewProcessInstance();
    Map<String, Object> taskVariables = new HashMap<String, Object>();
    taskVariables.put("reason", "I need to buy a jet");
    taskVariables.put("amount", 600);
    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    newTransaction();
    List<TaskInstance> managerTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("manager");
    assertEquals(1, managerTasks.size());
    TaskInstance managerTask = managerTasks.get(0);
    managerTask.start();
    managerTask.end("accept");

    newTransaction();
    List<TaskInstance> foTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("fo");
    assertEquals(1, foTasks.size());

    TaskInstance foTask = foTasks.get(0);
    foTask.start();
    foTask.addComment("Justify two consecutive raises");
    foTask.end("reject");

    managerTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("manager");
    assertEquals(0, managerTasks.size());
  }

  public void testManagerEvaluationAcceptFOAccpet() {
    TaskInstance taskInstance = createNewProcessInstance();
    Map<String, Object> taskVariables = new HashMap<String, Object>();
    taskVariables.put("reason", "I need to buy a jet");
    taskVariables.put("amount", 600);
    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    newTransaction();
    List<TaskInstance> managerTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("manager");
    assertEquals(1, managerTasks.size());
    TaskInstance managerTask = managerTasks.get(0);
    managerTask.start();
    managerTask.end("accept");

    newTransaction();
    List<TaskInstance> foTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("fo");
    assertEquals(1, foTasks.size());

    TaskInstance foTask = foTasks.get(0);
    foTask.start();
    foTask.addComment("Justify two consecutive raises");
    foTask.end("accept");

    newTransaction();
    List<TaskInstance> accountantTasks = jbpmContext.getTaskMgmtSession().findTaskInstances(
        "accountant");
    assertEquals(1, accountantTasks.size());

    TaskInstance accountantTask = accountantTasks.get(0);
    accountantTask.start();
    accountantTask.addComment("ERP updated");
    accountantTask.end("terminate");
  }

  public void testManagerEvaluationAcceptFOMultipleIterationsAccpet() {
    TaskInstance taskInstance = createNewProcessInstance();
    Map<String, Object> taskVariables = new HashMap<String, Object>();
    taskVariables.put("reason", "I need to buy a jet");
    taskVariables.put("amount", 600);
    taskInstance.addVariables(taskVariables);
    taskInstance.end();

    newTransaction();
    List<TaskInstance> managerTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("manager");
    assertEquals(1, managerTasks.size());
    TaskInstance managerTask = managerTasks.get(0);
    managerTask.start();
    managerTask.end("accept");

    newTransaction();
    List<TaskInstance> foTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("fo");
    assertEquals(1, foTasks.size());

    TaskInstance foTask = foTasks.get(0);
    foTask.start();
    foTask.addComment("Justify two consecutive raises");
    foTask.end("more justification required");

    newTransaction();
    managerTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("manager");
    assertEquals(1, managerTasks.size());

    managerTask = managerTasks.get(0);
    managerTask.start();
    managerTask.addComment("The guy exceeds all the expectations");
    managerTask.end("accept");

    newTransaction();
    foTasks = jbpmContext.getTaskMgmtSession().findTaskInstances("fo");
    assertEquals(1, foTasks.size());

    foTask = foTasks.get(0);
    foTask.start();
    foTask.addComment("justification accepted");
    foTask.end("accept");

    newTransaction();
    List<TaskInstance> accountantTasks = jbpmContext.getTaskMgmtSession().findTaskInstances(
        "accountant");
    assertEquals(1, accountantTasks.size());

    TaskInstance accountantTask = accountantTasks.get(0);
    accountantTask.start();
    accountantTask.addComment("ERP updated");
    accountantTask.end("terminate");
  }
}
