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
package org.jbpm.graph.node;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.action.Script;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.Delegation;
import org.jbpm.taskmgmt.def.Task;

public class JpdlDbTest extends AbstractDbTestCase {

  public void testDecision() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <decision name='d'>"
      + "    <transition name='one' to='d'>"
      + "      <condition>a == 1</condition>"
      + "    </transition>"
      + "    <transition name='two' to='d'>"
      + "      <condition>a == 2</condition>"
      + "    </transition>"
      + "    <transition name='three' to='d'>"
      + "      <condition>a == 3</condition>"
      + "    </transition>"
      + "  </decision>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Decision decision = (Decision) processDefinition.getNode("d");
    assertEquals("a == 1", decision.getLeavingTransition("one").getCondition());
    assertEquals("a == 2", decision.getLeavingTransition("two").getCondition());
    assertEquals("a == 3", decision.getLeavingTransition("three").getCondition());
  }

  public static class MyDecisionHandler implements DecisionHandler {
    private static final long serialVersionUID = 1L;
    String decisionHandlerConfigText;

    public String decide(ExecutionContext executionContext) throws Exception {
      return "two";
    }
  }

  public void testDecisionDelegation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <decision name='d' >"
      + "    <handler class='"
      + MyDecisionHandler.class.getName()
      + "'>"
      + "      <decisionHandlerConfigText>testing... one, two... testing</decisionHandlerConfigText>"
      + "    </handler>"
      + "    <transition name='one' to='d'/>"
      + "    <transition name='two' to='d'/>"
      + "    <transition name='three' to='d'/>"
      + "  </decision>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Decision decision = (Decision) processDefinition.getNode("d");
    Delegation decisionDelegation = decision.getDecisionDelegation();
    assertEquals(MyDecisionHandler.class.getName(), decisionDelegation.getClassName());
    assertEquals("<decisionHandlerConfigText>testing... one, two... testing</decisionHandlerConfigText>", decisionDelegation.getConfiguration());
  }

  public void testFork() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <fork name='f' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertSame(Fork.class, processDefinition.getNode("f").getClass());
  }

  public void testJoin() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <join name='j' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertSame(Join.class, processDefinition.getNode("j").getClass());
  }

  public void testScript() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <script name='s'>"
      + "    <variable name='a' access='read' />"
      + "    <variable name='b' access='read-write' />"
      + "    <variable name='c' access='read-write' />"
      + "    <variable name='d' access='read-write-required' />"
      + "    <expression>e = m * Math.pow(c,2);</expression>"
      + "  </script>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Script script = (Script) processDefinition.getAction("s");
    assertEquals(4, script.getVariableAccesses().size());
    assertEquals("e = m * Math.pow(c,2);", script.getExpression());
  }

  public void testState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <state name='s' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertSame(State.class, processDefinition.getNode("s").getClass());
  }

  public void testTaskNode() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <task-node name='t' signal='first-wait' create-tasks='false'>"
      + "    <task name='change the world once' blocking='true'>"
      + "      <assignment class='anyonebutme' />"
      + "    </task>"
      + "    <task name='change the world twice' />"
      + "    <task name='change the world three times' />"
      + "  </task-node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    TaskNode taskNode = (TaskNode) processDefinition.getNode("t");
    assertNotNull(taskNode);
    assertEquals("t", taskNode.getName());
    assertEquals(TaskNode.SIGNAL_FIRST_WAIT, taskNode.getSignal());
    assertFalse(taskNode.getCreateTasks());
    assertEquals(3, taskNode.getTasks().size());

    Map tasks = new HashMap();
    for (Iterator iter = taskNode.getTasks().iterator(); iter.hasNext();) {
      Task task = (Task) iter.next();
      tasks.put(task.getName(), task);
    }
    Task task = (Task) tasks.get("change the world once");
    assertNotNull(task);
    assertSame(taskNode, task.getTaskNode());
    assertTrue(task.isBlocking());
    assertEquals("anyonebutme", task.getAssignmentDelegation().getClassName());

    task = (Task) tasks.get("change the world twice");
    assertNotNull(task);
    assertSame(taskNode, task.getTaskNode());
    assertFalse(task.isBlocking());

    assertTrue(tasks.containsKey("change the world three times"));
  }

  public void testNoAccessToObsoleteDecisionConditionTable() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + " <start-state>"
      + " <transition to='d' />"
      + " </start-state>"
      + " <decision name='d'>"
      + " <transition name='one' to='a'>"
      + " <condition>#{a == 1}</condition>"
      + " </transition>"
      + " <transition name='two' to='b'>"
      + " <condition>#{a == 2}</condition>"
      + " </transition>"
      + " <transition name='three' to='c'>"
      + " <condition>#{a == 3}</condition>"
      + " </transition>"
      + " </decision>"
      + " <state name='a' />"
      + " <state name='b' />"
      + " <state name='c' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Decision decision = (Decision) processDefinition.getNode("d");
    assertEquals("#{a == 1}", decision.getLeavingTransition("one").getCondition());
    assertEquals("#{a == 2}", decision.getLeavingTransition("two").getCondition());
    assertEquals("#{a == 3}", decision.getLeavingTransition("three").getCondition());

    // Assure org.jbpm.graph.node.Decision#execute gets the conditions from
    // table JBPM_TRANSITIONS rather than the obsolete JBPM_DECISIONCONDITION:
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("a", new Integer(2));
    processInstance.signal();
    assertEquals(processDefinition.getNode("b"), processInstance.getRootToken().getNode());
  }
}
