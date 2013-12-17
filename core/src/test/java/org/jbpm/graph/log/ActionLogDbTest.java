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
package org.jbpm.graph.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hibernate.type.Type;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.hibernate.LimitedStringType;
import org.jbpm.db.hibernate.LimitedTextType;
import org.jbpm.graph.def.Action;

public class ActionLogDbTest extends AbstractDbTestCase {

  public void testActionLog() {
    Action action = new Action();
    session.save(action);

    ActionLog actionLog = new ActionLog(action);
    actionLog = (ActionLog) saveAndReload(actionLog);

    try {
      assertNotNull(actionLog.getAction());
    }
    finally {
      session.delete(actionLog);
      session.delete(action);
    }
  }

  public void testActionExceptionLog() {
    RuntimeException exception = createExceptionChain(null, 0);

    ActionLog actionLog = new ActionLog();
    actionLog.setException(exception);
    actionLog = (ActionLog) saveAndReload(actionLog);

    try {
      String fullStackTrace = getStackTrace(exception);
      String savedStackTrace = actionLog.getException();

      int expectedLength = Math.min(fullStackTrace.length(), getStackTraceLimit());
      assertEquals(expectedLength, savedStackTrace.length());

      assertEquals(fullStackTrace.substring(0, expectedLength), savedStackTrace);
    }
    finally {
      session.delete(actionLog);
    }
  }

  private String getStackTrace(RuntimeException exception) {
    StringWriter exceptionWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(exceptionWriter));
    return exceptionWriter.toString();
  }

  private int getStackTraceLimit() {
    Type type = session.getSessionFactory()
      .getClassMetadata(ActionLog.class)
      .getPropertyType("exception");
    if (type instanceof LimitedStringType) {
      LimitedStringType limitedType = (LimitedStringType) type;
      return limitedType.getLimit();
    }
    if (type instanceof LimitedTextType) {
      LimitedTextType limitedType = (LimitedTextType) type;
      return limitedType.getLimit();
    }
    return Integer.MAX_VALUE;
  }

  private RuntimeException createExceptionChain(RuntimeException cause, int level) {
    return level == 100 ? cause : createExceptionChain(level % 10 == 0 ? new RuntimeException(
      "level " + level, cause) : cause, level + 1);
  }
}
