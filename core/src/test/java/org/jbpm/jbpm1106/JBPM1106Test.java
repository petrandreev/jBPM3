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
package org.jbpm.jbpm1106;

import java.util.Calendar;
import java.util.List;

import org.jbpm.command.CommandService;
import org.jbpm.command.GetProcessInstancesCommand;
import org.jbpm.command.impl.CommandServiceImpl;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * GetProcessInstancesCommand should set timestamp instead of date.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1106">JBPM-1106</a>
 * @author Alejandro Guizar
 */
public class JBPM1106Test extends AbstractDbTestCase {

  private CommandService commandService = new CommandServiceImpl(getJbpmConfiguration());

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = new ProcessDefinition("jbpm1106");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, 8, 10, 9, 30);
    processInstance.setStart(calendar.getTime());
    jbpmContext.save(processInstance);

    processInstance = new ProcessInstance(processDefinition);
    calendar.set(Calendar.HOUR_OF_DAY, 16);
    processInstance.setStart(calendar.getTime());
    jbpmContext.save(processInstance);

    processInstance = new ProcessInstance(processDefinition);
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    processInstance.setStart(calendar.getTime());
    jbpmContext.save(processInstance);

    processInstance = new ProcessInstance(processDefinition);
    calendar.set(Calendar.HOUR_OF_DAY, 9);
    processInstance.setStart(calendar.getTime());
    jbpmContext.save(processInstance);

    newTransaction();
  }

  public void testStartDate() {
    GetProcessInstancesCommand command = new GetProcessInstancesCommand();
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, 8, 10, 9, 0);
    command.setFromStartDate(calendar.getTime());
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    command.setUntilStartDate(calendar.getTime());

    List processInstances = (List) commandService.execute(command);
    assertEquals(2, processInstances.size());
  }

  public void testStartTime() {
    GetProcessInstancesCommand command = new GetProcessInstancesCommand();
    Calendar calendar = Calendar.getInstance();
    calendar.set(2008, 8, 10, 9, 0);
    command.setFromStartDate(calendar.getTime());
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    command.setUntilStartDate(calendar.getTime());

    List processInstances = (List) commandService.execute(command);
    assertEquals(1, processInstances.size());
  }

}
