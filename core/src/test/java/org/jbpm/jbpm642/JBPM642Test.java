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
package org.jbpm.jbpm642;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 */
public class JBPM642Test extends AbstractJbpmTestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testSignalTokenInFork() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm642/processdefinition.xml");

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    Token rootToken = processInstance.getRootToken();
    assertEquals("fork", rootToken.getNode().getName());

    try {
      rootToken.signal();
      fail("expected locked exception");
    }
    catch (JbpmException e) {
      assert e.getMessage().indexOf("locked") != -1 : e;
    }

    rootToken.getChild("left").signal();
    rootToken.getChild("2").signal();
    assertEquals("middle", rootToken.getNode().getName());

    rootToken.signal();
    assert processInstance.hasEnded() : "expected " + processInstance + " to have ended";
  }
}
