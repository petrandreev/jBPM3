package org.jbpm.taskmgmt.exe;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class TaskVariableAccessDbTest extends AbstractDbTestCase {

  /**
   * verifies task local variables.
   */
  public void testVarUpdate() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition"
      + "  name='panem et circenses'>"
      + "  <start-state name='start'>"
      + "    <transition to='f' />"
      + "  </start-state>"
      + "  <fork name='f'>"
      + "    <transition name='left'  to='panem' />"
      + "    <transition name='right' to='circenses' />"
      + "  </fork>"
      + "  <task-node name='panem'>"
      + "    <task name='bake bread'>"
      + "      <controller>"
      + "        <!-- empty access means this is a task-local variable -->"
      + "        <!-- with no counterpart in the process variables -->"
      + "        <variable name='hero' access='' />"
      + "      </controller>"
      + "    </task>"
      + "    <transition to='j' />"
      + "  </task-node>"
      + "  <task-node name='circenses'>"
      + "    <task name='play monopoly'>"
      + "      <controller>"
      + "        <variable name='hero' access='' />"
      + "      </controller>"
      + "    </task>"
      + "    <transition to='j' />"
      + "  </task-node>"
      + "  <join name='j'>"
      + "    <transition to='end' />"
      + "  </join>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("panem et circenses");
    processInstance.signal();

    newTransaction();
    TaskInstance breadTaskInstance = findTask("bake bread");
    breadTaskInstance.setVariable("hero", "asterix");
    jbpmContext.save(breadTaskInstance);
    long breadTokenId = breadTaskInstance.getToken().getId();

    newTransaction();
    Token breadToken = jbpmContext.loadToken(breadTokenId);
    ContextInstance contextInstance = breadToken.getProcessInstance().getContextInstance();
    assertNull(contextInstance.getVariable("hero", breadToken));

    newTransaction();
    breadTaskInstance = jbpmContext.loadTaskInstance(breadTaskInstance.getId());
    assertEquals("asterix", breadTaskInstance.getVariable("hero"));

    newTransaction();
    TaskInstance monopolyTaskInstance = findTask("play monopoly");
    monopolyTaskInstance.setVariable("hero", "obelix");
    jbpmContext.save(monopolyTaskInstance);
    long monopolyTokenId = monopolyTaskInstance.getToken().getId();

    newTransaction();
    monopolyTaskInstance = jbpmContext.loadTaskInstance(monopolyTaskInstance.getId());
    assertEquals("obelix", monopolyTaskInstance.getVariable("hero"));

    newTransaction();
    Token monopolyToken = jbpmContext.loadToken(monopolyTokenId);
    contextInstance = monopolyToken.getProcessInstance().getContextInstance();
    assertNull(contextInstance.getVariable("hero", monopolyToken));

    newTransaction();
    breadTaskInstance = jbpmContext.loadTaskInstanceForUpdate(breadTaskInstance.getId());
    breadTaskInstance.end();

    newTransaction();
    monopolyTaskInstance = jbpmContext.loadTaskInstanceForUpdate(monopolyTaskInstance.getId());
    monopolyTaskInstance.end();

    newTransaction();
    breadToken = jbpmContext.loadToken(breadTokenId);
    monopolyToken = jbpmContext.loadToken(monopolyTokenId);
    contextInstance = breadToken.getProcessInstance().getContextInstance();
    assertNull(contextInstance.getVariable("hero", breadToken));
    assertNull(contextInstance.getVariable("hero", monopolyToken));
  }

  private TaskInstance findTask(String taskName) {
    Session session = jbpmContext.getSession();
    Criteria criteria = session.createCriteria(TaskInstance.class);
    criteria.add(Restrictions.eq("name", taskName));
    List taskInstances = criteria.list();
    return (TaskInstance) taskInstances.get(0);
  }
}
