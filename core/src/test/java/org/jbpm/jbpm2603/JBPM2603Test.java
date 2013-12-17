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
package org.jbpm.jbpm2603;

import java.util.Calendar;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.Delegation;
import org.jbpm.job.Timer;

/**
 * Restore automatic save of timer actions.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2603">JBPM-2603</a>
 * @author Alejandro Guizar
 */
public class JBPM2603Test extends AbstractDbTestCase {

  public void testStaticTimerAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition"
      + "  name='static timer action'>"
      + "  <start-state><transition to='mid'/></start-state>"
      + "  <state name='mid'>"
      + "    <timer duedate='1 hour'>"
      + "      <action name='example' class='org.example.Action'/>"
      + "    </timer>"
      + "  </state>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    Timer timer = (Timer) session.createCriteria(Timer.class).uniqueResult();
    assertEquals(processDefinition.getAction("example").getId(), timer.getAction().getId());
  }

  public void testDynamicTimerAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition"
      + "  name='dynamic timer action'>"
      + "  <start-state><transition to='mid'/></start-state>"
      + "  <state name='mid'>"
      + "    <event type='node-enter'>"
      + "      <action class='"
      + CreateDynamicTimer.class.getName()
      + "'/>"
      + "    </event>"
      + "  </state>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    Timer timer = (Timer) session.createCriteria(Timer.class).uniqueResult();
    Action action = timer.getAction();
    assertEquals("org.example.Action", action.getActionDelegation().getClassName());

    jobSession.deleteJob(timer);
    session.delete(action);
  }

  public static class CreateDynamicTimer implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Action action = new Action(new Delegation("org.example.Action"));

      Calendar oneSecond = Calendar.getInstance();
      oneSecond.add(Calendar.SECOND, 1);

      Timer timer = new Timer(executionContext.getToken());
      timer.setDueDate(oneSecond.getTime());
      timer.setAction(action);

      executionContext.getJbpmContext().getServices().getSchedulerService().createTimer(timer);
    }
  }
}
