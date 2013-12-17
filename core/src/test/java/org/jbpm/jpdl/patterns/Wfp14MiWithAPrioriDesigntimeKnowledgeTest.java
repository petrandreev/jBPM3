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

import java.util.ArrayList;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_13.swf
 */
public class Wfp14MiWithAPrioriDesigntimeKnowledgeTest extends AbstractJbpmTestCase {

  public void testSituation1() {
    ProcessDefinition pd = ProcessDefinition.parseXmlString("<process-definition name='the life of a baby'>"
      + "  <start-state name='a'>"
      + "    <transition to='b' />"
      + "  </start-state>"
      + "  <state name='b'>"
      + "    <transition to='t' />"
      + "  </state>"
      + "  <task-node name='t'>"
      + "    <task name='eat' />"
      + "    <task name='drink' />"
      + "    <task name='sleep' />"
      + "    <transition to='c' />"
      + "  </task-node>"
      + "  <state name='c' />"
      + "</process-definition>");

    ProcessInstance pi = new ProcessInstance(pd);
    TaskMgmtInstance taskMgmtInstance = pi.getTaskMgmtInstance();

    pi.signal();
    Token token = pi.getRootToken();

    // after start, the token is waiting in state b
    assertSame(pd.getNode("b"), token.getNode());

    // and no tasks have been created yet
    assertEquals(0, taskMgmtInstance.getUnfinishedTasks(token).size());

    // now we signal the process to move on.
    // execution will arrive at the task-node
    // the default behaviour of the task node is to create each task and wait
    // till the last one finishes
    pi.signal();

    // now, 3 tasks have been created...
    List tasks = new ArrayList(taskMgmtInstance.getUnfinishedTasks(token));
    assertEquals(3, tasks.size());
    // ... and the process is in the task state
    assertSame(pd.getNode("t"), token.getNode());

    // now we finish the tasks one by one
    // finish task 0
    ((TaskInstance) tasks.get(0)).end();

    // still 2 tasks remaining
    assertEquals(2, taskMgmtInstance.getUnfinishedTasks(token).size());
    // and the process is still in node t
    assertSame(pd.getNode("t"), token.getNode());

    // finish task 1
    ((TaskInstance) tasks.get(1)).end();

    // still 1 task remaining
    assertEquals(1, taskMgmtInstance.getUnfinishedTasks(token).size());
    // and the process is still in node t
    assertSame(pd.getNode("t"), token.getNode());

    // finish task 2
    ((TaskInstance) tasks.get(2)).end();

    // no more tasks remaining
    assertEquals(0, taskMgmtInstance.getUnfinishedTasks(token).size());
    // and the process has moved to node c
    assertEquals(pd.getNode("c"), token.getNode());
  }
}
