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
package org.jbpm.jbpm2908;

import org.jbpm.JbpmConfiguration;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Load an alternate {@link JbpmConfiguration} and start a process instance from within an
 * action handler. This test chases a {@link StackOverflowError} or an infinite loop.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2908">JBPM-2908</a>
 * @author Toshiya Kobayashi
 */
public class JBPM2908Test extends AbstractDbTestCase {

  public void testChangingJbpmConfigurationInActionHandler() throws Exception {
    // deploy definition
    ProcessDefinition processDefinition1 = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2908/processdefinition1.xml");
    ProcessDefinition processDefinition2 = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2908/processdefinition2.xml");
    deployProcessDefinition(processDefinition1);
    deployProcessDefinition(processDefinition2);

    // start instance
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("process1");
    processInstance.signal();
    assert processInstance.hasEnded() : "expected " + processInstance + " to have ended";
  }
}
