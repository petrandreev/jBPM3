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

import java.util.concurrent.Semaphore;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * This test creates a number of process instances. Every instance has a call to an ActionHandler.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2043">JBPM-2043</a>
 * @author mvecera@redhat.com
 * @author pmacik@redhat.com
 * @author thomas.diesler@jboss.com
 * @author alex.guizar@jboss.com
 * @since 18-Feb-2009
 */
public class SimplePerformanceTest extends AbstractDbTestCase {

  private static final int WARMUP_INSTANCES = 100;
  private static final int MEASUREMENT_INSTANCES = 1000;

  private static final Semaphore signalLight = new Semaphore(0);

  private ProcessDefinition processDefinition;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    processDefinition = ProcessDefinition.parseXmlString("<process-definition name='perf'>"
        + "  <event type='process-start'>"
        + "    <action class='"
        + AsyncSignalAction.class.getName()
        + "' async='true'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='end'/>"
        + "  </start-state>"
        + "  <end-state name='end'/>"
        + "</process-definition>");
    jbpmContext.deployProcessDefinition(processDefinition);

    startJobExecutor();
  }

  @Override
  protected void tearDown() throws Exception {
    stopJobExecutor();
    jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    super.tearDown();
  }

  public void testAsyncCall() {
    // Won't Fix [JBPM-2043] Performance test coverage
    if (getHibernateDialect().indexOf("HSQL") != -1) return;

    launchProcessInstances(WARMUP_INSTANCES);

    long startTime = System.currentTimeMillis();
    launchProcessInstances(MEASUREMENT_INSTANCES);

    long duration = System.currentTimeMillis() - startTime;
    System.out.println("=== Test finished processing "
        + MEASUREMENT_INSTANCES
        + " instances in "
        + duration
        + "ms ===");
    System.out.println("=== This is "
        + Math.round(1000f * MEASUREMENT_INSTANCES / duration)
        + " instances per second ===");
  }

  private void launchProcessInstances(int count) {
    for (int i = 0; i < count; i++) {
      newTransaction();
      ProcessInstance pi = new ProcessInstance(processDefinition);
      jbpmContext.save(pi);
    }

    commitAndCloseSession();
    try {
      signalLight.acquire(count);
    }
    catch (InterruptedException e) {
      fail(getName() + " got interrupted while waiting for process instances to end");
    }
    finally {
      beginSessionTransaction();
    }
  }

  public static class AsyncSignalAction implements ActionHandler {
    private static final long serialVersionUID = -8617329370138396271L;

    public void execute(final ExecutionContext executionContext) throws Exception {
      signalLight.release();
      executionContext.leaveNode();
    }
  }
}
