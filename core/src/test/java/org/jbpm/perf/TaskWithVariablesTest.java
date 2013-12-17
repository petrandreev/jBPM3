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
package org.jbpm.perf;

import java.util.List;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class TaskWithVariablesTest extends AbstractDbTestCase {

  public static class ErnieAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext)
      throws Exception {
      assignable.setActorId("manager");
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='one' />"
      + "  </start-state>"
      + "  <task-node name='one'>"
      + "    <task name='one'>"
      + "      <assignment class='org.jbpm.perf.TaskWithVariablesTest$ErnieAssignmentHandler' />"
      + "    </task>"
      + "    <transition to='two' />"
      + "  </task-node>"
      + "  <task-node name='two'>"
      + "    <task name='two'>"
      + "      <assignment class='"
      + ErnieAssignmentHandler.class.getName()
      + "' />"
      + "    </task>"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);
  }

  public void testStates() {
    log.info("");
    log.info("=== CREATING PROCESS INSTANCE =======================================================");
    log.info("");
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(getName());
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("hotel", "best western");
    contextInstance.setVariable("city", "wengen");
    contextInstance.setVariable("ski conditions", "excellent");
    contextInstance.setVariable("slopes", "well prepared and sunny");
    contextInstance.setVariable("food", "just enough");
    processInstance.signal();

    newTransaction();

    log.info("");
    log.info("=== PERFORMING TASK ONE =======================================================");
    log.info("");
    List taskList = jbpmContext.getTaskList("manager");
    assertEquals(1, taskList.size());
    TaskInstance taskInstance = (TaskInstance) taskList.get(0);
    taskInstance.setVariable("item", "cookies");
    taskInstance.end();

    newTransaction();

    log.info("");
    log.info("=== PERFORMING TASK TWO =======================================================");
    log.info("");
    taskList = jbpmContext.getTaskList("manager");
    assertEquals(1, taskList.size());
    taskInstance = (TaskInstance) taskList.get(0);
    taskInstance.setVariable("delivery address", "829 maple street");
    taskInstance.end();
  }
}
