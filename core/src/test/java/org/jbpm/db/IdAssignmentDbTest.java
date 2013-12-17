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
package org.jbpm.db;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class IdAssignmentDbTest extends AbstractDbTestCase {
  static int successfullInvocations = 0;

  protected void setUp() throws Exception {
    super.setUp();
    successfullInvocations = 0;
  }

  public static class TaskInstanceIdVerifier implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      if (executionContext.getTaskInstance().getId() == 0) {
        throw new RuntimeException("task instance didn't have an id");
      }
      successfullInvocations++;
    }
  }

  public void testTaskInstanceId() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <event type='task-create'>"
      + "    <action class='"
      + TaskInstanceIdVerifier.class.getName()
      + "' />"
      + "  </event>"
      + "  <start-state>"
      + "    <transition to='distribute work' />"
      + "  </start-state>"
      + "  <task-node name='distribute work'>"
      + "    <task name='negotiate a rebate' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    assertEquals(1, successfullInvocations);
  }

  public static class ProcessInstanceIdVerifier implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      if (executionContext.getProcessInstance().getId() == 0) {
        throw new RuntimeException("process instance didn't have an id");
      }
      successfullInvocations++;
    }
  }

  public void testProcessInstanceId() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <event type='process-start'>"
      + "    <action class='"
      + ProcessInstanceIdVerifier.class.getName()
      + "'/>"
      + "  </event>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <state name='a' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    assertEquals(1, successfullInvocations);
  }

  public static class TokenIdVerifier implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      if (executionContext.getToken().getId() == 0) {
        throw new RuntimeException("token '"
          + executionContext.getToken()
          + "' didn't have an id");
      }
      successfullInvocations++;
    }
  }

  public void testTokenId() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='f' />"
      + "  </start-state>"
      + "  <fork name='f'>"
      + "    <transition name='a' to='a' />"
      + "    <transition name='b' to='b' />"
      + "    <event type='node-leave'>"
      + "      <action class='"
      + TokenIdVerifier.class.getName()
      + "' />"
      + "    </event>"
      + "  </fork>"
      + "  <state name='a' />"
      + "  <state name='b' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    assertEquals(2, successfullInvocations);
  }
}
