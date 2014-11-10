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
package org.jbpm.jpdl.patterns;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_14.swf
 */
public class Wfp15MiWithAPrioriRuntimeKnowledgeTest extends AbstractJbpmTestCase {

  public static int scenario = -1;

  public static class CreateTasks implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      TaskMgmtDefinition tmd = (TaskMgmtDefinition) executionContext.getDefinition(TaskMgmtDefinition.class);
      Task task = tmd.getTask("watch movie amadeus");

      // create as many task instances as the scenario prescribes :
      // 0 tasks for scenario 1
      // 1 task for scenario 2
      // 2 tasks for scenario 3
      // 3 tasks for scenario 4
      TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();
      for (int i = 1; i < scenario; i++) {
        tmi.createTaskInstance(task, executionContext.getToken());
      }
    }
  }

  public static ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
    + "  <start-state name='a'>"
    + "    <transition to='b' />"
    + "  </start-state>"
    + "  <state name='b'>"
    + "    <transition to='t' />"
    + "  </state>"
    + "  <task-node name='t' create-tasks='false'>"
    + "    <event type='node-enter'>"
    + "      <action class='org.jbpm.jpdl.patterns.Wfp15MiWithAPrioriRuntimeKnowledgeTest$CreateTasks' />"
    + "    </event>"
    + "    <task name='watch movie amadeus' />"
    + "    <transition to='c' />"
    + "  </task-node>"
    + "  <state name='c' />"
    + "</process-definition>");

  public static Node t = processDefinition.getNode("t");
  public static Node c = processDefinition.getNode("c");

  public void testAprioriRuntimeKnowledgeScenario1() {
    scenario = 1;

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    processInstance.signal();
    assertSame(c, token.getNode());
  }

  public void testAprioriRuntimeKnowledgeScenario2() {
    scenario = 2;

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    processInstance.signal();
    assertSame(t, token.getNode());

    endOneTask(token);
    assertSame(c, token.getNode());
  }

  public void testAprioriRuntimeKnowledgeScenario3() {
    scenario = 3;

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    processInstance.signal();
    assertSame(t, token.getNode());

    endOneTask(token);
    assertSame(t, token.getNode());

    endOneTask(token);
    assertSame(c, token.getNode());
  }

  public void testAprioriRuntimeKnowledgeScenario4() {
    scenario = 4;

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();
    processInstance.signal();
    assertSame(t, token.getNode());

    endOneTask(token);
    assertSame(t, token.getNode());

    endOneTask(token);
    assertSame(t, token.getNode());

    endOneTask(token);
    assertSame(c, token.getNode());
  }

  public static void endOneTask(Token token) {
    TaskMgmtInstance tmi = token.getProcessInstance().getTaskMgmtInstance();
    TaskInstance taskInstance = (TaskInstance) tmi.getUnfinishedTasks(token).iterator().next();
    taskInstance.end();
  }
}
