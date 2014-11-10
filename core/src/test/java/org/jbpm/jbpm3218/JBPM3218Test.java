/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, JBoss Inc., and individual contributors as indicated
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
package org.jbpm.jbpm3218;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.command.CommandService;
import org.jbpm.command.ExecuteJobsCommand;
import org.jbpm.command.impl.CommandServiceImpl;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Timer;

/**
 * ExecuteJobsCommand uses a hardcoded number of retries (instead of injected jbpm.job.retries value) 
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-3218">JBPM-3218</a>
 */
public class JBPM3218Test extends AbstractDbTestCase {

  private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_HH:mm:ss");
  private CommandService commandService = new CommandServiceImpl(getJbpmConfiguration());

  protected void setUp() throws Exception {
    super.setUp();
    
    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    deployProcessDefinition(processDefinition);
  }

  public void testStartDate() {
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(getName());

    // Create and persist timer.. 
    Timer timer = new Timer(processInstance.getRootToken());
    timer.setName("jbpm3218test:" + dateFormat.format(new Date()));
    timer.setDueDate(getOneHourAgo());
    // Force retry handling
    timer.setException("simulated Exception");
    // Withouth repeat, the job is not available to retrieve after execution
    timer.setRepeat("1 week");
    jbpmContext.getServices().getSchedulerService().createTimer(timer);
    newTransaction(); 
    
    ExecuteJobsCommand overdueJobsCommand = new ExecuteJobsCommand();
    commandService.execute(overdueJobsCommand);
    
    timer = jobSession.loadTimer(timer.getId());
    int retries = timer.getRetries();
  
    String retriesProperty = "jbpm.job.retries";
    int configRetries = 0;
    try { 
      configRetries = Configs.getInt(retriesProperty);
    }
    catch(Exception e ) { 
      fail(retriesProperty + " could not be retrieved.");
    }
    assertTrue("expected " + (configRetries-1) + " retries, not " + retries, retries == (configRetries-1));
  }

  private Date getOneHourAgo() { 
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR, -1);
    // databases such as mysql do not have millisecond precision
    calendar.set(Calendar.MILLISECOND, 0);
    Date oneHourAgo = calendar.getTime();
    return oneHourAgo;
  }

}
