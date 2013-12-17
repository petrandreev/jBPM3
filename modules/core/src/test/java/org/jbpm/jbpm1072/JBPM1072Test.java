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
package org.jbpm.jbpm1072;

import java.util.concurrent.atomic.AtomicInteger;

import org.jbpm.JbpmConfiguration;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.executor.JobExecutor;

/**
 * Concurrent job executors can process the same job in parallel. The test simulates multiple nodes
 * in the network processing a common job set. The key setting is to give each job executor a
 * different name; normally, a job executor is named after the node's inet address
 * https://jira.jboss.org/jira/browse/JBPM-1072
 * 
 * @author Jiri Pechanec
 * @author Alejandro Guizar
 */
public class JBPM1072Test extends AbstractDbTestCase {

  private static final int JOB_EXECUTOR_COUNT = 4;

  private JobExecutor[] jobExecutors = new JobExecutor[JOB_EXECUTOR_COUNT];
  private long processDefinitionId;

  private static final String PROCESS_DEFINITION = "<process-definition name='job-executors'>"
      + "<event type='process-end'>"
      + "<action expression='#{eventCallback.processEnd}' />"
      + "</event>"
      + "<start-state name='start-state1'>"
      + "<transition to='Service 1'></transition>"
      + "</start-state>"
      + "<node name='Service 1' async='true'>"
      + "<action class='"
      + Counter.class.getName()
      + "' />"
      + "<transition to='Service 2' />"
      + "</node>"
      + "<node name='Service 2' async='true'>"
      + "<action class='"
      + Counter.class.getName()
      + "' />"
      + "<transition to='end-state1' />"
      + "</node>"
      + "<end-state name='end-state1' />"
      + "</process-definition>";

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(PROCESS_DEFINITION);
    jbpmContext.deployProcessDefinition(processDefinition);
    newTransaction();
    processDefinitionId = processDefinition.getId();

    startJobExecutors();
  }

  @Override
  protected void tearDown() throws Exception {
    stopJobExecutors();
    graphSession.deleteProcessDefinition(processDefinitionId);
    super.tearDown();

    EventCallback.clear();
  }

  public void testMultipleJobExecutors() {
    // Won't Fix [JBPM-1072] Concurrent JobExecutors can process the same job in parallel
    if (getHibernateDialect().indexOf("HSQL") != -1) return;

    // kick off process instance
    ProcessDefinition processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("eventCallback", new EventCallback());
    processInstance.signal();
    jbpmContext.save(processInstance);

    commitAndCloseSession();
    try {
      EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
      assertEquals(2, Counter.getCount());

      waitForJobs(EventCallback.DEFAULT_TIMEOUT);
    }
    finally {
      beginSessionTransaction();
    }
  }

  private void startJobExecutors() {
    for (int i = 0; i < jobExecutors.length; i++) {
      jobExecutors[i] = (JobExecutor) JbpmConfiguration.Configs.getObject("jbpm.job.executor");
      jobExecutors[i].setName("JbpmJobExecutor/" + i);
      jobExecutors[i].start();
    }
  }

  private void stopJobExecutors() {
    for (int i = jobExecutors.length - 1; i >= 0; i--) {
      try {
        jobExecutors[i].stopAndJoin();
      }
      catch (InterruptedException e) {
        // continue to next executor
      }
    }
  }

  public static class Counter implements ActionHandler {

    private static final AtomicInteger count = new AtomicInteger();

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext exeContext) throws Exception {
      count.incrementAndGet();
      exeContext.leaveNode();
    }

    public static int getCount() {
      return count.get();
    }
  }
}
