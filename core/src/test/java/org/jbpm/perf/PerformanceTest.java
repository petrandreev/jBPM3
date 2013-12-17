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
package org.jbpm.perf;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * This test creates a number of process instances. Every instance has a call to
 * an ActionHandler.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2043">JBPM-2043</a>
 * @author mvecera@redhat.com
 * @author pmacik@redhat.com
 * @author Alejandro Guizar
 * @since 18-Feb-2009
 */
public class PerformanceTest extends AbstractDbTestCase {

  private static final int WARMUP_INSTANCES = 100;
  private static final int MEASURED_INSTANCES = 1000;

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='jbpm2043'>"
      + "  <start-state name='start'>"
      + "    <transition to='midway'/>"
      + "  </start-state>"
      + "  <node name='midway' async='true'>"
      + "    <transition to='end'/>"
      + "  </node>"
      + "  <end-state name='end'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);
  }

  public void testPerformance() {
    long firstTime = System.currentTimeMillis();
    launchProcessInstances(WARMUP_INSTANCES);
    processJobs();

    long secondTime = System.currentTimeMillis();
    launchProcessInstances(MEASURED_INSTANCES);
    processJobs((secondTime - firstTime) * (MEASURED_INSTANCES / WARMUP_INSTANCES) * 2);

    long duration = System.currentTimeMillis() - secondTime;
    System.out.println("### Processed "
      + (1000 * MEASURED_INSTANCES / duration)
      + " instances per second ###");
  }

  private void launchProcessInstances(int count) {
    for (int i = 0; i < count; i++) {
      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("jbpm2043");
      processInstance.signal();
      newTransaction();
    }
  }
}
