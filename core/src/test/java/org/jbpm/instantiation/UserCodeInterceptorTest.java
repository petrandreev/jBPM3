package org.jbpm.instantiation;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class UserCodeInterceptorTest extends AbstractJbpmTestCase {

  static List logs = new ArrayList();

  private JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    super.setUp();
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
        + "  <jbpm-context />"
        + "  <bean name='jbpm.user.code.interceptor' class='"
        + TestInterceptor.class.getName()
        + "' singleton='true'/>"
        + "</jbpm-configuration>");
    jbpmContext = jbpmConfiguration.createJbpmContext();
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
    jbpmContext.getJbpmConfiguration().close();
    logs.clear();
    super.tearDown();
  }

  public void testAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <event type='node-leave'>"
        + "      <action name='takingthetransition' class='"
        + TestAction.class.getName()
        + "' />"
        + "    </event>"
        + "    <transition to='end'/>"
        + "  </start-state>"
        + "  <state name='end'/>"
        + "</process-definition>");
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List expectedLogs = new ArrayList();
    expectedLogs.add("before action takingthetransition in node start");
    expectedLogs.add("action executed");
    expectedLogs.add("after action takingthetransition in node start");

    assertEquals(expectedLogs, logs);
  }

  public void testInterceptControllerInitialization() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='t'/>"
        + "  </start-state>"
        + "  <task-node name='t'>"
        + "    <task>"
        + "      <controller class='"
        + TestController.class.getName()
        + "'/>"
        + "    </task>"
        + "    <transition to='end'/>"
        + "  </task-node>"
        + "  <state name='end' />"
        + "</process-definition>");
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List expectedLogs = new ArrayList();
    expectedLogs.add("before task controller initialization");
    expectedLogs.add("initializing task variables for TaskInstance(t)");
    expectedLogs.add("after task controller initialization");

    assertEquals(expectedLogs, logs);
  }

  public void testInterceptControllerSubmission() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='t'/>"
        + "  </start-state>"
        + "  <task-node name='t'>"
        + "    <task>"
        + "      <controller class='"
        + TestController.class.getName()
        + "'/>"
        + "    </task>"
        + "    <transition to='end'/>"
        + "  </task-node>"
        + "  <state name='end' />"
        + "</process-definition>");
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskInstance ti = (TaskInstance) processInstance.getTaskMgmtInstance()
        .getTaskInstances()
        .iterator()
        .next();
    ti.end();

    List expectedLogs = new ArrayList();
    expectedLogs.add("before task controller initialization");
    expectedLogs.add("initializing task variables for TaskInstance(t)");
    expectedLogs.add("after task controller initialization");
    expectedLogs.add("before task controller submission");
    expectedLogs.add("submitting task variables for TaskInstance(t)");
    expectedLogs.add("after task controller submission");

    assertEquals(expectedLogs.get(0), logs.get(0));
    assertEquals(expectedLogs.get(1), logs.get(1));
    assertEquals(expectedLogs.get(2), logs.get(2));
    assertEquals(expectedLogs.get(3), logs.get(3));
    assertEquals(expectedLogs.get(4), logs.get(4));
    assertEquals(expectedLogs.get(5), logs.get(5));
  }

  public void testNodeBehaviour() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='auto'/>"
        + "  </start-state>"
        + "  <node name='auto'>"
        + "    <action name='theautonode' class='"
        + TestAutoAction.class.getName()
        + "' />"
        + "    <transition to='end'/>"
        + "  </node>"
        + "  <state name='end' />"
        + "</process-definition>");
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List expectedLogs = new ArrayList();
    expectedLogs.add("before action theautonode in node auto");
    expectedLogs.add("auto action executed");
    expectedLogs.add("after action theautonode in node end");

    assertEquals(expectedLogs, logs);
  }

  public void testReferencedNodeBehaviour() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='auto'/>"
        + "  </start-state>"
        + "  <node name='auto'>"
        + "    <action name='reference' ref-name='referenced' />"
        + "    <transition to='end'/>"
        + "  </node>"
        + "  <state name='end' />"
        + "  <action name='referenced' class='"
        + TestAutoAction.class.getName()
        + "' />"
        + "</process-definition>");
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List expectedLogs = new ArrayList();
    expectedLogs.add("before action reference in node auto");
    expectedLogs.add("auto action executed");
    expectedLogs.add("after action reference in node end");

    assertEquals(expectedLogs, logs);
  }

  public void testReferencedAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='auto'/>"
        + "  </start-state>"
        + "  <node name='auto'>"
        + "    <event type='node-enter'>"
        + "      <action name='reference' ref-name='referenced' />"
        + "    </event>"
        + "    <transition to='end'/>"
        + "  </node>"
        + "  <state name='end' />"
        + "  <action name='referenced' class='"
        + TestAction.class.getName()
        + "' />"
        + "</process-definition>");
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List expectedLogs = new ArrayList();
    expectedLogs.add("before action reference in node auto");
    expectedLogs.add("action executed");
    expectedLogs.add("after action reference in node auto");

    assertEquals(expectedLogs, logs);
  }

  public void testInterceptAssignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='t'/>"
        + "  </start-state>"
        + "  <task-node name='t'>"
        + "    <task>"
        + "      <assignment class='"
        + TestAssignment.class.getName()
        + "' />"
        + "    </task>"
        + "    <transition to='end'/>"
        + "  </task-node>"
        + "  <state name='end' />"
        + "</process-definition>");
    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    List expectedLogs = new ArrayList();
    expectedLogs.add("before assignment");
    expectedLogs.add("assigning TaskInstance(t)");
    expectedLogs.add("after assignment");

    assertEquals(expectedLogs, logs);
  }

  public static class TestInterceptor implements UserCodeInterceptor {

    public void executeAction(Action action, ExecutionContext executionContext) throws Exception {
      logs.add("before action "
          + action.getName()
          + " in node "
          + executionContext.getNode().getName());
      action.execute(executionContext);
      logs.add("after action "
          + action.getName()
          + " in node "
          + executionContext.getNode().getName());
    }

    public void executeAssignment(AssignmentHandler assignmentHandler, Assignable assignable,
        ExecutionContext executionContext) throws Exception {
      logs.add("before assignment");
      assignmentHandler.assign(assignable, executionContext);
      logs.add("after assignment");
    }

    public void executeTaskControllerInitialization(TaskControllerHandler taskControllerHandler,
        TaskInstance taskInstance, ContextInstance contextInstance, Token token) {
      logs.add("before task controller initialization");
      taskControllerHandler.initializeTaskVariables(taskInstance, contextInstance, token);
      logs.add("after task controller initialization");
    }

    public void executeTaskControllerSubmission(TaskControllerHandler taskControllerHandler,
        TaskInstance taskInstance, ContextInstance contextInstance, Token token) {
      logs.add("before task controller submission");
      taskControllerHandler.submitTaskVariables(taskInstance, contextInstance, token);
      logs.add("after task controller submission");
    }
  }

  public static class TestAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      logs.add("action executed");
    }
  }

  public static class TestAutoAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      logs.add("auto action executed");
      executionContext.leaveNode();
    }
  }

  public static class TestAssignment implements AssignmentHandler {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
      logs.add("assigning " + assignable);
      assignable.setActorId("shipper");
    }
  }

  public static class TestController implements TaskControllerHandler {
    private static final long serialVersionUID = 1L;

    public void initializeTaskVariables(TaskInstance taskInstance, ContextInstance contextInstance,
        Token token) {
      logs.add("initializing task variables for " + taskInstance);
    }

    public void submitTaskVariables(TaskInstance taskInstance, ContextInstance contextInstance,
        Token token) {
      logs.add("submitting task variables for " + taskInstance);
    }
  }

}
