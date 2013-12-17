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
package org.jbpm.taskmgmt.exe;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;

public class TaskVariablesDbTest extends AbstractDbTestCase {

  public void testDefaultVariablePersistence() {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    processDefinition.setName("default variable persistence");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    TaskInstance taskInstance = processInstance.getTaskMgmtInstance().createTaskInstance(
      processInstance.getRootToken());
    taskInstance.setVariable("key", "value");

    taskInstance = saveAndReload(taskInstance);
    assertNotNull(taskInstance);
    assertEquals("value", taskInstance.getVariable("key"));
  }

  public void testSetOnTaskInstanceGetOnProcess() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='set on task get on process'>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='vartask' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    ContextInstance contextInstance = processInstance.getContextInstance();
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();

    Map expectedVariables = new HashMap();
    assertEquals(expectedVariables, taskInstance.getVariables());

    assertFalse(taskInstance.hasVariable("a"));
    assertNull(taskInstance.getVariable("a"));
    assertNull(contextInstance.getVariable("a"));

    taskInstance.setVariable("a", "1");

    taskInstance = saveAndReload(taskInstance);
    contextInstance = taskInstance.getContextInstance();

    expectedVariables.put("a", "1");
    assertEquals(expectedVariables, taskInstance.getVariables());

    assertTrue(taskInstance.hasVariable("a"));
    assertEquals("1", taskInstance.getVariable("a"));
    assertEquals("1", contextInstance.getVariable("a"));
  }

  public void testSetOnProcessGetOnTaskInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='set on process get on task'>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='vartask' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", "1");
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();

    taskInstance = saveAndReload(taskInstance);
    Map expectedVariables = new HashMap();
    expectedVariables.put("a", "1");
    assertEquals(expectedVariables, taskInstance.getVariables());

    taskInstance = saveAndReload(taskInstance);
    contextInstance = taskInstance.getContextInstance();

    assertTrue(taskInstance.hasVariable("a"));
    assertEquals("1", taskInstance.getVariable("a"));
    assertEquals("1", contextInstance.getVariable("a"));
  }

  public void testSetLocally() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='set locally'>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='vartask' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();

    taskInstance = saveAndReload(taskInstance);
    ContextInstance contextInstance = taskInstance.getContextInstance();

    Map expectedVariables = new HashMap();
    assertEquals(expectedVariables, taskInstance.getVariables());
    assertFalse(taskInstance.hasVariable("a"));
    assertNull(taskInstance.getVariable("a"));
    assertNull(contextInstance.getVariable("a"));

    taskInstance.setVariableLocally("a", "1");

    taskInstance = saveAndReload(taskInstance);
    contextInstance = taskInstance.getContextInstance();

    expectedVariables.put("a", "1");
    assertEquals(expectedVariables, taskInstance.getVariables());

    assertTrue(taskInstance.hasVariable("a"));
    assertEquals("1", taskInstance.getVariable("a"));
    assertNull(contextInstance.getVariable("a"));
  }

  public void testCopyWithController() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='copy with controller'>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='vartask'>"
      + "      <controller>"
      + "        <variable name='a' />"
      + "        <variable name='b' />"
      + "      </controller>"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", "1");
    contextInstance.setVariable("b", "2");
    contextInstance.setVariable("c", "3");

    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();

    taskInstance = saveAndReload(taskInstance);

    Map expectedVariables = new HashMap();
    expectedVariables.put("a", "1");
    expectedVariables.put("b", "2");
    expectedVariables.put("c", "3");
    assertEquals(expectedVariables, taskInstance.getVariables());

    taskInstance.setVariable("a", "1 modified");
    taskInstance.setVariable("b", "2 modified");
    taskInstance.setVariable("c", "3 modified");

    taskInstance = saveAndReload(taskInstance);

    expectedVariables.clear();
    expectedVariables.put("a", "1 modified");
    expectedVariables.put("b", "2 modified");
    expectedVariables.put("c", "3 modified");
    assertEquals(expectedVariables, taskInstance.getVariables());

    taskInstance = saveAndReload(taskInstance);
    contextInstance = taskInstance.getContextInstance();

    expectedVariables.clear();
    expectedVariables.put("a", "1"); // task instance had local copy for var a
    expectedVariables.put("b", "2"); // task instance had local copy for var b
    expectedVariables.put("c", "3 modified");
    assertEquals(expectedVariables, contextInstance.getVariables());
  }

  public void testOverwriteNullValue() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='overwrite-null-value'>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='vartask'>"
      + "      <controller>"
      + "        <variable name='v' />"
      + "      </controller>"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();

    assertNull(taskInstance.getVariable("v"));
    taskInstance.setVariable("v", "facelets is great");

    taskInstance = saveAndReload(taskInstance);
    assertEquals("facelets is great", taskInstance.getVariable("v"));
  }

  public void testNewTaskInstanceVariablesWithoutController() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='task variables without controller'>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t'>"
      + "    <task name='vartask' />"
      + "    <transition to='u' />"
      + "  </task-node>"
      + "  <state name='u' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();
    taskInstance.setVariableLocally("a", "value-a");
    taskInstance.setVariableLocally("b", "value-b");

    taskInstance = saveAndReload(taskInstance);
    ContextInstance contextInstance = taskInstance.getContextInstance();

    assertFalse(contextInstance.hasVariable("a"));
    assertFalse(contextInstance.hasVariable("b"));

    assertEquals("value-a", taskInstance.getVariable("a"));
    assertEquals("value-b", taskInstance.getVariable("b"));

    taskInstance.end();

    assertEquals("value-a", contextInstance.getVariable("a"));
    assertEquals("value-b", contextInstance.getVariable("b"));

    taskInstance = saveAndReload(taskInstance);
    contextInstance = taskInstance.getContextInstance();

    assertEquals("value-a", contextInstance.getVariable("a"));
    assertEquals("value-b", contextInstance.getVariable("b"));
  }

  public void testTaskVariableQuery() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='task-variable-query'>"
      + "  <start-state>"
      + "    <transition to='t' />"
      + "  </start-state>"
      + "  <task-node name='t' create-tasks='no'>"
      + "    <event type='node-enter'>"
      + "      <action class='"
      + CreateTasksAction.class.getName()
      + "'/>"
      + "    </event>"
      + "    <task name='vartask'>"
      + "      <assignment actor-id='me' />"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    newTransaction();
    assertEquals(5, jbpmContext.getTaskList("me").size());

    newTransaction();
    TaskInstance task = getTask("index", 3, "me");
    assertEquals(3, ((Long) task.getVariable("index")).longValue());
  }

  private TaskInstance getTask(String variableName, long variableValue, String actorId) {
    return (TaskInstance) jbpmContext.getSession().createCriteria(TaskInstance.class).add(
      Restrictions.eq("actorId", actorId)).createCriteria("variableInstances").add(
      Restrictions.eq("name", variableName)).add(
      Restrictions.sqlRestriction("{alias}.LONGVALUE_ = ?", new Long(variableValue),
        Hibernate.LONG)).uniqueResult();
  }

  public static final class CreateTasksAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      TaskNode taskNode = (TaskNode) executionContext.getNode();
      Task task = taskNode.getTask("vartask");
      TaskMgmtInstance taskMgmtInstance = executionContext.getTaskMgmtInstance();

      for (int index = 0; index < 5; index++) {
        TaskInstance ti = taskMgmtInstance.createTaskInstance(task, executionContext);
        ti.setVariableLocally("index", new Long(index));
      }
    }
  }
}
