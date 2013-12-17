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
package org.jbpm.jbpm2691;

import java.util.List;

import org.jbpm.JbpmConfiguration;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Job;

/**
 * Save exceptions thrown while executing a job in a separate transaction.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2691">JBPM-2691</a>
 * @author Alejandro Guizar
 */
public class JBPM2691Test extends AbstractDbTestCase {

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
        + "  <int name='jbpm.job.retries' value='1' />"
        + "</jbpm-configuration>");
    }
    return jbpmConfiguration;
  }

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2691/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testSaveExceptionInSeparateTx() {
    ProcessInstance processInstance = jbpmContext.newProcessInstance("jobex");
    processInstance.signal();

    processJobs();
    processInstance = jbpmContext.getProcessInstance(processInstance.getId());
    Token rootToken = processInstance.getRootToken();
    List jobs = jobSession.findJobsByToken(rootToken);
    assertEquals(1, jobs.size());

    Job job = (Job) jobs.get(0);
    String exception = job.getException();
    assertNotNull(exception);
    assert exception.indexOf("boom") != -1 : exception;

    // exception should leave job in its original state
    assertEquals("async", rootToken.getNode().getName());
  }
}
