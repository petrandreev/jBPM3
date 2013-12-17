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
package org.jbpm.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.util.CollectionUtil;

public class LoggingSession {

  final Session session;
  final JbpmSession jbpmSession;

  public LoggingSession(JbpmSession jbpmSession) {
    this.session = jbpmSession.getSession();
    this.jbpmSession = jbpmSession;
  }

  public LoggingSession(Session session) {
    this.session = session;
    this.jbpmSession = null;
  }

  /**
   * returns a map of {@linkplain Token tokens} to {@linkplain List lists}. The lists contain the
   * ordered logs for the given token. The lists are retrieved with {@link #findLogsByToken(long)}.
   */
  public Map<Token, List<ProcessLog>> findLogsByProcessInstance(long processInstanceId) {
    return findLogsByProcessInstance((ProcessInstance) session.load(ProcessInstance.class, processInstanceId));
  }

  /**
   * returns a map of {@linkplain Token tokens} to {@linkplain List lists}. The lists contain the
   * ordered logs for the given token. The lists are retrieved with {@link #findLogsByToken(long)}.
   */
  public Map<Token, List<ProcessLog>> findLogsByProcessInstance(ProcessInstance processInstance) {
    Map<Token, List<ProcessLog>> tokenLogs = new HashMap<Token, List<ProcessLog>>();
    try {
      collectTokenLogs(tokenLogs, processInstance.getRootToken());
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get logs for " + processInstance, e);
    }
    return tokenLogs;
  }

  private void collectTokenLogs(Map<Token, List<ProcessLog>> tokenLogs, Token token) {
    tokenLogs.put(token, findLogsByToken(token));
    Map<String, Token> children = token.getChildren();
    if ((children != null) && (!children.isEmpty())) {
      for (Token child : children.values()) {
        collectTokenLogs(tokenLogs, child);
      }
    }
  }

  /**
   * collects the logs for a given token, ordered by creation time.
   */
  public List<ProcessLog> findLogsByToken(long tokenId) {
    return findLogsByToken((Token) session.load(Token.class, tokenId));
  }

  /**
   * collects the logs for a given token, ordered by creation time.
   */
  public List<ProcessLog> findLogsByToken(Token token) {
    try {
      Query query = session.getNamedQuery("LoggingSession.findLogsByToken");
      query.setEntity("token", token);
      return CollectionUtil.checkList(query.list(), ProcessLog.class);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get logs for " + token, e);
    }
  }

  /**
   * saves the given process log to the database.
   */
  public void saveProcessLog(ProcessLog processLog) {
    try {
      session.save(processLog);
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't save process log '" + processLog + "'", e);
    }
  }

  /**
   * load the process log for a given id.
   */
  public ProcessLog loadProcessLog(long processLogId) {
    ProcessLog processLog = null;
    try {
      processLog = (ProcessLog) session.load(ProcessLog.class, new Long(processLogId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't load process log '" + processLogId + "'", e);
    }
    return processLog;
  }

  /**
   * get the process log for a given id.
   */
  public ProcessLog getProcessLog(long processLogId) {
    ProcessLog processLog = null;
    try {
      processLog = (ProcessLog) session.get(ProcessLog.class, new Long(processLogId));
    }
    catch (Exception e) {
      handle(e);
      throw new JbpmException("couldn't get process log '" + processLogId + "'", e);
    }
    return processLog;
  }

  private void handle(Exception exception) {
    log.error(exception);
    if (jbpmSession != null)
      jbpmSession.handleException();
  }

  private static final Log log = LogFactory.getLog(LoggingSession.class);
}
