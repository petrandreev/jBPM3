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
package org.jbpm.jbpm1921;

import java.util.Arrays;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Assigning a task to two pooled actors and calling
 * <code>jbpmContext.getGroupTaskList(List actorIds)</code> with these two
 * actors will get you a list with duplicate task instances.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1921">JBPM-1921</a>
 * @author Alejandro Guizar
 */
public class JBPM1921Test extends AbstractDbTestCase {

  public void testFindByActorId() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<?xml version='1.0'?>"
      + "<process-definition name='jbpm1921'>"
      + "  <start-state name='start'>"
      + "    <transition to='team work'/>"
      + "  </start-state>"
      + "  <task-node name='team work'>"
      + "    <task>"
      + "      <assignment pooled-actors='ernie,bert,ernie' />"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance(processDefinition.getName());
    processInstance.signal();
    List pooledTasks = taskMgmtSession.findPooledTaskInstances("ernie");
    assertEquals(1, pooledTasks.size());
  }

  public void testFindByActorIds() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<?xml version='1.0'?>"
      + "<process-definition name='jbpm1921'>"
      + "  <start-state name='start'>"
      + "    <transition to='team work'/>"
      + "  </start-state>"
      + "  <task-node name='team work'>"
      + "    <task>"
      + "      <assignment pooled-actors='ernie,bert,groover' />"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance(processDefinition.getName());
    processInstance.signal();
    List pooledTasks = jbpmContext.getGroupTaskList(Arrays.asList(new String[] { "ernie",
      "bert" }));
    assertEquals(1, pooledTasks.size());
  }
}
