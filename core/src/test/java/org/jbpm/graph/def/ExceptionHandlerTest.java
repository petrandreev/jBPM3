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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * @author bernd.ruecker@camunda.com
 */
public class ExceptionHandlerTest extends AbstractJbpmTestCase {

  public static class NoExceptionAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
    }
  }

  public static class ThrowExceptionAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      throw new Exception("exception in action handler");
    }
  }

  public static class ThrowInnerExceptionAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      throw new Exception("exception inside of exception handler");
    }
  }

  public void testExceptionHandlerThrowingException() {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>" +
        "<process-definition name='TestException'>" +
        "   <start-state name='start'>" +
        "      <transition to='end'>" +
        "         <action class='" +
        ThrowExceptionAction.class.getName() +
        "' />" +
        "      </transition>" +
        "   </start-state>   " +
        "   <end-state name='end' />" +
        "   <exception-handler>" +
        "      <action class='" +
        ThrowInnerExceptionAction.class.getName() +
        "' />" +
        "   </exception-handler>" +
        "</process-definition>";

    ProcessDefinition def = ProcessDefinition.parseXmlString(xml);
    ProcessInstance pi = def.createProcessInstance();

    try {
      pi.signal();
    }
    catch (DelegationException ex) {
      // check that exception is thrown to the client nested in a DelegationException
      assertEquals("exception inside of exception handler", ex.getCause().getMessage());
    }
  }

  public void testMissingExceptionHandlerClass() {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>" +
        "<process-definition name='TestException'>" +
        "   <start-state name='start'>" +
        "      <transition to='end'>" +
        "         <action class='" +
        ThrowExceptionAction.class.getName() +
        "' />" +
        "      </transition>" +
        "   </start-state>   " +
        "   <end-state name='end' />" +
        "   <exception-handler>" +
        "      <action class='org.jbpm.graph.def.ExceptionHandlerTest$DOESNOTEXIST' />" +
        "   </exception-handler>" +
        "</process-definition>";

    ProcessDefinition def = ProcessDefinition.parseXmlString(xml);
    ProcessInstance pi = def.createProcessInstance();

    try {
      pi.getRootToken().signal();
    }
    catch (DelegationException ex) {
      // check that exception is thrown to the client nested in a DelegationException
      assertSame(ClassNotFoundException.class, ex.getCause().getClass());
    }
  }

  public void testNoException() {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>" +
        "<process-definition name='TestException'>" +
        "   <start-state name='start'>" +
        "      <transition to='end'>" +
        "         <action class='" +
        ThrowExceptionAction.class.getName() +
        "' />" +
        "      </transition>" +
        "   </start-state>   " +
        "   <end-state name='end' />" +
        "   <exception-handler>" +
        "      <action class='" +
        NoExceptionAction.class.getName() +
        "' />" +
        "   </exception-handler>" +
        "</process-definition>";

    ProcessDefinition def = ProcessDefinition.parseXmlString(xml);
    ProcessInstance pi = def.createProcessInstance();
    pi.signal();

    // exception is handled correctly
    assertTrue("expected " + pi + " to have ended", pi.hasEnded());
  }

  /**
   * If exception handlers are defined in multiple nodes, only the first one is triggered
   * during one execution.
   * 
   * @see <a href="https://jira.jboss.org/browse/JBPM-2854">JBPM-2854</a>
   */
  public void testMultipleExceptionHandler() {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>" +
        "<process-definition name='TestException'>" +
        "   <start-state name='start'>" +
        "      <transition to='node1' />" +
        "   </start-state>   " +
        "   <node name='node1'>" +        
        "      <event type='node-enter'>" +
        "         <script>executionContext.setVariable(\"count\", 0)</script>" +
        "         <action class='" +
        ThrowExceptionAction.class.getName() +
        "' />" +
        "      </event>" +
        "      <exception-handler>" +
        "         <script>executionContext.setVariable(\"count\", count + 1)</script>" +
        "      </exception-handler>" +
        "      <transition to='node2' />" +
        "   </node>" +
        "   <node name='node2'>" +
        "      <event type='node-enter'>" +
        "         <action class='" +
        ThrowExceptionAction.class.getName() +
        "' />" +
        "      </event>" +
        "      <exception-handler>" +
        "         <script>executionContext.setVariable(\"count\", count + 1)</script>" +
        "      </exception-handler>" +
        "      <transition to='end' />" +
        "   </node>" +
        "   <end-state name='end' />" +
        "</process-definition>";

    ProcessDefinition def = ProcessDefinition.parseXmlString(xml);
    ProcessInstance pi = def.createProcessInstance();

    // should not throw DelegationException
    pi.signal();

    // two exceptions are handled
    Integer count = (Integer) pi.getContextInstance().getVariable("count");
    assertEquals(2, count.intValue());
  }
}
