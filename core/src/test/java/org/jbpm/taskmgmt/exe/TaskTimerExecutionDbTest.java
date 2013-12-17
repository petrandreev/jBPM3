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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Timer;

public class TaskTimerExecutionDbTest extends AbstractDbTestCase {

  public static class PlusPlus implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Integer count = (Integer) executionContext.getVariable("count");
      count = new Integer(count != null ? count.intValue() + 1 : 1);
      executionContext.setVariable("count", count);
    }
  }

  public void testTimerCreation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling'>"
      + "      <timer name='ceiling-timer' duedate='0 seconds'>"
      + "        <action class='"
      + PlusPlus.class.getName()
      + "' />"
      + "      </timer>"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    Timer timer = (Timer) session.createCriteria(Timer.class).uniqueResult();
    assertEquals("ceiling-timer", timer.getName());
  }

  public void testTimerDeletion() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling'>"
      + "      <timer name='ceiling-timer' duedate='0 seconds'>"
      + "        <action class='"
      + PlusPlus.class.getName()
      + "' />"
      + "      </timer>"
      + "    </task>"
      + "    <transition to='b' />"
      + "  </task-node>"
      + "  <state name='b' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    newTransaction();
    List taskInstances = taskMgmtSession.findTaskInstancesByToken(processInstance.getRootToken()
      .getId());
    assertEquals(1, taskInstances.size());

    TaskInstance taskInstance = (TaskInstance) taskInstances.get(0);
    taskInstance.end();
    jbpmContext.save(taskInstance);

    newTransaction();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assertNull("expected variable 'count' to be null", processInstance.getContextInstance()
      .getVariable("count"));
  }

  public void testTimerExecution() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling'>"
      + "      <timer name='ceiling-timer' duedate='0 seconds'>"
      + "        <action class='"
      + PlusPlus.class.getName()
      + "' />"
      + "      </timer>"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    processJobs();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assertEquals(new Integer(1), processInstance.getContextInstance().getVariable("count"));
  }

  public void testTaskNodeTimerExecution() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <timer name='ceiling-timer' duedate='0 seconds'>"
      + "      <action class='"
      + PlusPlus.class.getName()
      + "' />"
      + "    </timer>"
      + "    <task name='clean ceiling' />"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    processJobs();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assertEquals(new Integer(1), processInstance.getContextInstance().getVariable("count"));
  }

  public void testTimerExecutionRepeat() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling'>"
      + "      <timer name='ceiling-timer' duedate='0 seconds' repeat='60 second'>"
      + "        <action class='"
      + PlusPlus.class.getName()
      + "' />"
      + "      </timer>"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    newTransaction();
    // fetch the original duedate
    Timer timer = (Timer) session.createCriteria(Timer.class).uniqueResult();
    long originalDueDate = timer.getDueDate().getTime();

    timer.execute(jbpmContext);
    processInstance = timer.getProcessInstance();
    assertEquals(new Integer(1), processInstance.getContextInstance().getVariable("count"));

    // check whether timer has been re-scheduled due to repeat
    timer = (Timer) session.createCriteria(Timer.class).uniqueResult();
    assertNotNull(timer);
    // verify timer was rescheduled 60 seconds after original duedate
    assertEquals(originalDueDate + 60000, timer.getDueDate().getTime());
  }

  public void testTimerELCreation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='a' />"
      + "  </start-state>"
      + "  <task-node name='a'>"
      + "    <task name='clean ceiling'>"
      + "      <timer name='ceiling-timer' duedate='#{baseDate} + 2 days'>"
      + "        <action class='"
      + PlusPlus.class.getName()
      + "' />"
      + "      </timer>"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    Calendar baseDate = Calendar.getInstance();
    Date dateTestDate = new Date();
    baseDate.setTime(dateTestDate);
    baseDate.clear(Calendar.MILLISECOND);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("baseDate", baseDate.getTime());
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    Calendar baseDateTest = Calendar.getInstance();
    baseDateTest.setTime(dateTestDate);
    baseDateTest.clear(Calendar.MILLISECOND);
    baseDateTest.add(Calendar.DAY_OF_YEAR, 2);

    Timer timer = (Timer) session.createCriteria(Timer.class).uniqueResult();
    assertEquals("ceiling-timer", timer.getName());

    assertNotNull(timer.getDueDate());
    assertEquals(baseDateTest.getTime(), timer.getDueDate());
  }
}
