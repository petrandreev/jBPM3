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
package org.jbpm.enterprise.ejbtimer;

import java.rmi.RemoteException;

import junit.framework.Test;

import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.enterprise.AbstractEnterpriseTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.scheduler.ejbtimer.EntitySchedulerService;

/**
 * Exercises for the {@linkplain EntitySchedulerService EJB scheduler service}.
 * 
 * @author Alejandro Guizar
 */
public class EjbSchedulerTest extends AbstractEnterpriseTestCase {

  public static Test suite() throws Exception {
    return new IntegrationTestSetup(EjbSchedulerTest.class, "enterprise-test.war");
  }

  public void testScheduleFuture() throws Exception {
    deployProcessDefinition("<process-definition name='future'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processInstanceId = startProcessInstance("future").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testSchedulePast() throws Exception {
    deployProcessDefinition("<process-definition name='past'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='-1 second' transition='timeout' />"
        + "    <transition name='timeout' to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processInstanceId = startProcessInstance("past").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testScheduleRepeat() throws Exception {
    deployProcessDefinition("<process-definition name='repeat'>"
        + "  <event type='timer'>"
        + "    <action expression='#{eventCallback.timer}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='1 second' repeat='1 second' />"
        + "    <transition to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    ProcessInstance processInstance = startProcessInstance("repeat");
    long processInstanceId = processInstance.getId();
    for (int i = 0; i < 3; i++) {
      EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
      assertEquals("a", getProcessInstanceState(processInstanceId));
    }
    signalToken(processInstance.getRootToken().getId());
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testCancel() throws Exception {
    deployProcessDefinition("<process-definition name='cancel'>"
        + "  <event type='timer'>"
        + "    <action expression='#{eventCallback.timer}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='1 second' repeat='1 second' />"
        + "    <transition to='b' />"
        + "  </state>"
        + "  <state name='b'>"
        + "    <transition to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    ProcessInstance processInstance = startProcessInstance("cancel");
    long processInstanceId = processInstance.getId();
    // first expiration
    EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
    assertEquals("a", getProcessInstanceState(processInstanceId));
    // repeated expiration
    EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
    assertEquals("a", getProcessInstanceState(processInstanceId));
    // cancel timer
    long rootTokenId = processInstance.getRootToken().getId();
    signalToken(rootTokenId);
    // no more expirations
    try {
      EventCallback.waitForEvent(Event.EVENTTYPE_TIMER, 1000);
      System.out.println("canceled timer fired again, probably due to race condition");
      EventCallback.waitForEvent(Event.EVENTTYPE_TIMER, 1000);
      fail("expected timeout exception");
    }
    catch (org.jbpm.JbpmException e) {
      // timeout exception was expected
    }
    // proceed to end state
    signalToken(rootTokenId);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testScheduleSequence() throws Exception {
    deployProcessDefinition("<process-definition name='sequence'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <event type='timer'>"
        + "    <action expression='#{eventCallback.timer}'/>"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='b' />"
        + "  </state>"
        + "  <state name='b'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='c' />"
        + "  </state>"
        + "  <state name='c'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='d' />"
        + "  </state>"
        + "  <state name='d'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='e' />"
        + "  </state>"
        + "  <state name='e'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processInstanceId = startProcessInstance("sequence").getId();
    for (char state = 'b'; state <= 'e'; state++) {
      EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
      assertEquals(Character.toString(state), getProcessInstanceState(processInstanceId));
    }
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testScheduleFork() throws Exception {
    deployProcessDefinition("<process-definition name='fork'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='f' />"
        + "  </start-state>"
        + "  <fork name='f'>"
        + "    <transition name='a' to='a' />"
        + "    <transition name='b' to='b' />"
        + "    <transition name='c' to='c' />"
        + "    <transition name='d' to='d' />"
        + "    <transition name='e' to='e' />"
        + "  </fork>"
        + "  <state name='a'>"
        + "    <timer duedate='0 seconds' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='b'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='c'>"
        + "    <timer duedate='2 seconds' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='d'>"
        + "    <timer duedate='3 seconds' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='e'>"
        + "    <timer duedate='4 seconds' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <join name='j' async='exclusive' lock='UPGRADE'>"
        + "    <transition to='end' />"
        + "  </join>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processInstanceId = startProcessInstance("fork").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  private String getProcessInstanceState(final long processInstanceId) throws RemoteException {
    return (String) commandService.execute(new Command() {
      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        return jbpmContext
            .loadProcessInstance(processInstanceId)
            .getRootToken()
            .getNode()
            .getName();
      }
    });
  }
}
