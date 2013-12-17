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
package org.jbpm.graph.exe;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;

public class SubProcessPlusConcurrencyDbTest extends AbstractDbTestCase {
    
  protected String getJbpmTestConfig() {
    return "org/jbpm/graph/node/sub-process-async.cfg.xml";
  }    

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition subProcess = ProcessDefinition.parseXmlString("<process-definition name='sub'>"
      + "  <start-state>"
      + "    <transition to='wait' />"
      + "  </start-state>"
      + "  <state name='wait'>"
      + "    <transition to='end' />"
      + "  </state>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(subProcess);

    ProcessDefinition superProcess = ProcessDefinition.parseXmlString("<process-definition name='super'>"
      + "  <start-state>"
      + "    <transition name='without subprocess' to='fork' />"
      + "    <transition name='with subprocess' to='subprocess' />"
      + "  </start-state>"
      + "  <process-state name='subprocess'>"
      + "    <sub-process name='sub' />"
      + "    <transition to='fork'/>"
      + "  </process-state>"
      + "  <fork name='fork'>"
      + "    <transition name='a' to='join' />"
      + "    <transition name='b' to='join' />"
      + "  </fork>"
      + "  <join name='join' lock='UPGRADE'>"
      + "    <transition to='s' />"
      + "  </join>"
      + "  <state name='s'>"
      + "    <event type='node-enter'>"
      + "      <action class='org.jbpm.graph.exe.SubProcessPlusConcurrencyDbTest$EnterNodeS' />"
      + "    </event>"
      + "  </state>"
      + "</process-definition>");
    deployProcessDefinition(superProcess);
  }

  public static class EnterNodeS implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      ContextInstance contextInstance = executionContext.getContextInstance();
      Integer invocationCount = (Integer) contextInstance.getVariable("invocationCount");
      if (invocationCount == null) {
        contextInstance.setVariable("invocationCount", new Integer(1));
      }
      else {
        contextInstance.setVariable("invocationCount", new Integer(
          invocationCount.intValue() + 1));
      }
    }
  }

  public void testWithoutSubProcess() {
    ProcessInstance pi = jbpmContext.newProcessInstanceForUpdate("super");
    pi.signal("without subprocess");
    assertEquals("s", pi.getRootToken().getNode().getName());
    assertEquals(new Integer(1), pi.getContextInstance().getVariable("invocationCount"));
  }

  public void testWithSubProcess() {
    ProcessInstance pi = jbpmContext.newProcessInstanceForUpdate("super");
    pi.signal("with subprocess");
    long pid = pi.getId();

    processJobs();
    pi = jbpmContext.loadProcessInstance(pid);
    ProcessInstance subPi = pi.getRootToken().getSubProcessInstance();
    assertEquals("wait", subPi.getRootToken().getNode().getName());

    subPi = jbpmContext.loadProcessInstanceForUpdate(subPi.getId());
    subPi.signal();

    processJobs();
    pi = jbpmContext.loadProcessInstance(pid);
    assertEquals("s", pi.getRootToken().getNode().getName());
    assertEquals(new Integer(1), pi.getContextInstance().getVariable("invocationCount"));
  }
}
