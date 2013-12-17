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

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Action;

public class ActionLogDbTest extends AbstractDbTestCase
{
  public void testActionLog()
  {
    Action action = new Action();
    session.save(action);
    
    ActionLog actionLog = new ActionLog(action);
    actionLog = (ActionLog)saveAndReload(actionLog);
    assertNotNull(actionLog.getAction());
    
    session.delete(actionLog);
    session.delete(action);
  }

  public void testActionExceptionLog()
  {
    RuntimeException rte = getRuntimeException();
    StringWriter stwr = new StringWriter();
    PrintWriter prwr = new PrintWriter(stwr);
    rte.printStackTrace(prwr);
    
    ActionLog actionLog = new ActionLog();
    actionLog.setException(rte);
    actionLog = (ActionLog)saveAndReload(actionLog);
    
    String rteStr = actionLog.getException();
    assertNotNull("Exception not null", rteStr);
    assertEquals("Exception string", stwr.toString().length(), rteStr.length());
    
    session.delete(actionLog);
  }

  private RuntimeException getRuntimeException()
  {
    RuntimeException rte = new RuntimeException("level 0");
    for (int level = 0; level < 10; level++)
    {
      rte = new RuntimeException("level " + level, rte);
    }
    return rte;
  }
}
