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
package org.jbpm.jbpm3213;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Conditions on transitions from decisions.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2010">JBPM-2010</a>
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JBPM3213Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm3213/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  protected void tearDown() throws Exception {
    jbpmConfiguration.getJobExecutor().setNbrOfThreads(1);
    super.tearDown();
  }

  public void testConditionsOnDecisions() {
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("jbpm2010");
    try { 
      processInstance.signal();
      fail( "An exception should have been thrown because there are no viable guarded transitions.");
    }
    catch(Exception e ) { 
      String message = e.getMessage();
      assertTrue("Exception message should refer to failed condition.", message.matches("condition.*guarding Transition(.*) not met.*"));
    }

    long processInstanceId = processInstance.getId();

    processJobs();

    processInstance = jbpmContext.loadProcessInstance(processInstanceId);
    assertTrue("expected " + processInstance + " to have NOT ended", ! processInstance.hasEnded());
  }

}
