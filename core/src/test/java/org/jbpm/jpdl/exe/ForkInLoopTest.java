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
package org.jbpm.jpdl.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class ForkInLoopTest extends AbstractJbpmTestCase {

  public void testCycle() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <transition to='f' />"
        + "  </state>"
        + "  <fork name='f'>"
        + "    <transition name='b' to='b' />"
        + "    <transition name='c' to='c' />"
        + "  </fork>"
        + "  <state name='b'>"
        + "    <transition to='j' />"
        + "  </state>"
        + "  <state name='c'>"
        + "    <transition to='j' />"
        + "  </state>"
        + "  <join name='j'>"
        + "    <transition to='d' />"
        + "  </join>"
        + "  <state name='d'>"
        + "    <transition to='a' />"
        + "  </state>"
        + "</process-definition>");

    ProcessInstance pi = (ProcessInstance) processDefinition.createInstance();

    for (int i = 0; i < 3; i++) {
      log.debug("starting round " + i);
      pi.signal();
      assertEquals("a", pi.getRootToken().getNode().getName());

      pi.signal();

      String extend = "";
      if (i >= 1) extend = Integer.toString(i + 1);

      assertEquals("b", pi.findToken("/b" + extend).getNode().getName());
      assertEquals("c", pi.findToken("/c" + extend).getNode().getName());

      log.debug("signalling '/b" + extend + "' in round " + i);
      pi.findToken("/b" + extend).signal();
      log.debug("signalling '/c" + extend + "' in round " + i);
      pi.findToken("/c" + extend).signal();

      assertEquals("d", pi.getRootToken().getNode().getName());
    }
  }

}