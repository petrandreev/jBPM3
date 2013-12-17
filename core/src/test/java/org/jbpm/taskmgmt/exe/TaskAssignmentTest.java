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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.security.authentication.DefaultAuthenticationServiceFactory;
import org.jbpm.taskmgmt.def.AssignmentHandler;

public class TaskAssignmentTest extends AbstractJbpmTestCase {
  
  public static class TestAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;
    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
      assignable.setActorId("johndoe");
    }
  }
  
  public void testTaskAssignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='change nappy'>" +
      "      <assignment class='org.jbpm.taskmgmt.exe.TaskAssignmentTest$TestAssignmentHandler' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    
    TaskMgmtInstance taskMgmtInstance =processInstance.getTaskMgmtInstance();
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    
    assertEquals("johndoe", changeNappy.getActorId());
    assertNull(changeNappy.getPooledActors());
  }
  
  public static class MultipleAssignmentHandler implements AssignmentHandler {
    private static final long serialVersionUID = 1L;
    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
      assignable.setPooledActors(new String[]{"me", "you", "them"});
    }
  }

  public void testMultiActorTaskAssignment() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='change nappy'>" +
      "      <assignment class='org.jbpm.taskmgmt.exe.TaskAssignmentTest$MultipleAssignmentHandler' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    
    Set expectedActorIds = new HashSet( Arrays.asList(new Object[]{"me", "you", "them"}) );
    
    // collect actor ids from the task instances
    Set pooledActorIds = new HashSet();
    Iterator iter = changeNappy.getPooledActors().iterator();
    while (iter.hasNext()) {
      PooledActor pooledActor = (PooledActor) iter.next();
      pooledActorIds.add(pooledActor.getActorId());
      assertTrue(pooledActor.getTaskInstances().contains(changeNappy));
      assertNull(pooledActor.getSwimlaneInstance());
    }
    
    assertEquals(expectedActorIds, pooledActorIds);
  }

  public static class AssignmentHandlerContextTester implements AssignmentHandler {
    private static final long serialVersionUID = 1L;
    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
    }
  }
  
  public void testAssignmentHandlerContext() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='change nappy'>" +
      "      <assignment class='"+AssignmentHandlerContextTester.class.getName()+"' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals(1, taskMgmtInstance.getTaskInstances().size());
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertNull(changeNappy.getActorId());
    assertNull(changeNappy.getPooledActors());
  }
  
  public static class AssignmentHandlerNullActorId implements AssignmentHandler {
    private static final long serialVersionUID = 1L;
    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
      assignable.setActorId(null);
    }
  }
  
  public void testAssignmentHandlerWithNullActorId() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='change nappy'>" +
      "      <assignment class='"+AssignmentHandlerNullActorId.class.getName()+"' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals(1, taskMgmtInstance.getTaskInstances().size());
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertNull(changeNappy.getActorId());
  }
  
  public void testStartTaskSwimlaneAssignmentTest() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context>" +
      "    <service name='authentication' factory='" +
      DefaultAuthenticationServiceFactory.class.getName() +
      "' />" +
      "  </jbpm-context>" +
      "</jbpm-configuration>");

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <swimlane name='initiator' />" +
      "  <start-state>" +
      "    <task name='start this process' swimlane='initiator' />" +
      "    <transition to='wait' />" +
      "  </start-state>" +
      "  <state name='wait' />" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    jbpmContext.setActorId("user");
    try {
      taskMgmtInstance.createStartTaskInstance().end();
    } finally {
      jbpmContext.setActorId(null);
      jbpmContext.close();
      jbpmConfiguration.close();
    }

    SwimlaneInstance swimlaneInstance = taskMgmtInstance.getSwimlaneInstance("initiator");
    assertEquals("user", swimlaneInstance.getActorId());
  }

  public void testActorId() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='change nappy'>" +
      "      <assignment actor-id='me' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals(1, taskMgmtInstance.getTaskInstances().size());
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertEquals("me", changeNappy.getActorId());
    assertNull(changeNappy.getPooledActors());
  }

  public void testPooledActors() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='change nappy'>" +
      "      <assignment pooled-actors='flinstones,huckleberries' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals(1, taskMgmtInstance.getTaskInstances().size());
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertNull(changeNappy.getActorId());
    
    Set expectedPooledActors = new HashSet();
    expectedPooledActors.add("flinstones");
    expectedPooledActors.add("huckleberries");
    
    Set assignedPooledActors = new HashSet();
    Iterator iter = changeNappy.getPooledActors().iterator();
    while (iter.hasNext()) {
      PooledActor pooledActor = (PooledActor) iter.next();
      assignedPooledActors.add(pooledActor.getActorId());
    }

    assertEquals( expectedPooledActors, assignedPooledActors ); 
  }

  public void testActorIdsAndPooledActors() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='change nappy'>" +
      "      <assignment actor-id='me' pooled-actors='flinstones,huckleberries' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    assertEquals(1, taskMgmtInstance.getTaskInstances().size());
    TaskInstance changeNappy = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertEquals("me", changeNappy.getActorId());
    
    Set expectedPooledActors = new HashSet();
    expectedPooledActors.add("flinstones");
    expectedPooledActors.add("huckleberries");
    
    Set assignedPooledActors = new HashSet();
    Iterator iter = changeNappy.getPooledActors().iterator();
    while (iter.hasNext()) {
      PooledActor pooledActor = (PooledActor) iter.next();
      assignedPooledActors.add(pooledActor.getActorId());
    }

    assertEquals( expectedPooledActors, assignedPooledActors ); 
  }

  
  public static class AssignActionHandler implements ActionHandler {
    private static final long serialVersionUID = 1L;
    static List assignments = null;
    public void execute(ExecutionContext executionContext) throws Exception {
      TaskInstance taskInstance = executionContext.getTaskInstance();
      assignments.add("from '"+taskInstance.getPreviousActorId()+"' to '"+taskInstance.getActorId()+"'");
    }
  }
  public void testTaskAssignmentEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='do something'>" +
      "      <assignment actor-id='me' />" +
      "      <event type='task-assign'>" +
      "        <action class='org.jbpm.taskmgmt.exe.TaskAssignmentTest$AssignActionHandler' />" +
      "      </event>" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    AssignActionHandler.assignments = new ArrayList();
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    
    List expectedAssignments = new ArrayList();
    expectedAssignments.add("from 'null' to 'me'");
    assertEquals(expectedAssignments, AssignActionHandler.assignments);
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance doSomething = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    doSomething.setActorId("you");

    expectedAssignments.add("from 'me' to 'you'");
    assertEquals(expectedAssignments, AssignActionHandler.assignments);
  }

  
  public static class AssignmentBean {
    // methods used to test actor-id assignments
    public String getTheBestProgrammer() {
      return "me";
    }
    public String aNonGetterAssignmentMethod() {
      return "you";
    }

    // methods used to test pooled-actors assignments
    public String[] getTheBestProgrammersStringArray() {
      return new String[]{"me", "you", "them"};
    }
    public List getTheBestProgrammersCollection() {
      List programmers = new ArrayList();
      programmers.add("me");
      programmers.add("you");
      programmers.add("them");
      return programmers;
    }
    public String theBestProgrammersAsCommaSeparatedString() {
      return "me, you, them";
    }
    public String[] aStringArrayMethod() {
      return new String[]{"me", "you", "them"};
    }
    public List aCollectionMethod() {
      List programmers = new ArrayList();
      programmers.add("me");
      programmers.add("you");
      programmers.add("them");
      return programmers;
    }
    public String aCommaSeparatedStringMethod() {
      return "me, you, them";
    }
  }
  
  public void testActorIdPropertyExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='do something'>" +
      "      <assignment actor-id='#{assignmentBean.theBestProgrammer}' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    AssignActionHandler.assignments = new ArrayList();
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("assignmentBean", new AssignmentBean());
    processInstance.signal();
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance doSomething = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertEquals("me", doSomething.getActorId());
    assertNull(doSomething.getPooledActors());
  }

  public void testActorIdMethodExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='do something'>" +
      "      <assignment actor-id='#{assignmentBean.aNonGetterAssignmentMethod}' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    AssignActionHandler.assignments = new ArrayList();
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("assignmentBean", new AssignmentBean());
    processInstance.signal();
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance doSomething = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertEquals("you", doSomething.getActorId());
    assertNull(doSomething.getPooledActors());
  }

  public void testPooledActorsStringArrayPropertyExpression() {
    pooledActorExpressionTest("#{assignmentBean.theBestProgrammersStringArray}");
  }

  public void testPooledActorsCollectionPropertyExpression() {
    pooledActorExpressionTest("#{assignmentBean.theBestProgrammersCollection}");
  }

  public void testPooledActorsStringPropertyExpression() {
    pooledActorExpressionTest("#{assignmentBean.theBestProgrammersAsCommaSeparatedString}");
  }

  public void testPooledActorStringArrayMethodExpression() {
    pooledActorExpressionTest("#{assignmentBean.aStringArrayMethod}");
  }

  public void testPooledActorCollectionMethodExpression() {
    pooledActorExpressionTest("#{assignmentBean.aCollectionMethod}");
  }

  public void testPooledActorCommaSeparatedStringMethodExpression() {
    pooledActorExpressionTest("#{assignmentBean.aCommaSeparatedStringMethod}");
  }

  public void pooledActorExpressionTest(String expression) {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <task-node name='a'>" +
      "    <task name='do something'>" +
      "      <assignment pooled-actors='"+expression+"' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    
    AssignActionHandler.assignments = new ArrayList();
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("assignmentBean", new AssignmentBean());
    processInstance.signal();
    
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    TaskInstance doSomething = (TaskInstance) taskMgmtInstance.getTaskInstances().iterator().next();
    assertNull(doSomething.getActorId());
    
    Set expectedActorIds = new HashSet();
    expectedActorIds.add("me");
    expectedActorIds.add("you");
    expectedActorIds.add("them");
    
    Set pooledActorIds = PooledActor.extractActorIds(doSomething.getPooledActors());
    assertEquals(expectedActorIds, pooledActorIds);
  }
}
