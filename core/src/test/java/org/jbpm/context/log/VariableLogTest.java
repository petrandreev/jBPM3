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
package org.jbpm.context.log;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.bytes.ByteArray;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.context.log.variableinstance.LongUpdateLog;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.exe.LoggingInstance;

public class VariableLogTest extends AbstractJbpmTestCase {
  
  private ProcessDefinition processDefinition = null;
  private ProcessInstance processInstance = null;
  private ContextInstance contextInstance = null;
  private LoggingInstance loggingInstance = null;
  
  protected void setUp() throws Exception
  {
    super.setUp();
    processDefinition = new ProcessDefinition();
    processDefinition.addDefinition(new ContextDefinition());
    processInstance = new ProcessInstance( processDefinition );
    contextInstance = processInstance.getContextInstance();
    loggingInstance = processInstance.getLoggingInstance();
  }
  
  public void testVariableCreateLogs() {
    contextInstance.setVariable("a", new Integer(3));
    // // contextSession.updateProcessContextVariables(contextInstance);
    
    VariableCreateLog createLog = (VariableCreateLog)loggingInstance.getLogs(VariableCreateLog.class).get(0);
    assertEquals("a", createLog.getVariableInstance().getName());
    
    LongUpdateLog updateLog = (LongUpdateLog)loggingInstance.getLogs(LongUpdateLog.class).get(0);

    assertNull(updateLog.getOldValue());
    assertEquals(new Long(3), updateLog.getNewValue());
  }
  
  public void testByteArrayUpdateLog() {
    contextInstance.setVariable("a", "first value".getBytes());
    contextInstance.setVariable("a", "second value".getBytes());
    
    // System.out.println(loggingInstance.getLogs());
    
    List logs = loggingInstance.getLogs(VariableUpdateLog.class);
    VariableUpdateLog variableLog = (VariableUpdateLog)logs.get(1);
    
    assertTrue(Arrays.equals("first value".getBytes(), ((ByteArray) variableLog.getOldValue()).getBytes()));
    assertTrue(Arrays.equals("second value".getBytes(), ((ByteArray) variableLog.getNewValue()).getBytes()));
  }

  public void testDateUpdateLog() {
    Date now = new Date();
    Date future = new Date(now.getTime()+5);
    contextInstance.setVariable("a", now);
    contextInstance.setVariable("a", future);

    VariableUpdateLog variableLog = (VariableUpdateLog)loggingInstance.getLogs(VariableUpdateLog.class).get(1);
    
    assertEquals(now, variableLog.getOldValue());
    assertEquals(future, variableLog.getNewValue());
  }

  public void testDoubleUpdateLog() {
    contextInstance.setVariable("a", new Double(3.3));
    contextInstance.setVariable("a", new Double(4.4));

    VariableUpdateLog variableLog = (VariableUpdateLog)loggingInstance.getLogs(VariableUpdateLog.class).get(1);
    
    assertEquals(new Double(3.3), variableLog.getOldValue());
    assertEquals(new Double(4.4), variableLog.getNewValue());
  }

  public void testLongUpdateLog() {
    contextInstance.setVariable("a", new Integer(3));
    contextInstance.setVariable("a", new Integer(5));

    VariableUpdateLog variableLog = (VariableUpdateLog)loggingInstance.getLogs(VariableUpdateLog.class).get(1);

    assertEquals(new Long(3), variableLog.getOldValue());
    assertEquals(new Long(5), variableLog.getNewValue());
  }

  public void testStringUpdateLog() {
    contextInstance.setVariable("a", "pope");
    contextInstance.setVariable("a", "me");

    VariableUpdateLog variableLog = (VariableUpdateLog)loggingInstance.getLogs(VariableUpdateLog.class).get(1);

    assertEquals("pope", variableLog.getOldValue());
    assertEquals("me", variableLog.getNewValue());
  }

  public void testVariableDeleteLog() {
    contextInstance.setVariable("a", new Integer(3));
    contextInstance.deleteVariable("a");

    VariableDeleteLog deleteLog = (VariableDeleteLog)loggingInstance.getLogs(VariableDeleteLog.class).get(0);
    
    assertEquals("a", deleteLog.getVariableInstance().getName());
  }
}
