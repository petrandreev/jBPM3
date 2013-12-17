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
package org.jbpm.db;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.ExecuteNodeJob;
import org.jbpm.job.Job;

public class JobSessionDbTest extends AbstractDbTestCase {
  
  public static final int timeout = 60000;

  public static class FailingAction implements ActionHandler {
    private static final long serialVersionUID = 1L;
    
    public void execute(ExecutionContext executionContext) throws Exception {
      throw new RuntimeException("TEST-EXCEPTION");
    }    
  }

  /**
   * Test case which generates a {@link Job} ({@link ExecuteNodeJob} via async=true)
   * which causes an exception. Afterwards it is checked if this job 
   * is found by the getFailedJobs method
   */
  public void testLoadFailedJobs() throws Exception {
    String xml = 
        "<process-definition name='TestJob'>"
      + " <start-state>"
      + "   <transition to='async state' />"
      + " </start-state>"
      + " <node name='async state' async='true'>"
      + "   <transition to='end'>"
      + "     <action name='throw exception' class='org.jbpm.db.JobSessionDbTest$FailingAction' />"
      + "   </transition>"
      + " </node>"
      + " <end-state name='end' />"
      + "</process-definition>";
    
    // create a process definition
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(xml);
    // save it in the database
    graphSession.deployProcessDefinition(processDefinition);
    
    newTransaction();
    try {      
      // start a new process instance and signal it
      ProcessInstance pi = jbpmContext.newProcessInstance("TestJob");
      pi.getRootToken().signal();
      jbpmContext.save(pi);
      
      newTransaction();
      
      // job is created now, but not yet executed
      assertEquals(1, getNbrOfJobsAvailable());      
      assertEquals(0, jobSession.findFailedJobs().size());

      // start job executor wait for job to be executed
      // and failure is written to database
      commitAndCloseSession();
      try {
        startJobExecutor();

        long startTime = System.currentTimeMillis();
        while( getFailedJobCount() <= 0 ) {
          if (System.currentTimeMillis() - startTime > timeout) {
            fail("test execution exceeded threshold of " + timeout + " milliseconds");
          }
          Thread.sleep(500);
        }
      }
      finally {
        stopJobExecutor();
        beginSessionTransaction();
      }
      
      List<Job> failedJobs = jobSession.findFailedJobs();

      // now the one job we have should be failed
      assertEquals(1, getNbrOfJobsAvailable());
      assertEquals(1, failedJobs.size());
      
      // and information is set on the job
      Job job = failedJobs.get(0);
      assertEquals(0, job.getRetries());

      String exception = job.getException();
      assertNotNull("expected job.exception not to be null", exception);
      assertTrue("expected job.exception to contain TEST-EXCEPTION",
          exception.contains("TEST-EXCEPTION"));    
    }
    finally {
      // cleanup
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  int getFailedJobCount() {
    beginSessionTransaction();
    try {
      return jobSession.findFailedJobs().size();
    }
    finally {
      commitAndCloseSession();
    }
  }

}