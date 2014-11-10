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
package org.jbpm.jbpm2812;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Test to verify timer execution inside a super-state
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2812">JBPM-2812</a>
 * @author Martin Putz
 */
public class JBPM2812Test extends AbstractDbTestCase {

  public void testTimerWithSuperState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='jbpm2812'>"
      + "  <start-state name='start'>"
      + "    <transition to='super-state/state1'/>"
      + "  </start-state>"
      + "  <super-state name='super-state'>"
      + "    <state name='state1'>"
      + "      <timer duedate='1 second' name='timeout-timer' transition='timeout'/>"
      + "      <transition to='state2' name='go'/>"
      + "    </state>"
      + "    <state name='state2'>"
      + "  	 <transition to='end' name='go'/>"
      + "    </state>"
      + "    <transition to='timed-out-end' name='timeout'/>"
      + "  </super-state>"
      + "  <end-state name='timed-out-end'/>"
      + "  <end-state name='end'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertEquals("state1", processInstance.getRootToken().getNode().getName());

    processJobs();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assertTrue("expected " + processInstance + " to have ended", processInstance.hasEnded());
    assertEquals("timed-out-end", processInstance.getRootToken().getNode().getName());
  }
}
