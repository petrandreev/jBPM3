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
package org.jbpm.jbpm2489;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Process instance loses access to variables when timer is present inside fork
 * construct.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2489">JBPM-2489</a>
 * @author Alejandro Guizar
 */
public class JBPM2489Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2489/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  public void testLostAccessToVariableWithTimer() {
    ProcessInstance processInstance = jbpmContext.newProcessInstance("bpm_orchestration2");
    processInstance.getContextInstance().setVariable("theBody", getName().getBytes());
    processInstance.signal();
    jbpmContext.save(processInstance);

    processJobs();

    processInstance = jbpmContext.getProcessInstance(processInstance.getId());
    assertTrue("expected " + processInstance + " to have ended", processInstance.hasEnded());
  }
}
