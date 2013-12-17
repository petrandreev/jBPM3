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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jbpm.bytes.ByteArray;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.exe.LoggingInstance;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.util.DateDbTestUtil;

public class VariableUpdateDbTest extends AbstractDbTestCase {
  private ProcessDefinition processDefinition;
  private ProcessInstance processInstance;
  private ContextInstance contextInstance;

  protected void setUp() throws Exception {
    super.setUp();
    processDefinition = new ProcessDefinition();
    processDefinition.addDefinition(new ContextDefinition());
    graphSession.saveProcessDefinition(processDefinition);

    processInstance = new ProcessInstance(processDefinition);
    contextInstance = processInstance.getContextInstance();
  }

  @Override
  protected void tearDown() throws Exception {
    jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    super.tearDown();
  }

  public void testVariableCreateLogs() {
    contextInstance.setVariable("a", new Integer(3));

    jbpmContext.save(processInstance);
    newTransaction();
    List<ProcessLog> logs = loggingSession.findLogsByToken(processInstance.getRootToken().getId());
    VariableCreateLog createLog = LoggingInstance.getLogs(logs, VariableCreateLog.class).get(0);

    assertEquals("a", createLog.getVariableInstance().getName());
  }

  public void testByteArrayUpdateLog() {
    contextInstance.setVariable("a", "first value".getBytes());
    jbpmContext.save(processInstance);
    contextInstance.setVariable("a", "second value".getBytes());

    VariableUpdateLog variableLog = saveAndReloadUpdateLog(processInstance);

    assertTrue(Arrays.equals("first value".getBytes(),
        ((ByteArray) variableLog.getOldValue()).getBytes()));
    assertTrue(Arrays.equals("second value".getBytes(),
        ((ByteArray) variableLog.getNewValue()).getBytes()));
  }

  public void testDateUpdateLog() {
    final Date now = Calendar.getInstance().getTime();
    final Date future = new Date(now.getTime() + 5);
    contextInstance.setVariable("a", now);
    jbpmContext.save(processInstance);
    contextInstance.setVariable("a", future);

    VariableUpdateLog variableLog = saveAndReloadUpdateLog(processInstance);

    DateDbTestUtil dbUtilInst = DateDbTestUtil.getInstance();
    assertEquals(dbUtilInst.convertDateToSeconds(now),
        dbUtilInst.convertDateToSeconds((Date) variableLog.getOldValue()));
    assertEquals(dbUtilInst.convertDateToSeconds(future),
        dbUtilInst.convertDateToSeconds((Date) variableLog.getNewValue()));
  }

  public void testDoubleUpdateLog() {
    contextInstance.setVariable("a", new Double(3.3));
    jbpmContext.save(processInstance);
    contextInstance.setVariable("a", new Double(4.4));

    VariableUpdateLog variableLog = saveAndReloadUpdateLog(processInstance);

    assertEquals(new Double(3.3), variableLog.getOldValue());
    assertEquals(new Double(4.4), variableLog.getNewValue());
  }

  public void testLongUpdateLog() {
    contextInstance.setVariable("a", new Integer(3));
    jbpmContext.save(processInstance);
    contextInstance.setVariable("a", new Integer(5));

    VariableUpdateLog variableLog = saveAndReloadUpdateLog(processInstance);

    assertEquals(new Long(3), variableLog.getOldValue());
    assertEquals(new Long(5), variableLog.getNewValue());
  }

  public void testStringUpdateLog() {
    contextInstance.setVariable("a", "pope");
    jbpmContext.save(processInstance);
    contextInstance.setVariable("a", "me");

    VariableUpdateLog variableLog = saveAndReloadUpdateLog(processInstance);

    assertEquals("pope", variableLog.getOldValue());
    assertEquals("me", variableLog.getNewValue());
  }

  public void testVariableDeleteLog() {
    contextInstance.setVariable("a", new Integer(3));
    jbpmContext.save(processInstance);
    contextInstance.deleteVariable("a");

    jbpmContext.save(processInstance);
    newTransaction();
    List<ProcessLog> logs = loggingSession.findLogsByToken(processInstance.getRootToken().getId());
    VariableDeleteLog deleteLog = LoggingInstance.getLogs(logs, VariableDeleteLog.class).get(0);

    assertEquals("a", deleteLog.getVariableInstance().getName());
  }

  private VariableUpdateLog saveAndReloadUpdateLog(ProcessInstance processInstance) {
    jbpmContext.save(processInstance);
    newTransaction();
    List<ProcessLog> logs = loggingSession.findLogsByToken(processInstance.getRootToken().getId());
    return LoggingInstance.getLogs(logs, VariableUpdateLog.class).get(1);
  }
}
