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
package org.jbpm.context.exe;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.context.def.ContextDefinition;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class ContextInstanceDbTest extends AbstractDbTestCase {

  public void testContextInstanceTokenVariableMaps() {
    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    processDefinition.addDefinition(new ContextDefinition());
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token rootToken = processInstance.getRootToken();
    Token childToken = new Token(rootToken, "shipping");

    ContextInstance contextInstance = processInstance.getContextInstance();
    Map tokenVariableMaps = new HashMap();
    tokenVariableMaps.put(rootToken, new TokenVariableMap(rootToken, contextInstance));
    tokenVariableMaps.put(childToken, new TokenVariableMap(childToken, contextInstance));
    contextInstance.tokenVariableMaps = tokenVariableMaps;

    processInstance = saveAndReload(processInstance);
    rootToken = processInstance.getRootToken();
    childToken = rootToken.getChild("shipping");
    contextInstance = processInstance.getContextInstance();
    tokenVariableMaps = contextInstance.tokenVariableMaps;

    assertEquals(2, tokenVariableMaps.size());
    assertTrue(tokenVariableMaps.containsKey(rootToken));
    assertTrue(tokenVariableMaps.containsKey(childToken));
    TokenVariableMap tokenVariableMap = (TokenVariableMap) tokenVariableMaps.get(rootToken);
    assertNotNull(tokenVariableMap);
    tokenVariableMap = (TokenVariableMap) tokenVariableMaps.get(childToken);
    assertNotNull(tokenVariableMap);
  }

  public void testVariableUpdate() {
    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    processDefinition.addDefinition(new ContextDefinition());
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", "one");

    processInstance = saveAndReload(processInstance);

    contextInstance = processInstance.getContextInstance();
    assertEquals("one", contextInstance.getVariable("a"));
    contextInstance.setVariable("a", "two");

    processInstance = saveAndReload(processInstance);

    contextInstance = processInstance.getContextInstance();
    assertEquals("two", contextInstance.getVariable("a"));
  }
}
