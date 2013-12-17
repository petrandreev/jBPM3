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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.persistence.JbpmPersistenceException;

public class LoggingSession {

  final Session session;
  /** @deprecated */
  final JbpmSession jbpmSession;

  /** @deprecated use {@link #LoggingSession(Session)} instead */
  public LoggingSession(JbpmSession jbpmSession) {
    this.session = jbpmSession.getSession();
    this.jbpmSession = jbpmSession;
  }

  public LoggingSession(Session session) {
    this.session = session;
    this.jbpmSession = null;
  }

  /**
   * returns a map that maps {@link Token}s to {@link List}s. The lists contain the ordered logs
   * for the given token. The lists are retrieved with {@link #findLogsByToken(long)}.
   */
  public Map findLogsByProcessInstance(long processInstanceId) {
    Map tokenLogs = new HashMap();
    try {
      ProcessInstance processInstance = (ProcessInstance) session.load(ProcessInstance.class, new Long(
        processInstanceId));
      collectTokenLogs(tokenLogs, processInstance.getRootToken());
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("couldn't get logs for process instance "
        + processInstanceId, e);
    }
    return tokenLogs;
  }

  private void collectTokenLogs(Map tokenLogs, Token token) {
    tokenLogs.put(token, findLogsByToken(token.getId()));
    Map children = token.getChildren();
    if (children != null) {
      for (Iterator i = children.values().iterator(); i.hasNext();) {
        Token child = (Token) i.next();
        collectTokenLogs(tokenLogs, child);
      }
    }
  }

  /**
   * collects the logs for a given token, ordered by creation time.
   */
  public List findLogsByToken(long tokenId) {
    try {
      Token token = (Token) session.load(Token.class, new Long(tokenId));
      return session.getNamedQuery("LoggingSession.findLogsByToken")
        .setEntity("token", token)
        .list();
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("couldn't get logs for token '" + tokenId + "'", e);
    }
  }

  /**
   * saves the given process log to the database.
   */
  public void saveProcessLog(ProcessLog processLog) {
    try {
      session.save(processLog);
    }
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("couldn't save process log '" + processLog + "'", e);
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
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("couldn't load process log '" + processLogId + "'", e);
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
    catch (HibernateException e) {
      handle(e);
      throw new JbpmPersistenceException("couldn't get process log '" + processLogId + "'", e);
    }
    return processLog;
  }

  private void handle(HibernateException exception) {
    // exception will be rethrown, no need to log here
    if (jbpmSession != null) jbpmSession.handleException();
  }
}
