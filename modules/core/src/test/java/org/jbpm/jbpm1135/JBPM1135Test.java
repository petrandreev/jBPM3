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
package org.jbpm.jbpm1135;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/** 
 * When a timer fires and another timer is created as part of its execution,
 * no exception should be thrown.
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1135
 * 
 * @author Alejandro Guizar
 */
public class JBPM1135Test extends AbstractDbTestCase {

  private long processDefinitionId;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm1135/timerprocess.xml");
    jbpmContext.deployProcessDefinition(processDefinition);
    processDefinitionId = processDefinition.getId();
    newTransaction();

    startJobExecutor();
  }

  @Override
  protected void tearDown() throws Exception {
    stopJobExecutor();
    graphSession.deleteProcessDefinition(processDefinitionId);
    super.tearDown();

    EventCallback.clear();
  }

  public void testTimerOnTimer() {
    ProcessDefinition processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("eventCallback", new EventCallback());
    processInstance.signal();
    jbpmContext.save(processInstance);
    assertEquals("firstNode", processInstance.getRootToken().getNode().getName());

    newTransaction();

    EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
    long processInstanceId = processInstance.getId();
    assertEquals("secondNode", jbpmContext.loadProcessInstance(processInstanceId)
        .getRootToken()
        .getNode()
        .getName());

    newTransaction();

    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue(jbpmContext.loadProcessInstance(processInstanceId).hasEnded());
  }
}
