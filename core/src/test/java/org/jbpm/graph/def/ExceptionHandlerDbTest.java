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
package org.jbpm.graph.def;

import java.util.List;

import org.jbpm.db.AbstractDbTestCase;

public class ExceptionHandlerDbTest extends AbstractDbTestCase {

  public void testExceptionClassName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <exception-handler exception-class='org.coincidence.FatalAttractionException' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    ExceptionHandler exceptionHandler = (ExceptionHandler) processDefinition.getExceptionHandlers()
      .get(0);
    assertNotNull(exceptionHandler);
    assertEquals("org.coincidence.FatalAttractionException", exceptionHandler.getExceptionClassName());
  }

  public void testExceptionHandlerProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <exception-handler exception-class='org.coincidence.FatalAttractionException' />"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    ExceptionHandler exceptionHandler = (ExceptionHandler) processDefinition.getExceptionHandlers()
      .get(0);
    assertSame(processDefinition, exceptionHandler.getGraphElement());
  }

  public void testExceptionHandlerNode() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='a'>"
      + "    <exception-handler exception-class='org.coincidence.FatalAttractionException' />"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Node node = processDefinition.getNode("a");
    ExceptionHandler exceptionHandler = (ExceptionHandler) node.getExceptionHandlers().get(0);
    assertSame(node, exceptionHandler.getGraphElement());
  }

  public void testExceptionHandlerTransition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='a'>"
      + "    <transition name='self' to='a'>"
      + "      <exception-handler exception-class='org.coincidence.FatalAttractionException' />"
      + "    </transition>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Transition transition = processDefinition.getNode("a").getLeavingTransition("self");
    ExceptionHandler exceptionHandler = (ExceptionHandler) transition.getExceptionHandlers()
      .get(0);
    assertSame(transition, exceptionHandler.getGraphElement());
  }

  public void testExceptionHandlerActions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <exception-handler exception-class='org.coincidence.FatalAttractionException'>"
      + "    <action class='one' />"
      + "    <action class='two' />"
      + "    <action class='three' />"
      + "    <action class='four' />"
      + "  </exception-handler>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    ExceptionHandler exceptionHandler = (ExceptionHandler) processDefinition.getExceptionHandlers()
      .get(0);
    List actions = exceptionHandler.getActions();
    assertEquals("one", ((Action) actions.get(0)).getActionDelegation().getClassName());
    assertEquals("two", ((Action) actions.get(1)).getActionDelegation().getClassName());
    assertEquals("three", ((Action) actions.get(2)).getActionDelegation().getClassName());
    assertEquals("four", ((Action) actions.get(3)).getActionDelegation().getClassName());
  }
}
