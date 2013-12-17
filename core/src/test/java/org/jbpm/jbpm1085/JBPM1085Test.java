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
package org.jbpm.jbpm1085;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Join of new process instance fails in Oracle.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1085">JBPM-1085</a>
 */
public class JBPM1085Test extends AbstractDbTestCase {

  public void testJoinPersistence() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state>"
      + "    <transition to='f' />"
      + "  </start-state>"
      + "  <fork name='f'>"
      + "    <transition name='a' to='a' />"
      + "    <transition name='b' to='b' />"
      + "  </fork>"
      + "  <state name='a'>"
      + "    <transition to='j' />"
      + "  </state>"
      + "  <state name='b'>"
      + "    <transition to='j' />"
      + "  </state>"
      + "  <join name='j'>"
      + "    <transition to='end' />"
      + "  </join>"
      + "  <end-state name='end'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    processInstance.findToken("/a").signal();

    processInstance = saveAndReload(processInstance);
    processInstance.findToken("/b").signal();

    assertEquals("end", processInstance.getRootToken().getNode().getName());
    assertEquals("j", processInstance.findToken("/a").getNode().getName());
    assertEquals("j", processInstance.findToken("/b").getNode().getName());
  }
}
