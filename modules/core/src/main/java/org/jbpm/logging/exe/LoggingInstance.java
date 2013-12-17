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
public class LoggingInstance extends ModuleInstance {

  private static final long serialVersionUID = 1L;

  List<ProcessLog> logs = new ArrayList<ProcessLog>();
  transient List<CompositeLog> compositeLogStack = new ArrayList<CompositeLog>();

  public LoggingInstance() {
  }

  public void startCompositeLog(CompositeLog compositeLog) {
    addLog(compositeLog);
    compositeLogStack.add(compositeLog);
  }

  public void endCompositeLog() {
    compositeLogStack.remove(compositeLogStack.size() - 1);
  }

  public void addLog(ProcessLog processLog) {
    if (!compositeLogStack.isEmpty()) {
      CompositeLog currentCompositeLog = compositeLogStack.get(compositeLogStack.size() - 1);
      processLog.setParent(currentCompositeLog);
      currentCompositeLog.addChild(processLog);
    }
    processLog.setDate(Clock.getCurrentTime());

    logs.add(processLog);
  }

  public List<ProcessLog> getLogs() {
    return logs;
  }

  /**
   * get logs, filtered by log type.
   */
  public <L extends ProcessLog> List<L> getLogs(Class<L> logType) {
    return getLogs(logs, logType);
  }

  public static <L extends ProcessLog> List<L> getLogs(Collection<ProcessLog> logs, Class<L> logType) {
    List<L> filteredLogs = new ArrayList<L>();
    if (logs != null) {
      for (ProcessLog log : logs) {
        if (logType.isInstance(log)) {
          filteredLogs.add(logType.cast(log));
        }
      }
    }
    return filteredLogs;
  }

  List<CompositeLog> getCompositeLogStack() {
    return compositeLogStack;
  }

  public void logLogs() {
    for (ProcessLog processLog : logs) {
      if (processLog.getParent() == null) {
        logLog("", processLog);
      }
    }
  }

  void logLog(String indentation, ProcessLog processLog) {
    boolean isComposite = processLog instanceof CompositeLog;
    log.debug(indentation
        + (isComposite ? "+ [" : "  [")
        + processLog.getIndex()
        + "] "
        + processLog
        + " on "
        + processLog.getToken());
    if (isComposite) {
      CompositeLog compositeLog = (CompositeLog) processLog;
      List<ProcessLog> children = compositeLog.getChildren();
      if (children != null) {
        for (ProcessLog childLog : children) {
          logLog(indentation + "  ", childLog);
        }
      }
    }
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    compositeLogStack = new ArrayList<CompositeLog>();
  }

  private static final Log log = LogFactory.getLog(LoggingInstance.class);
}
