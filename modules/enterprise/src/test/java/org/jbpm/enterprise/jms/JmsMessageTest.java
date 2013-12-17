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
package org.jbpm.enterprise.jms;

import junit.framework.Test;

import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.enterprise.AbstractEnterpriseTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.msg.jms.JmsMessageService;

/**
 * Exercises for the {@linkplain JmsMessageService JMS message service}.
 * 
 * @author Alejandro Guizar
 */
public class JmsMessageTest extends AbstractEnterpriseTestCase {

  private static final int PROCESS_INSTANCE_COUNT = 5;

  public static Test suite() throws Exception {
    return new IntegrationTestSetup(JmsMessageTest.class, "enterprise-test.war");
  }

  public void testAsyncNode() {
    deployProcessDefinition("<process-definition name='node'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    long processInstanceId = startProcessInstance("node").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testAsyncAction() {
    deployProcessDefinition("<process-definition name='action'>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a'>"
        + "    <event type='node-enter'>"
        + "      <action async='true' expression='#{eventCallback.nodeEnter}' />"
        + "    </event>"
        + "    <event type='node-leave'>"
        + "      <action async='true' expression='#{eventCallback.nodeLeave}' />"
        + "    </event>"
        + "    <transition to='end'>"
        + "      <action async='true' expression='#{eventCallback.transition}' />"
        + "    </transition>"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    long processInstanceId = startProcessInstance("action").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_NODE_ENTER);
    EventCallback.waitForEvent(Event.EVENTTYPE_NODE_LEAVE);
    EventCallback.waitForEvent(Event.EVENTTYPE_TRANSITION);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testAsyncSequence() {
    deployProcessDefinition("<process-definition name='sequence'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <transition to='b' />"
        + "  </node>"
        + "  <node name='b' async='true'>"
        + "    <transition to='c' />"
        + "  </node>"
        + "  <node name='c' async='true'>"
        + "    <transition to='d' />"
        + "  </node>"
        + "  <node name='d' async='true'>"
        + "    <transition to='e' />"
        + "  </node>"
        + "  <node name='e' async='true'>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    long processInstanceId = startProcessInstance("sequence").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testAsyncFork() throws Exception {
    // [JBPM-1811] JmsMessageTest fails intermittently on HSQLDB
    if (getHibernateDialect().contains("HSQL")) {
      return;
    }

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
        + "  <node name='a'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='b' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='c' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='d' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='e' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <join name='j' async='exclusive'>"
        + "    <transition to='end' />"
        + "  </join>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    long processInstanceId = startProcessInstance("fork").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("expected process instance " + processInstanceId + " to have ended",
        hasProcessInstanceEnded(processInstanceId));
  }

  public void testAsyncExecutions() {
    // [JBPM-1811] JmsMessageTest fails intermittently on HSQLDB
    if (getHibernateDialect().contains("HSQL")) {
      return;
    }

    deployProcessDefinition("<process-definition name='execution'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}' />"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <transition to='b' />"
        + "  </node>"
        + "  <node name='b'>"
        + "    <event type='node-enter'>"
        + "      <action async='exclusive' expression='#{eventCallback.nodeEnter}' />"
        + "    </event>"
        + "    <transition to='c' />"
        + "  </node>"
        + "  <node name='c' async='exclusive'>"
        + "    <transition to='d' />"
        + "  </node>"
        + "  <node name='d'>"
        + "    <event type='node-leave'>"
        + "      <action async='exclusive' expression='#{eventCallback.nodeLeave}' />"
        + "    </event>"
        + "    <transition to='e' />"
        + "  </node>"
        + "  <node name='e' async='exclusive'>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");

    long[] processInstanceIds = new long[PROCESS_INSTANCE_COUNT];
    for (int i = 0; i < PROCESS_INSTANCE_COUNT; i++) {
      processInstanceIds[i] = startProcessInstance("execution").getId();
      EventCallback.waitForEvent(Event.EVENTTYPE_NODE_ENTER);
    }

    EventCallback.waitForEvent(PROCESS_INSTANCE_COUNT, Event.EVENTTYPE_NODE_LEAVE);
    EventCallback.waitForEvent(PROCESS_INSTANCE_COUNT, Event.EVENTTYPE_PROCESS_END);

    for (int i = 0; i < PROCESS_INSTANCE_COUNT; i++) {
      long processInstanceId = processInstanceIds[i];
      assertTrue("expected process instance " + processInstanceId + " to have ended",
          hasProcessInstanceEnded(processInstanceId));
    }
  }
}
