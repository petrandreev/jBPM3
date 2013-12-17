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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class TaskVariablesTest extends AbstractJbpmTestCase {

  public void testVariables() {
    TaskInstance taskInstance = new TaskInstance();
    taskInstance.setVariable("key", "value");
    assertEquals("value", taskInstance.getVariable("key"));
  }

  public void testSetOnTaskInstanceGetOnProcess() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='t' />" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task name='vartask' />" +
      "  </task-node>" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    
    HashMap expectedVariables = new HashMap();
    assertEquals(expectedVariables, taskInstance.getVariables());
    assertFalse(taskInstance.hasVariable("a"));
    assertNull(taskInstance.getVariable("a"));
    assertNull(contextInstance.getVariable("a"));
    
    taskInstance.setVariable("a", "1");
    
    expectedVariables.put("a", "1");
    assertEquals(expectedVariables, taskInstance.getVariables());

    assertTrue(taskInstance.hasVariable("a"));
    assertEquals("1", taskInstance.getVariable("a"));
    assertEquals("1", contextInstance.getVariable("a"));
  }


  public void testSetOnProcessGetOnTaskInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='t' />" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task name='vartask' />" +
      "  </task-node>" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    
    HashMap expectedVariables = new HashMap();
    assertEquals(expectedVariables, taskInstance.getVariables());
    assertFalse(taskInstance.hasVariable("a"));
    assertNull(taskInstance.getVariable("a"));
    assertNull(contextInstance.getVariable("a"));
    
    contextInstance.setVariable("a", "1");
    
    expectedVariables.put("a", "1");
    assertEquals(expectedVariables, taskInstance.getVariables());

    assertTrue(taskInstance.hasVariable("a"));
    assertEquals("1", taskInstance.getVariable("a"));
    assertEquals("1", contextInstance.getVariable("a"));
  }

  public void testSetLocally() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='t' />" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task name='vartask' />" +
      "  </task-node>" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    
    HashMap expectedVariables = new HashMap();
    assertEquals(expectedVariables, taskInstance.getVariables());
    assertFalse(taskInstance.hasVariable("a"));
    assertNull(taskInstance.getVariable("a"));
    assertNull(contextInstance.getVariable("a"));
    
    taskInstance.setVariableLocally("a", "1");
    
    expectedVariables.put("a", "1");
    assertEquals(expectedVariables, taskInstance.getVariables());

    assertTrue(taskInstance.hasVariable("a"));
    assertEquals("1", taskInstance.getVariable("a"));
    assertNull(contextInstance.getVariable("a"));
  }
  
  public void testCopyWithController() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='t' />" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task name='vartask'>" +
      "      <controller>" +
      "        <variable name='a' />" +
      "        <variable name='b' />" +
      "      </controller>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    
    contextInstance.setVariable("a", "1");
    contextInstance.setVariable("b", "2");
    contextInstance.setVariable("c", "3");
    
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    
    HashMap expectedVariables = new HashMap();
    expectedVariables.put("a", "1");
    expectedVariables.put("b", "2");
    expectedVariables.put("c", "3");
    assertEquals(expectedVariables, taskInstance.getVariables());
    
    taskInstance.setVariable("a", "1 modified");
    taskInstance.setVariable("b", "2 modified");
    taskInstance.setVariable("c", "3 modified");

    expectedVariables = new HashMap();
    expectedVariables.put("a", "1 modified");
    expectedVariables.put("b", "2 modified");
    expectedVariables.put("c", "3 modified");
    assertEquals(expectedVariables, taskInstance.getVariables());

    expectedVariables = new HashMap();
    expectedVariables.put("a", "1"); // task instance had local copy for var a
    expectedVariables.put("b", "2"); // task instance had local copy for var b
    expectedVariables.put("c", "3 modified");
    assertEquals(expectedVariables, contextInstance.getVariables());
  }
}
