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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.AssignmentHandler;

public class TaskAssignmentDbTest extends AbstractDbTestCase {

  public void testPersonalTasklist() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='personal'>"
      + "  <start-state>"
      + "    <transition to='work' />"
      + "  </start-state>"
      + "  <task-node name='work'>"
      + "    <task name='feed the chickens'>"
      + "      <assignment actor-id='john' />"
      + "    </task>"
      + "    <task name='walk the dog'>"
      + "      <assignment actor-id='john' />"
      + "    </task>"
      + "    <task name='play with wife'>"
      + "      <assignment actor-id='john' />"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    newTransaction();
    List taskInstances = jbpmContext.getTaskList("john");
    assertNotNull(taskInstances);
    assertEquals(3, taskInstances.size());

    List taskNames = Arrays.asList(new String[] { "feed the chickens", "walk the dog",
      "play with wife" });
    for (Iterator i = taskInstances.iterator(); i.hasNext();) {
      TaskInstance taskInstance = (TaskInstance) i.next();
      assert taskNames.contains(taskInstance.getName()) : taskInstance.getName();
    }
  }

  public static class PoolAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext)
      throws Exception {
      assignable.setPooledActors(new String[] { "john", "joe", "homer" });
    }
  }

  public void testPooledTasklist() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='pool'>"
      + "  <start-state>"
      + "    <transition to='work' />"
      + "  </start-state>"
      + "  <task-node name='work'>"
      + "    <task name='feed the chickens'>"
      + "      <assignment pooled-actors='john' />"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    newTransaction();
    List taskInstances = taskMgmtSession.findPooledTaskInstances("john");
    assertNotNull(taskInstances);
    assertEquals(1, taskInstances.size());

    TaskInstance taskInstance = (TaskInstance) taskInstances.get(0);
    assertEquals("feed the chickens", taskInstance.getName());
    assertNull(taskInstance.getActorId());
  }
}
