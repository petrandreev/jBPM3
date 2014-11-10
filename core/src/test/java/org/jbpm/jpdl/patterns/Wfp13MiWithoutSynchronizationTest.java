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
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_12.swf actually this
 * is a combination of unsynchronized tasks and runtime calculation of the
 * tasks.
 */
public class Wfp13MiWithoutSynchronizationTest extends AbstractJbpmTestCase {

  private ProcessDefinition pd;
  private ProcessInstance pi;

  static int scenario;

  public static class CreateTasks implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      // this piece of code is executed at runtime
      TaskMgmtDefinition taskMgmtDefinition = (TaskMgmtDefinition) executionContext.getDefinition(TaskMgmtDefinition.class);
      Task task = taskMgmtDefinition.getTask("undress");

      TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();
      for (int i = 1; i < scenario; i++) {
        tmi.createTaskInstance(task, executionContext.getToken());
      }
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    pd = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <start-state name='a'>"
      + "    <transition to='b' />"
      + "  </start-state>"
      + "  <node name='b'>"
      + "    <event type='node-enter'>"
      + "      <action class='org.jbpm.jpdl.patterns.Wfp13MiWithoutSynchronizationTest$CreateTasks'/>"
      + "    </event>"
      + "    <transition to='c' />"
      + "  </node>"
      + "  <state name='c' />"
      + "  <task name='undress' />"
      + "</process-definition>");

    pi = new ProcessInstance(pd);
  }

  public void testSituation1() {
    scenario = 1;
    pi.signal();
    assertNbrOfTasks(0);
  }

  public void testSituation2() {
    scenario = 2;
    pi.signal();
    assertNbrOfTasks(1);
  }

  public void testSituation3() {
    scenario = 3;
    pi.signal();
    assertNbrOfTasks(2);
  }

  public void testSituation4() {
    scenario = 4;
    pi.signal();
    assertNbrOfTasks(3);
  }

  private void assertNbrOfTasks(int nbrOfTasks) {
    TaskMgmtInstance taskMgmtInstance = pi.getTaskMgmtInstance();
    Token token = pi.getRootToken();

    assertEquals(nbrOfTasks, taskMgmtInstance.getUnfinishedTasks(token).size());
    assertSame(pd.getNode("c"), token.getNode());
  }
}
