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
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ExceptionHandlerTest extends AbstractJbpmTestCase {

  public static class NoExceptionAction implements ActionHandler
  {
    public void execute(ExecutionContext executionContext) throws Exception
    {
    }
  }

  public static class ThrowExceptionAction implements ActionHandler
  {
    public void execute(ExecutionContext executionContext) throws Exception
    {
      throw new Exception("exception in action handler");
    }
  }

  public static class ThrowInnerExceptionAction implements ActionHandler
  {
    public void execute(ExecutionContext executionContext) throws Exception
    {
      throw new Exception("exception inside of exception handler");
    }
  }
  
  public void testExceptionHandlerThrowingExcption() 
  {
   
    String xml = 
      "<?xml version='1.0' encoding='UTF-8'?>"
      +"<process-definition name='TestException'>"
      +"   <start-state name='start'>"
      +"      <transition name='to_state' to='first'>"
      +"         <action class='org.jbpm.graph.def.ExceptionHandlerTest$ThrowExceptionAction' />"
      +"      </transition>"
      +"   </start-state>   "
      +"   <state name='first'>"
      +"      <transition to='end' />"
      +"   </state>  "
      +"   <end-state name='end' />"
      +"   <exception-handler>"
      +"      <action class='org.jbpm.graph.def.ExceptionHandlerTest$ThrowInnerExceptionAction' />"
      +"   </exception-handler>"
      +"</process-definition>";
    
    ProcessDefinition def = ProcessDefinition.parseXmlString(xml);
    ProcessInstance pi = def.createProcessInstance();

    try
    {
      pi.getRootToken().signal();
    }
    catch (DelegationException ex)
    {
      // check that exception is thrown to the client nested in a DelegationException
      assertEquals("exception inside of exception handler", ex.getCause().getMessage());
    }
  }
  
  public void testMissingExceptionHandlerClass() {
    String xml = 
      "<?xml version='1.0' encoding='UTF-8'?>"
      +"<process-definition name='TestException'>"
      +"   <start-state name='start'>"
      +"      <transition name='to_state' to='first'>"
      +"         <action class='org.jbpm.graph.def.ExceptionHandlerTest$ThrowExceptionAction' />"
      +"      </transition>"
      +"   </start-state>   "
      +"   <state name='first'>"
      +"      <transition to='end' />"
      +"   </state>  "
      +"   <end-state name='end' />"
      +"   <exception-handler>"
      +"      <action class='org.jbpm.graph.def.ExceptionHandlerTest$DOESNTEXIST' />"
      +"   </exception-handler>"
      +"</process-definition>";
    
    ProcessDefinition def = ProcessDefinition.parseXmlString(xml);   
    ProcessInstance pi = def.createProcessInstance();
    
    try
    {
      pi.getRootToken().signal();
    }
    catch (DelegationException ex)
    {
      // check that exception is thrown to the client nested in a DelegationException
      assertEquals(ClassNotFoundException.class, ex.getCause().getClass());
    }
  }

  public void testNoException() {
    String xml = 
      "<?xml version='1.0' encoding='UTF-8'?>"
      +"<process-definition name='TestException'>"
      +"   <start-state name='start'>"
      +"      <transition name='to_state' to='first'>"
      +"         <action class='org.jbpm.graph.def.ExceptionHandlerTest$ThrowExceptionAction' />"
      +"      </transition>"
      +"   </start-state>   "
      +"   <state name='first'>"
      +"      <transition to='end' />"
      +"   </state>  "
      +"   <end-state name='end' />"
      +"   <exception-handler>"
      +"      <action class='org.jbpm.graph.def.ExceptionHandlerTest$NoExceptionAction' />"
      +"   </exception-handler>"
      +"</process-definition>";

    ProcessDefinition def = ProcessDefinition.parseXmlString(xml);
    ProcessInstance pi = def.createProcessInstance();
    pi.getRootToken().signal();
    
    // exception is handled correctly    
  } 
}
