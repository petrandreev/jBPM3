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
package org.jbpm.svc.save;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.LoggingService;
import org.jbpm.logging.log.ProcessLog;

public class SaveLogsOperation implements SaveOperation {

  private static final long serialVersionUID = 1L;

  public void save(ProcessInstance processInstance, JbpmContext jbpmContext) {
    LoggingService loggingService = jbpmContext.getServices().getLoggingService();
    if (loggingService == null) return;

    List logs = processInstance.getLoggingInstance().getLogs();
    if (logs.isEmpty()) return;

    if (log.isDebugEnabled()) log.debug("flushing " + logs.size() + " logs");
    for (Iterator iter = logs.iterator(); iter.hasNext();) {
      ProcessLog processLog = (ProcessLog) iter.next();
      Token token = processLog.getToken();
      if (token != null) {
        int index = token.nextLogIndex();
        processLog.setIndex(index);
      }
      loggingService.log(processLog);
    }
  }

  private static final Log log = LogFactory.getLog(SaveLogsOperation.class);
}
