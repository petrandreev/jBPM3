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
package org.jbpm.msg.command;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class AsyncExecutionDbTest extends AbstractDbTestCase {

  public static class RecordNode implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Integer count = (Integer) executionContext.getVariable("count");
      count = new Integer(count != null ? count.intValue() + 1 : 1);
      executionContext.setVariable("count", count);
      executionContext.setVariable(executionContext.getNode().getName(), count);
      executionContext.leaveNode();
    }
  }

  public void testAsyncExecution() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='async exec'>"
      + "  <start-state>"
      + "    <transition to='one' />"
      + "  </start-state>"
      + "  <node async='true' name='one'>"
      + "    <action class='"
      + RecordNode.class.getName()
      + "' />"
      + "    <transition to='two' />"
      + "  </node>"
      + "  <node async='exclusive' name='two'>"
      + "    <action class='"
      + RecordNode.class.getName()
      + "' />"
      + "    <transition to='three' />"
      + "  </node>"
      + "  <node async='true' name='three'>"
      + "    <action class='"
      + RecordNode.class.getName()
      + "' />"
      + "    <transition to='end' />"
      + "  </node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    assertEquals("one", processInstance.getRootToken().getNode().getName());
    assertEquals(1, getNbrOfJobsAvailable());

    processJobs();

    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assertTrue(processInstance.hasEnded());

    ContextInstance contextInstance = processInstance.getContextInstance();
    String[] nodes = { "one", "two", "three" };
    for (int i = 0; i < nodes.length; i++) {
      Integer value = (Integer) contextInstance.getVariable(nodes[i]);
      assertEquals(i + 1, value.intValue());
    }
  }

  public static class RecordAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      executionContext.setVariable(executionContext.getAction().getName(), Boolean.TRUE);
    }
  }

  public void testAsyncAction() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='async action'>"
      + "  <event type='process-start'>"
      + "    <action name='sa' async='true' class='"
      + RecordAction.class.getName()
      + "'/>"
      + "    <action name='se' async='exclusive' class='"
      + RecordAction.class.getName()
      + "'/>"
      + "  </event>"
      + "  <start-state>"
      + "    <transition to='one'>"
      + "      <action name='ta' async='true' class='"
      + RecordAction.class.getName()
      + "'/>"
      + "      <action name='te' async='exclusive' class='"
      + RecordAction.class.getName()
      + "'/>"
      + "    </transition>"
      + "  </start-state>"
      + "  <node name='one'>"
      + "    <event type='node-enter'>"
      + "      <action name='na' async='true' class='"
      + RecordAction.class.getName()
      + "'/>"
      + "      <action name='ne' async='exclusive' class='"
      + RecordAction.class.getName()
      + "'/>"
      + "    </event>"
      + "    <transition to='end' />"
      + "  </node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    assertEquals("end", processInstance.getRootToken().getNode().getName());
    assertEquals(6, getNbrOfJobsAvailable());

    processJobs();

    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());

    ContextInstance contextInstance = processInstance.getContextInstance();
    String[] actions = { "sa", "se", "ta", "te", "na", "ne" };
    for (int i = 0; i < actions.length; i++) {
      assertEquals(Boolean.TRUE, contextInstance.getVariable(actions[i]));
    }
  }
}
