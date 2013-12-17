package org.jbpm.scenarios;

import java.util.List;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class AsyncTimerAndSubProcessDbTest extends AbstractDbTestCase {

  public static class TimedDecisionHandler implements DecisionHandler {
    private static final long serialVersionUID = 1L;

    public String decide(ExecutionContext executionContext) throws Exception {
      return "default";
    }
  }

  public void testTimerInCombinationWithAsyncNode() throws Throwable {
    ProcessDefinition subDefinition = ProcessDefinition.parseXmlString("<process-definition name='sub'>"
      + "  <start-state name='start'>"
      + "    <transition to='decision'/>"
      + "  </start-state>"
      + "  <decision name='decision'>"
      + "    <handler class='"
      + TimedDecisionHandler.class.getName()
      + "' />"
      + "    <transition name='default' to='task' />"
      + "  </decision>"
      + "  <task-node name='task'>"
      + "    <task name='do stuff'>"
      + "      <controller>"
      + "        <variable name='a' access='read' />"
      + "      </controller>"
      + "      <assignment actor-id='victim' />"
      + "    </task>"
      + "    <transition to='end'/>"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(subDefinition);

    ProcessDefinition superDefinition = ProcessDefinition.parseXmlString("<process-definition name='super'>"
      + "  <start-state name='start'>"
      + "    <transition to='decision'/>"
      + "  </start-state>"
      + "  <decision name='decision'>"
      + "    <handler class='"
      + TimedDecisionHandler.class.getName()
      + "' />"
      + "    <transition name='default' to='timed' />"
      + "  </decision>"
      + "  <state name='timed'>"
      + "    <timer name='reminder' "
      + "           duedate='0 seconds' "
      + "           transition='timer fires' />"
      + "    <transition name='timer fires' to='async'/>"
      + "    <transition name='normal continuation' to='end'/>"
      + "  </state>"
      + "  <node name='async' async='true'>"
      + "    <transition to='subprocess'/>"
      + "  </node>"
      + "  <process-state name='subprocess'>"
      + "    <sub-process name='sub' />"
      + "    <variable name='a'/>"
      + "    <variable name='b'/>"
      + "    <transition to='decision' />"
      + "  </process-state>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(superDefinition);

    ProcessInstance superInstance = jbpmContext.newProcessInstanceForUpdate("super");
    ContextInstance superContext = superInstance.getContextInstance();
    superContext.setVariable("a", "value a");
    superContext.setVariable("b", "value b");
    superInstance.signal();

    processJobs();

    superInstance = jbpmContext.loadProcessInstance(superInstance.getId());
    assertEquals("subprocess", superInstance.getRootToken().getNode().getName());

    List taskInstances = jbpmContext.getTaskList("victim");
    assertEquals(1, taskInstances.size());
    TaskInstance taskInstance = (TaskInstance) taskInstances.get(0);
    taskInstance.setVariable("a", "value a updated");
    taskInstance.setVariable("b", "value b updated");
    taskInstance.end();

    jbpmContext.save(taskInstance);
    long taskInstanceId = taskInstance.getId();
    long tokenId = taskInstance.getToken().getId();

    newTransaction();
    taskInstance = jbpmContext.loadTaskInstance(taskInstanceId);
    assertEquals("value a updated", taskInstance.getVariable("a"));
    assertEquals("value b updated", taskInstance.getVariable("b"));

    Token token = jbpmContext.loadToken(tokenId);
    ContextInstance subContextInstance = token.getProcessInstance().getContextInstance();
    assertEquals("value a", subContextInstance.getVariable("a"));
    assertEquals("value b updated", subContextInstance.getVariable("b"));
  }
}
