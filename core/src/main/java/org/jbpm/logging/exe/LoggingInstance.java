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
package org.jbpm.logging.exe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.logging.log.CompositeLog;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.util.Clock;

/**
 * non persisted class that collects {@link org.jbpm.logging.log.ProcessLog}s during process
 * execution. When the process instance gets saved, the process logs will be saved by the
 * {@link org.jbpm.db.LoggingSession}.
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class LoggingInstance extends ModuleInstance {

  private static final long serialVersionUID = 1L;

  private List logs = new ArrayList();
  private transient List compositeLogStack = new ArrayList();

  public LoggingInstance() {
  }

  public synchronized void startCompositeLog(CompositeLog compositeLog) {
    addLog(compositeLog);
    compositeLogStack.add(compositeLog);
  }

  public synchronized void endCompositeLog() {
    compositeLogStack.remove(compositeLogStack.size() - 1);
  }

  public synchronized void addLog(ProcessLog processLog) {
    if (!compositeLogStack.isEmpty()) {
      CompositeLog compositeLog = (CompositeLog) compositeLogStack.get(compositeLogStack.size() - 1);
      compositeLog.addChild(processLog);
      processLog.setParent(compositeLog);
    }
    processLog.setDate(Clock.getCurrentTime());
    logs.add(processLog);
  }

  /**
   * If you modify the returned list in any way, you run the risk 
   * of causing exceptions in a concurrent situation. 
   * 
   * @return The list of logs
   */
  public List getLogs() {
    return logs;
  }

  /**
   * get logs, filtered by log type.
   */
  public List getLogs(Class filterClass) {
    return getLogs(getLogs(), filterClass);
  }

  public static List getLogs(Collection logs, Class filterClass) {
    List filteredLogs = new ArrayList();
    if (logs != null) {
      for (Iterator iter = logs.iterator(); iter.hasNext();) {
        Object log = iter.next();
        if (filterClass.isAssignableFrom(log.getClass())) filteredLogs.add(log);
      }
    }
    return filteredLogs;
  }

  List getCompositeLogStack() {
    return Collections.unmodifiableList(compositeLogStack);
  }

  public void logLogs() {
    for (Iterator iter = logs.iterator(); iter.hasNext();) {
      ProcessLog processLog = (ProcessLog) iter.next();
      if (processLog.getParent() == null) logLog("+-", processLog);
    }
  }

  private void logLog(String indentation, ProcessLog processLog) {
    log.debug(processLog.getToken() + "[" + processLog.getIndex() + "] " + processLog + " on "
      + processLog.getToken());
    if (processLog instanceof CompositeLog) {
      CompositeLog compositeLog = (CompositeLog) processLog;
      if (compositeLog.getChildren() != null) {
        for (Iterator iter = compositeLog.getChildren().iterator(); iter.hasNext();) {
          logLog("| " + indentation, (ProcessLog) iter.next());
        }
      }
    }
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    compositeLogStack = new ArrayList();
  }

  private static final Log log = LogFactory.getLog(LoggingInstance.class);
}
