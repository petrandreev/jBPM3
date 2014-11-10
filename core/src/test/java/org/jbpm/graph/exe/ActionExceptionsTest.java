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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.ProcessDefinition;

public class ActionExceptionsTest extends AbstractJbpmTestCase {

  public static class FailingAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext context) {
      throw new IllegalArgumentException();
    }
  }

  public void testFailingReferenced() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='state'/>"
        + "  </start-state>"
        + "  <state name='state'>"
        + "    <transition to='end'>"
        + "      <action ref-name='failing'/>"
        + "    </transition>"
        + "  </state>"
        + "  <end-state name='end'/>"
        + "  <action name='failing' class='org.jbpm.graph.exe.ActionExceptionsTest$FailingAction'/>"
        + "</process-definition>");
    ProcessInstance pi = new ProcessInstance(processDefinition);

    pi.signal();
    try {
      pi.signal();
    }
    catch (DelegationException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      return;
    }
    fail("should have throws IllegalArgumentException");
  }

  public void testFailingNotReferenced() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='state'/>"
        + "  </start-state>"
        + "  <state name='state'>"
        + "    <transition to='end'>"
        + "      <action class='org.jbpm.graph.exe.ActionExceptionsTest$FailingAction'/>"
        + "    </transition>"
        + "  </state>"
        + "  <end-state name='end'/>"
        + "</process-definition>");
    ProcessInstance pi = new ProcessInstance(processDefinition);

    pi.signal();
    try {
      pi.signal();
    }
    catch (DelegationException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
      return;
    }
    fail("should have throws IllegalArgumentException");
  }

}
