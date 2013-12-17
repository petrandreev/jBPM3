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
package org.jbpm.logging.log;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.exe.Token;

public class ProcessLogDbTest extends AbstractDbTestCase {

  public void testMessageLogMessage() {
    MessageLog messageLog = new MessageLog("piece of cake");
    messageLog = (MessageLog) saveAndReload(messageLog);
    assertEquals("piece of cake", messageLog.getMessage());
    session.delete(messageLog);
  }

  public void testProcessLogDate() {
    // discard milliseconds as some databases have second precision only
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.MILLISECOND, 0);
    Date now = calendar.getTime();

    ProcessLog processLog = new MessageLog();
    processLog.setDate(now);
    processLog = saveAndReload(processLog);
    assertEquals(now, processLog.getDate());

    session.delete(processLog);
  }

  public void testProcessLogToken() {
    Token token = new Token();
    session.save(token);
    ProcessLog processLog = new MessageLog();
    processLog.setToken(token);
    processLog = saveAndReload(processLog);
    assertNotNull(processLog.getToken());

    session.delete(processLog);
    session.delete(token);
  }

  public void testParentChildRelation() {
    CompositeLog compositeLog = new CompositeLog();
    ProcessLog procLog = new MessageLog("one");
    compositeLog.addChild(procLog);
    session.save(procLog);

    procLog = new MessageLog("two");
    compositeLog.addChild(procLog);
    session.save(procLog);

    procLog = new MessageLog("three");
    compositeLog.addChild(procLog);
    session.save(procLog);

    compositeLog = (CompositeLog) saveAndReload(compositeLog);
    assertEquals(3, compositeLog.getChildren().size());

    for (Iterator iter = compositeLog.getChildren().iterator(); iter.hasNext();) {
      ProcessLog childLog = (ProcessLog) iter.next();
      assertSame(compositeLog, childLog.getParent());
    }

    session.delete(compositeLog);
  }

}
