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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.AssignmentHandler;

public class SwimlaneTest extends AbstractJbpmTestCase {

  public void testStartStateSwimlaneInitialization() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='initiator' />"
      + "  <start-state>"
      + "    <task swimlane='initiator' />"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <state name='a' />"
      + "</process-definition>");

    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <jbpm-context>"
      + "    <service name='authentication' factory='org.jbpm.security.authentication.DefaultAuthenticationServiceFactory' />"
      + "    <service name='tx' factory='org.jbpm.tx.TxServiceFactory' />"
      + "  </jbpm-context>"
      + "</jbpm-configuration>");
    try {
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      jbpmContext.setActorId("the other guy");

      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
      taskMgmtInstance.createStartTaskInstance();
      processInstance.signal();

      assertEquals("the other guy", taskMgmtInstance.getSwimlaneInstance("initiator")
        .getActorId());
    }
    finally {
      jbpmConfiguration.close();
    }
  }

  public static class TestAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext)
      throws Exception {
      Integer count = (Integer) executionContext.getVariable("count");
      executionContext.setVariable("count", new Integer(count != null ? count.intValue() + 1
        : 1));
      assignable.setActorId("me");
    }
  }

  public void testSwimlaneAssignmentHandler() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='stalker'>"
      + "    <assignment class='org.jbpm.taskmgmt.exe.SwimlaneTest$TestAssignmentHandler' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='stalker' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <task-node name='b'>"
      + "    <task name='beauty sleep' swimlane='stalker' />"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    Integer count = (Integer) processInstance.getContextInstance().getVariable("count");
    assertEquals(1, count.intValue());

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance stalker = taskMgmtInstance.getSwimlaneInstance("stalker");
    assertEquals("me", stalker.getActorId());

    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertEquals("me", changeNappy.getActorId());
    
    changeNappy.end();
    TaskInstance beautySleep = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(processInstance.getRootToken())
      .iterator()
      .next();
    assertEquals("me", beautySleep.getActorId());
  }

  public void testSwimlaneActorId() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='stalker'>"
      + "    <assignment actor-id='johndoe' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='stalker' />"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance stalker = taskMgmtInstance.getSwimlaneInstance("stalker");
    assertEquals("johndoe", stalker.getActorId());

    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertEquals("johndoe", changeNappy.getActorId());
  }

  public void testSwimlanePooledActor() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='stalker'>"
      + "    <assignment pooled-actors='hippies,hells angles' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='stalker' />"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance stalker = taskMgmtInstance.getSwimlaneInstance("stalker");
    assertNull(stalker.getActorId());

    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertNull(changeNappy.getActorId());

    Set retrievedSwimlaneInstancePooledActorIds = new HashSet();
    for (Iterator iter = stalker.getPooledActors().iterator(); iter.hasNext();) {
      PooledActor pooledActor = (PooledActor) iter.next();
      retrievedSwimlaneInstancePooledActorIds.add(pooledActor.getActorId());
    }
    Set expectedPooledActorIds = new HashSet();
    expectedPooledActorIds.add("hippies");
    expectedPooledActorIds.add("hells angles");
    assertEquals(expectedPooledActorIds, retrievedSwimlaneInstancePooledActorIds);

    Set retrievedTaskInstancePooledActorIds = new HashSet();
    for (Iterator iter = changeNappy.getPooledActors().iterator(); iter.hasNext();) {
      PooledActor pooledActor = (PooledActor) iter.next();
      retrievedTaskInstancePooledActorIds.add(pooledActor.getActorId());
    }
    assertEquals(expectedPooledActorIds, retrievedTaskInstancePooledActorIds);
  }

  public void testSwimlanePooledActorThenTaskInstanceAssignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='stalker'>"
      + "    <assignment pooled-actors='hippies,hells angles' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='stalker' />"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance stalker = taskMgmtInstance.getSwimlaneInstance("stalker");
    assertNull(stalker.getActorId());

    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertNull(changeNappy.getActorId());
    
    changeNappy.setActorId("johndoe");
    assertEquals("johndoe", stalker.getActorId());
    assertEquals("johndoe", changeNappy.getActorId());
  }

  public void testSwimlanePooledActorThenTaskInstanceReassignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='stalker'>"
      + "    <assignment pooled-actors='hippies,hells angles' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='stalker' />"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance stalker = taskMgmtInstance.getSwimlaneInstance("stalker");
    assertNull(stalker.getActorId());

    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertNull(changeNappy.getActorId());

    changeNappy.setActorId("johndoe");
    changeNappy.setActorId("joesmoe");
    assertEquals("joesmoe", stalker.getActorId());
    assertEquals("joesmoe", changeNappy.getActorId());
  }

  public void testSwimlanePooledActorThenSwimlaneInstanceAssignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='stalker'>"
      + "    <assignment pooled-actors='hippies,hells angles' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='stalker' />"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance stalker = taskMgmtInstance.getSwimlaneInstance("stalker");
    assertNull(stalker.getActorId());

    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertNull(changeNappy.getActorId());

    stalker.setActorId("johndoe");
    assertEquals("johndoe", stalker.getActorId());
    assertNull(changeNappy.getActorId());
  }

  public void testSwimlaneReassignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hero'>"
      + "    <assignment pooled-actors='hippies,hells angles' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='hero' />'"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <task-node name='b'>"
      + "    <task name='make bottle' swimlane='hero' />'"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    changeNappy.setActorId("johndoe");
    changeNappy.end();

    TaskInstance makeBottle = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertEquals("johndoe", makeBottle.getActorId());

    Set retrievedTaskInstancePooledActorIds = new HashSet();
    for (Iterator iter = makeBottle.getPooledActors().iterator(); iter.hasNext();) {
      PooledActor pooledActor = (PooledActor) iter.next();
      retrievedTaskInstancePooledActorIds.add(pooledActor.getActorId());
    }

    Set expectedPooledActorIds = new HashSet();
    expectedPooledActorIds.add("hippies");
    expectedPooledActorIds.add("hells angles");
    assertEquals(expectedPooledActorIds, retrievedTaskInstancePooledActorIds);
  }

  public void testSwimlanePooledActorsUpdate() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hero'>"
      + "    <assignment pooled-actors='hippies,hells angles' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='hero' />'"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <task-node name='b'>"
      + "    <task name='make bottle' swimlane='hero' />'"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance stalker = taskMgmtInstance.getSwimlaneInstance("hero");
    stalker.setPooledActors(new String[] {
      "footballers", "hooligans", "stewards"
    });
    
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    changeNappy.setActorId("johndoe");
    changeNappy.end();

    Token token = processInstance.getRootToken();
    TaskInstance makeBottle = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(token)
      .iterator()
      .next();
    assertEquals("johndoe", makeBottle.getActorId());

    Set retrievedTaskInstancePooledActorIds = new HashSet();
    for (Iterator iter = makeBottle.getPooledActors().iterator(); iter.hasNext();) {
      PooledActor pooledActor = (PooledActor) iter.next();
      retrievedTaskInstancePooledActorIds.add(pooledActor.getActorId());
    }
    Set expectedPooledActorIds = new HashSet();
    expectedPooledActorIds.add("footballers");
    expectedPooledActorIds.add("hooligans");
    expectedPooledActorIds.add("stewards");
    assertEquals(expectedPooledActorIds, retrievedTaskInstancePooledActorIds);
  }

  public void testSwimlaneActorReassignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hero'>"
      + "    <assignment class='org.jbpm.taskmgmt.exe.SwimlaneTest$TestAssignmentHandler' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='hero' />'"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <task-node name='b'>"
      + "    <task name='make bottle' swimlane='hero' />'"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    ContextInstance contextInstance = processInstance.getContextInstance();
    Integer count = (Integer) contextInstance.getVariable("count");
    assertEquals(1, count.intValue());

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertEquals("me", changeNappy.getActorId());
    
    changeNappy.end();
    TaskInstance makeBottle = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(processInstance.getRootToken())
      .iterator()
      .next();
    assertEquals("me", makeBottle.getActorId());

    count = (Integer) contextInstance.getVariable("count");
    assertEquals(1, count.intValue());
  }

  public static class MultipleAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext)
      throws Exception {
      Integer count = (Integer) executionContext.getVariable("count");
      executionContext.setVariable("count", new Integer(count != null ? count.intValue() + 1
        : 1));

      assignable.setPooledActors(new String[] {
        "me", "you", "them"
      });
    }
  }

  public void testSwimlanePoolInitialization() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hero'>"
      + "    <assignment class='org.jbpm.taskmgmt.exe.SwimlaneTest$MultipleAssignmentHandler' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='hero' />'"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    Integer count = (Integer) processInstance.getContextInstance().getVariable("count");
    assertEquals(1, count.intValue());

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertNull(changeNappy.getActorId());

    Set pooledActors = changeNappy.getPooledActors();
    assertEquals(3, pooledActors.size());

    List expectedPooledActorIds = Arrays.asList(new String[] {
      "me", "you", "them"
    });
    for (Iterator iter = pooledActors.iterator(); iter.hasNext();) {
      PooledActor pooledActor = (PooledActor) iter.next();
      assertTrue(expectedPooledActorIds.contains(pooledActor.getActorId()));
    }
  }

  public void testSwimlanePoolReassignmentOfNonTakenTask() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hero'>"
      + "    <assignment class='org.jbpm.taskmgmt.exe.SwimlaneTest$MultipleAssignmentHandler' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='hero' />'"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <task-node name='b'>"
      + "    <task name='make bottle' swimlane='hero' />'"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    Integer count = (Integer) processInstance.getContextInstance().getVariable("count");
    assertEquals(1, count.intValue());

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertNull(changeNappy.getActorId());
    assertEquals(3, changeNappy.getPooledActors().size());

    SwimlaneInstance swimlaneInstance = taskMgmtInstance.getSwimlaneInstance("hero");
    assertEquals(changeNappy.getPooledActors(), swimlaneInstance.getPooledActors());
    
    changeNappy.end();
    TaskInstance makeBottle = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(processInstance.getRootToken())
      .iterator()
      .next();
    assertNull(makeBottle.getActorId());
    assertEquals(3, makeBottle.getPooledActors().size());
    assertEquals(makeBottle.getPooledActors(), swimlaneInstance.getPooledActors());
  }

  public void testSwimlanePoolReassignmentOfTakenTask() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hero'>"
      + "    <assignment class='org.jbpm.taskmgmt.exe.SwimlaneTest$MultipleAssignmentHandler' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='hero' />'"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <task-node name='b'>"
      + "    <task name='make bottle' swimlane='hero' />'"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    
    Integer count = (Integer) processInstance.getContextInstance().getVariable("count");
    assertEquals(1, count.intValue());

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance swimlaneInstance = taskMgmtInstance.getSwimlaneInstance("hero");
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    changeNappy.setActorId("should-be-one-of-the-actors-in-the-pool-but-doesnt-have-to");
    assertEquals("should-be-one-of-the-actors-in-the-pool-but-doesnt-have-to",
      changeNappy.getActorId());
    assertEquals(3, changeNappy.getPooledActors().size());
    assertEquals(changeNappy.getPooledActors(), swimlaneInstance.getPooledActors());

    changeNappy.end();
    TaskInstance makeBottle = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(processInstance.getRootToken())
      .iterator()
      .next();
    assertEquals("should-be-one-of-the-actors-in-the-pool-but-doesnt-have-to",
      makeBottle.getActorId());
    assertEquals(3, makeBottle.getPooledActors().size());
    assertEquals(makeBottle.getPooledActors(), swimlaneInstance.getPooledActors());
  }

  public static class NullAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext)
      throws Exception {
    }
  }

  public void testNullActorsForSwimlaneInitialization() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hero'>"
      + "    <assignment class='org.jbpm.taskmgmt.exe.SwimlaneTest$NullAssignmentHandler' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='change nappy' swimlane='hero' />'"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    SwimlaneInstance swimlaneInstance = taskMgmtInstance.getSwimlaneInstance("hero");
    assertNull(swimlaneInstance.getActorId());
    assertNull(swimlaneInstance.getPooledActors());

    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances()
      .iterator()
      .next();
    assertNull(changeNappy.getActorId());
    assertNull(changeNappy.getPooledActors());
  }

  public void testTwoSwimlanes() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <swimlane name='hooligan'>"
      + "    <assignment actor-id='johndoe' />"
      + "  </swimlane>"
      + "  <swimlane name='policeman'>"
      + "    <assignment actor-id='joesmoe' />"
      + "  </swimlane>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='throw rock' swimlane='hooligan' />"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <task-node name='b'>"
      + "    <task name='hit him with your rithem stick' swimlane='policeman' />"
      + "    <transition to='c' />"
      + "  </task-node>"
      + "  <task-node name='c'>"
      + "    <task name='get nursed' swimlane='hooligan' />"
      + "    <transition to='a' />"
      + "  </task-node>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    processInstance.signal();

    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance throwRock = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(token)
      .iterator()
      .next();
    assertEquals("johndoe", throwRock.getActorId());

    throwRock.end();
    TaskInstance hitWithRithmStick = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(token)
      .iterator()
      .next();
    assertEquals("joesmoe", hitWithRithmStick.getActorId());

    hitWithRithmStick.end();
    TaskInstance getNursed = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(token)
      .iterator()
      .next();
    assertEquals("johndoe", getNursed.getActorId());

    SwimlaneInstance hooligan = taskMgmtInstance.getSwimlaneInstance("hooligan");
    hooligan.setActorId("janedane");

    getNursed.end();
    throwRock = (TaskInstance) taskMgmtInstance.getUnfinishedTasks(token).iterator().next();
    assertEquals("janedane", throwRock.getActorId());
  }
}
