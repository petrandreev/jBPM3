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
package org.jbpm.jbpm2818;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Leaving node in super-state throws JbpmException: cannot leave Node without leaving
 * transition.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-2818">JBPM-2818</a>
 * @author Alejandro Guizar
 */
public class JBPM2818Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2818/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  public void testSuperStateLeavingTransition() {
    ProcessInstance processInstance = jbpmContext.newProcessInstance("jbpm2818");
    processInstance.signal();
    assertEquals("state1", processInstance.getRootToken().getNode().getName());

    processInstance = saveAndReload(processInstance);
    processInstance.signal();
    assertTrue("expected " + processInstance + " to have ended", processInstance.hasEnded());
  }
}
