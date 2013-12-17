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
package org.jbpm.graph.def;

import java.util.HashSet;
import java.util.Set;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.node.Decision;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.Fork;
import org.jbpm.graph.node.InterleaveEnd;
import org.jbpm.graph.node.InterleaveStart;
import org.jbpm.graph.node.Join;
import org.jbpm.graph.node.Merge;
import org.jbpm.graph.node.MilestoneNode;
import org.jbpm.graph.node.ProcessState;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.State;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;

/**
 * All decendents of {@link org.jbpm.graph.def.GraphElement} have an concrete
 * implementation of the method getSupportedEventTypes() which returns a String
 * array of event names that are accepted by this graph element.
 * 
 * This test case has two purposes: 1) insuring that the graph elements return
 * their expected list 2) document which graph elements support which events
 * through logging
 * 
 * @author Jim Rigsbee
 * @since 3.0
 */
public class SupportedEventsTest extends AbstractJbpmTestCase {

  public void testNodeEvents() {
    assertSupportedEvents(new Node(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testDecisionEvents() {
    assertSupportedEvents(new Decision(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testEndStateEvents() {
    assertSupportedEvents(new EndState(), new String[] { "node-enter" });
  }

  public void testForkEvents() {
    assertSupportedEvents(new Fork(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testInterleaveEndEvents() {
    assertSupportedEvents(new InterleaveEnd(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testInterleaveStartEvents() {
    assertSupportedEvents(new InterleaveStart(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testJoinEvents() {
    assertSupportedEvents(new Join(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testMergeEvents() {
    assertSupportedEvents(new Merge(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testMilestoneNodeEvents() {
    assertSupportedEvents(new MilestoneNode(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testProcessStateEvents() {
    assertSupportedEvents(new ProcessState(), new String[] { "node-leave", "node-enter", "after-signal", "before-signal", "subprocess-created",
        "subprocess-end" });
  }

  public void testStartStateEvents() {
    assertSupportedEvents(new StartState(), new String[] { "node-leave", "after-signal" });
  }

  public void testStateEvents() {
    assertSupportedEvents(new State(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testSuperStateEvents() {
    assertSupportedEvents(new SuperState(), new String[] { "transition", "before-signal", "after-signal", "node-enter", "node-leave", "superstate-enter",
        "superstate-leave", "subprocess-created", "subprocess-end", "task-create", "task-assign", "task-start", "task-end", "timer" });
  }

  public void testTaskNodeEvents() {
    assertSupportedEvents(new TaskNode(), new String[] { "node-enter", "node-leave", "before-signal", "after-signal" });
  }

  public void testTaskEvents() {
    assertSupportedEvents(new Task(), new String[] { "task-create", "task-assign", "task-start", "task-end" });
  }

  public void testProcessDefinitionEvents() {
    assertSupportedEvents(new ProcessDefinition(), new String[] { "transition", "before-signal", "after-signal", "process-start", "process-end", "node-enter",
        "node-leave", "superstate-enter", "superstate-leave", "subprocess-created", "subprocess-end", "task-create", "task-assign", "task-start", "task-end",
        "timer" });
  }

  public void testTransitionEvents() {
    assertSupportedEvents(new Transition(), new String[] { "transition" });
  }

  private void assertSupportedEvents(GraphElement graphElement, String[] expectedEventTypes) {
    String[] supportedEventTypes = graphElement.getSupportedEventTypes();

    Set expectedSet = getHashSet(expectedEventTypes);
    Set supportedSet = getHashSet(supportedEventTypes);

    assertEquals(expectedSet, supportedSet);
  }

  private HashSet getHashSet(String[] strings) {
    HashSet set = new HashSet();
    for (int i = 0; i < strings.length; i++) {
      set.add(strings[i]);
    }
    return set;
  }
}
