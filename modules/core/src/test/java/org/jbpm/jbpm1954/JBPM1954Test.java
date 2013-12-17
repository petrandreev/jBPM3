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
package org.jbpm.jbpm1954;

import org.hibernate.HibernateException;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.DecisionHandler;

/**
 * JbpmException thrown from Decision after exception in delegation class
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1954">JBPM-1954</a>
 * @author Alejandro Guizar
 */
public class JBPM1954Test extends AbstractJbpmTestCase {

  public void testHandleExceptionInDecisionHandler() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='jbpm1954'>"
        + "  <start-state>"
        + "    <transition to='decision' />"
        + "  </start-state>"
        + "  <decision name='decision'>"
        + "    <handler class='"
        + ProblematicDecisionHandler.class.getName()
        + "' />"
        + "    <exception-handler exception-class='org.hibernate.HibernateException'>"
        + "      <script>System.out.println(executionContext.getException().getMessage());</script>"
        + "    </exception-handler>"
        + "    <transition to='end' />"
        + "  </decision>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertTrue("expected " + processInstance + " to have ended", processInstance.hasEnded());
  }

  public void testHandleExceptionInDecisionHandlerAndMoveToken() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='jbpm1954'>"
        + "  <start-state>"
        + "    <transition to='decision' />"
        + "  </start-state>"
        + "  <decision name='decision'>"
        + "    <handler class='"
        + ProblematicDecisionHandler.class.getName()
        + "' />"
        + "    <exception-handler exception-class='org.hibernate.HibernateException'>"
        + "      <script>token.setNode(executionContext.getProcessDefinition().getNode(\"error\"));</script>"
        + "    </exception-handler>"
        + "    <transition to='end' />"
        + "  </decision>"
        + "  <state name='error'/>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertSame(processDefinition.getNode("error"), processInstance.getRootToken().getNode());
  }

  public static class ProblematicDecisionHandler implements DecisionHandler {
    private static final long serialVersionUID = 1L;

    public String decide(ExecutionContext executionContext) throws Exception {
      throw new HibernateException("simulated failure");
    }
  }
}
