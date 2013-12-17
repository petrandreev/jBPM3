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

import java.util.ArrayList;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.ProcessDefinition;

public class ExceptionHandlingTest extends AbstractJbpmTestCase
{

  static List executedActions = null;

  protected void setUp() throws Exception
  {
    super.setUp();
    executedActions = new ArrayList();
  }

  public static class BatterException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  public static class Batter implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
      throw new BatterException();
    }
  }

  public static class Pitcher implements ActionHandler
  {
    private static final long serialVersionUID = 1L;
    Throwable exception = null;

    public void execute(ExecutionContext executionContext) throws DelegationException
    {
      this.exception = executionContext.getException();
      executedActions.add(this);
    }
  }

  public void testUncaughtException()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />" + "    </event>" + "  </state>" + "</process-definition>");

    try
    {
      new ProcessInstance(processDefinition).signal();
      fail("expected exception");
    }
    catch (DelegationException e)
    {
      assertSame(BatterException.class, e.getCause().getClass());
    }
  }

  public static class RuntimeBatter implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
      throw new RuntimeException("here i come");
    }
  }

  public void testUncaughtRuntimeException()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$RuntimeBatter' />" + "    </event>" + "  </state>" + "</process-definition>");

    try
    {
      new ProcessInstance(processDefinition).signal();
      fail("expected exception");
    }
    catch (DelegationException e)
    {
      assertSame(RuntimeException.class, e.getCause().getClass());
    }
  }

  public static class ErrorBatter implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
      throw new Error("jvm trouble coming your way");
    }
  }

  public void testUncaughtError()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$ErrorBatter' />" + "    </event>" + "  </state>" + "</process-definition>");

    try
    {
      new ProcessInstance(processDefinition).signal();
      fail("expected exception");
    }
    catch (DelegationException e)
    {
      fail("i don't want a delegation exception.  jBPM should not handle Error's since that might lead to JVM halts");
    }
    catch (Error e)
    {
      // OK
    }
  }

  public void testSimpleCatchAll()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />" + "    </event>" + "    <exception-handler>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Pitcher'/>" + "    </exception-handler>" + "  </state>" + "</process-definition>");

    new ProcessInstance(processDefinition).signal();
    assertEquals(1, executedActions.size());
    Pitcher executedPitcher = (Pitcher)executedActions.get(0);
    assertSame(BatterException.class, executedPitcher.exception.getClass());
  }

  public void testCatchOnlyTheSpecifiedException()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />" + "    </event>"
        + "    <exception-handler exception-class='org.jbpm.graph.exe.ExceptionHandlingTest$BatterException'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Pitcher'/>" + "    </exception-handler>" + "  </state>" + "</process-definition>");

    new ProcessInstance(processDefinition).signal();
  }

  public void testDontCatchTheNonSpecifiedException()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />" + "    </event>"
        + "    <exception-handler exception-class='java.lang.RuntimeException'>" + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Pitcher'/>"
        + "    </exception-handler>" + "  </state>" + "</process-definition>");

    try
    {
      new ProcessInstance(processDefinition).signal();
    }
    catch (DelegationException e)
    {
      assertSame(BatterException.class, e.getCause().getClass());
    }
  }

  public static class SecondExceptionHandler implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws DelegationException
    {
      executedActions.add(this);
    }
  }

  public void testCatchWithTheSecondSpecifiedExceptionHandler()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />"
        + "    </event>"
        +
        // the first exception-handler will not catch the BatterException
        "    <exception-handler exception-class='java.lang.RuntimeException'>" + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Pitcher'/>"
        + "    </exception-handler>"
        +
        // but the second exception-handler will catch all
        "    <exception-handler>" + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$SecondExceptionHandler'/>" + "    </exception-handler>"
        + "  </state>" + "</process-definition>");

    new ProcessInstance(processDefinition).signal();
    assertEquals(1, executedActions.size());
    assertSame(SecondExceptionHandler.class, executedActions.get(0).getClass());
  }

  public void testTwoActionsInOneExceptionHandler()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />" + "    </event>" + "    <exception-handler>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Pitcher'/>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$SecondExceptionHandler'/>" + "    </exception-handler>" + "  </state>"
        + "</process-definition>");

    new ProcessInstance(processDefinition).signal();
    assertEquals(2, executedActions.size());
    assertSame(Pitcher.class, executedActions.get(0).getClass());
    assertSame(SecondExceptionHandler.class, executedActions.get(1).getClass());
  }

  public void testProcessDefinitionExceptionHandling()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='play ball' />"
        + "  </start-state>" + "  <state name='play ball'>" + "    <event type='node-enter'>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />" + "    </event>" + "  </state>" + "  <exception-handler>"
        + "    <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Pitcher'/>" + "  </exception-handler>" + "</process-definition>");

    new ProcessInstance(processDefinition).signal();
    assertEquals(1, executedActions.size());
  }

  public void testSuperStateExceptionHandling()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state>" + "    <transition to='superstate/play ball' />"
        + "  </start-state>" + "  <super-state name='superstate'>" + "    <state name='play ball'>" + "      <event type='node-enter'>"
        + "        <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Batter' />" + "      </event>" + "    </state>" + "    <exception-handler>"
        + "      <action class='org.jbpm.graph.exe.ExceptionHandlingTest$Pitcher'/>" + "    </exception-handler>" + "  </super-state>" + "</process-definition>");

    new ProcessInstance(processDefinition).signal();
    assertEquals(1, executedActions.size());
  }
}
