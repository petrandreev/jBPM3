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
package org.jbpm.jbpm2834;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.State;

/**
 * {@link Node#getDefaultLeavingTransition()} returns <code>null</code> when
 * there is no unconditional transition.
 *
 * This test is no longer valid because transitions on conditions are only valid 
 *  on transitions leaving Decisions. Comments explaining this were added to the 
 *  jira issue (Marco Rietveld, May 18, 2011) 
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2834">JBPM-2834</a>
 * @author Alejandro Guizar
 */
public class JBPM2834Test extends AbstractJbpmTestCase {

  public void testDummyTest() {
    assertTrue(true);
  }
  
  public void ignoreTestConditionalDefaultLeavingTransition() {
    // parse definition
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2834/processdefinition.xml");
    // start instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    // check default leaving transition
    State state = (State) processInstance.getRootToken().getNode();
    assertEquals("default", state.getDefaultLeavingTransition().getName());
    // check condition is still enforced
    try {
      processInstance.signal();
      fail("expected condition to be enforced");
    }
    catch (JbpmException e) {
      assert e.getMessage().indexOf("condition") != -1 : e;
    }
    // satisfy condition
    processInstance.getContextInstance().setVariable("go", Boolean.TRUE);
    processInstance.signal();
    assert processInstance.hasEnded() : "expected " + processInstance + " to have ended";
  }
}
