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

  static int counter = 0;

  public static class PlusPlus implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      counter++;
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    counter = 0;
  }

  public void testTimerCreation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling'>"
        + "      <timer name='ceiling-timer' duedate='0 seconds'>"
        + "        <action class='org.jbpm.taskmgmt.exe.TaskTimerExecutionDbTest$PlusPlus' />"
        + "      </timer>"
        + "    </task>"
        + "  </task-node>"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);

      Timer timer = getTimer();
      assertEquals("ceiling-timer", timer.getName());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTimerDeletion() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling'>"
        + "      <timer name='ceiling-timer' duedate='0 seconds'>"
        + "        <action class='org.jbpm.taskmgmt.exe.TaskTimerExecutionDbTest$PlusPlus' />"
        + "      </timer>"
        + "    </task>"
        + "    <transition to='b' />"
        + "  </task-node>"
        + "  <state name='b' />"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      jbpmContext.save(processInstance);

      newTransaction();

      List<TaskInstance> taskInstances = taskMgmtSession.findTaskInstancesByToken(processInstance.getRootToken()
          .getId());
      assertEquals(1, taskInstances.size());

      TaskInstance taskInstance = taskInstances.get(0);
      taskInstance.end();
      jbpmContext.save(taskInstance);

      newTransaction();

      assertEquals(0, getTimerCount());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTimerExecution() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling'>"
        + "      <timer name='ceiling-timer' duedate='0 seconds'>"
        + "        <action class='org.jbpm.taskmgmt.exe.TaskTimerExecutionDbTest$PlusPlus' />"
        + "      </timer>"
        + "    </task>"
        + "  </task-node>"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      jbpmContext.save(processInstance);

      processJobs(5000);

      assertEquals(1, counter);
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }

  }

  public void testTaskNodeTimerExecution() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <timer name='ceiling-timer' duedate='0 seconds'>"
        + "      <action class='org.jbpm.taskmgmt.exe.TaskTimerExecutionDbTest$PlusPlus' />"
        + "    </timer>"
        + "    <task name='clean ceiling' />"
        + "  </task-node>"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      jbpmContext.save(processInstance);

      newTransaction();

      processJobs(5000);
      assertEquals(1, counter);
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTimerExecutionRepeat() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling'>"
        + "      <timer name='ceiling-timer' duedate='0 seconds' repeat='60 second'>"
        + "        <action class='org.jbpm.taskmgmt.exe.TaskTimerExecutionDbTest$PlusPlus' />"
        + "      </timer>"
        + "    </task>"
        + "  </task-node>"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      jbpmContext.save(processInstance);

      newTransaction();

      // fetch the original duedate
      Timer timer = getTimer();
      assertNotNull(timer);
      long originalDueDate = timer.getDueDate().getTime();

      timer.execute(jbpmContext);
      assertEquals(1, counter);

      // check if the timer has be re-scheduled because of the repeat.
      timer = getTimer();
      assertNotNull(timer);
      // check that the timer was rescheduled with a duedate 60 seconds after the original duedate.
      assertEquals(originalDueDate + 60000, timer.getDueDate().getTime());
    }
    finally {
      newTransaction();
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTimerELCreation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <task-node name='a'>"
        + "    <task name='clean ceiling'>"
        + "      <timer name='ceiling-timer' duedate='#{baseDate} + 2 days'>"
        + "        <action class='org.jbpm.taskmgmt.exe.TaskTimerExecutionDbTest$PlusPlus' />"
        + "      </timer>"
        + "    </task>"
        + "  </task-node>"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    try {
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

      Timer timer = getTimer();
      assertEquals("ceiling-timer", timer.getName());

      assertNotNull(timer.getDueDate());
      assertEquals(baseDateTest.getTime(), timer.getDueDate());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  private Timer getTimer() {
    return (Timer) session.createCriteria(Timer.class).setMaxResults(1).uniqueResult();
  }
}
