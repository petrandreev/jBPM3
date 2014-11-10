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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.log.CompositeLog;
import org.jbpm.logging.log.MessageLog;
import org.jbpm.logging.log.ProcessLog;

public class CompositeLogTest extends AbstractJbpmTestCase {

  public void testCompositeLogs() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    ProcessInstance pi = new ProcessInstance(processDefinition);
    Token root = pi.getRootToken();
    LoggingInstance li = pi.getLoggingInstance();

    ProcessLog one = new MessageLog(null);
    li.addLog(one);

    assertNull(one.getParent());
    assertSame(one, li.getLogs().get(1));
    assertEquals(2, li.getLogs().size());
    
    assertEquals(0, li.getCompositeLogStack().size());

    CompositeLog two = new CompositeLog();
    two.setToken(root);
    li.startCompositeLog(two);

    assertNull(two.getParent());
    assertSame(two, li.getLogs().get(2));
    assertEquals(3, li.getLogs().size());
    assertEquals(1, li.getCompositeLogStack().size());

    ProcessLog three = new MessageLog(null);
    li.addLog(three);

    assertSame(two, three.getParent());
    assertSame(three, li.getLogs().get(3));
    assertEquals(4, li.getLogs().size());
    assertEquals(1, li.getCompositeLogStack().size());

    CompositeLog four = new CompositeLog();
    four.setToken(root);
    li.startCompositeLog(four);

    assertSame(two, four.getParent());
    assertSame(four, li.getLogs().get(4));
    assertEquals(5, li.getLogs().size());
    assertEquals(2, li.getCompositeLogStack().size());

    ProcessLog five = new MessageLog(null);
    li.addLog(five);

    assertSame(four, five.getParent());
    assertSame(two, five.getParent().getParent());
    assertNull(five.getParent().getParent().getParent());
    assertSame(five, li.getLogs().get(5));
    assertEquals(6, li.getLogs().size());
    assertEquals(2, li.getCompositeLogStack().size());

    li.endCompositeLog();

    assertEquals(1, li.getCompositeLogStack().size());

    ProcessLog six = new MessageLog(null);
    li.addLog(six);

    assertSame(two, six.getParent());
    assertNull(six.getParent().getParent());
    assertSame(six, li.getLogs().get(6));
    assertEquals(7, li.getLogs().size());
    assertEquals(1, li.getCompositeLogStack().size());

    li.endCompositeLog();

    assertEquals(0, li.getCompositeLogStack().size());
  }
}
