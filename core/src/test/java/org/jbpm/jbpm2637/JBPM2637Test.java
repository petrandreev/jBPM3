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
package org.jbpm.jbpm2637;

import java.util.Collections;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

/**
 * {@link TaskMgmtSession#findTaskInstancesByIds(List)} fails if taskInstanceIds
 * is an empty list.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2637">JBPM-2637</a>
 * @author Alejandro Guizar
 */
public class JBPM2637Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = new ProcessDefinition("jbpm2637");
    processDefinition.addDefinition(new TaskMgmtDefinition());
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("jbpm2637");
    processInstance.getTaskMgmtInstance().createTaskInstance(processInstance.getRootToken());
    newTransaction();
  }

  public void testFindTaskInstancesEmptyActorList() {
    assertEquals(0, taskMgmtSession.findTaskInstances(Collections.EMPTY_LIST).size());
  }

  public void testFindTaskInstancesEmptyActorArray() {
    assertEquals(0, taskMgmtSession.findTaskInstances(new String[0]).size());
  }

  public void testFindPooledTaskInstancesEmptyActorList() {
    assertEquals(0, taskMgmtSession.findPooledTaskInstances(Collections.EMPTY_LIST).size());
  }

  public void testFindTaskInstancesEmptyIdList() {
    assertEquals(0, taskMgmtSession.findTaskInstancesByIds(Collections.EMPTY_LIST).size());
  }
}
