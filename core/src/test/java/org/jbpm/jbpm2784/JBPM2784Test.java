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
package org.jbpm.jbpm2784;

import java.util.Random;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * JBPM-2784: XML schema does not accept condition attribute in transition.
 * 
 * @author Alejandro Guizar
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2784">JBPM-2784</a>
 */
public class JBPM2784Test extends AbstractJbpmTestCase {

  public void testTransitionConditionAttribute() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<?xml version='1.0'?>"
      + "<process-definition name='jbpm2784' xmlns='urn:jbpm.org:jpdl-3.2'>"
      + "  <start-state name='start'>"
      + "    <transition to='blackjack'/>"
      + "  </start-state>"
      + "  <decision name='blackjack'>"
      + "    <transition name='hit' to='hit' condition='${count &lt; 17}'/>"
      + "    <transition name='stand' to='stand'/>"
      + "  </decision>"
      + "  <end-state name='hit'/>"
      + "  <end-state name='stand'/>"
      + "</process-definition>");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);

    int count = new Random().nextInt(21) + 1;
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("count", new Integer(count));

    processInstance.signal();
    assertTrue("expected " + processInstance + " to have ended", processInstance.hasEnded());

    assertEquals(count < 17 ? "hit" : "stand", processInstance.getRootToken()
      .getNode()
      .getName());
  }
}
