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
package org.jbpm.jbpm1452;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.configuration.ObjectFactory;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * JbpmConfiguration assumes the object factory has a jbpm.job.executor entry
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1452
 * 
 * @author Alejandro Guizar
 */
public class JBPM1452Test extends AbstractDbTestCase {

  private long processDefinitionId;

  @Override
  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
          + "  <null name='jbpm.job.executor' />"
          + "</jbpm-configuration>");
    }
    return jbpmConfiguration;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = new ProcessDefinition("Audit");
    jbpmContext.deployProcessDefinition(processDefinition);
    processDefinitionId = processDefinition.getId();
  }

  @Override
  protected void tearDown() throws Exception {
    graphSession.deleteProcessDefinition(processDefinitionId);
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testNoJobExecutor() {
    // check the job executor is properly nullified
    ObjectFactory objectFactory = jbpmContext.getObjectFactory();
    assertTrue("expected object factory to have object jbpm.job.executor",
        objectFactory.hasObject("jbpm.job.executor"));
    assertNull(objectFactory.createObject("jbpm.job.executor"));
    // start and end a process instance, no exception should be thrown
    try {
      ProcessDefinition processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      jbpmContext.save(processInstance);

      newTransaction();
      processInstance.end();
    }
    catch (JbpmException e) {
      jbpmContext.setRollbackOnly();
      fail("could not end process instance in absence of job executor: " + e);
    }
  }
}
