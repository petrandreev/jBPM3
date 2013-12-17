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
package org.jbpm.graph.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import bsh.ParseException;

public class ScriptTest extends AbstractJbpmTestCase {

  public void testActionScript() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <state name='a'>" +
      "    <event type='node-enter'>" +
      "      <script>" +
      "        <variable name='a' access='write' />" +
      "        <expression>" +
      "          a = b + c;" +
      "        </expression>" +
      "      </script>" +
      "    </event>" +
      "  </state>" +
      "</process-definition>"
    );
    processDefinition.addDefinition(new ContextDefinition());

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("b", new Integer(3));
    contextInstance.setVariable("c", new Integer(9));
    processInstance.signal();

    assertEquals(new Integer(12), contextInstance.getVariable("a"));
  }

  public void testNodeScript() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <node name='a'>" +
      "    <script>" +
      "      executionContext.leaveNode(\"c\");" +
      "    </script>" +
      "    <transition name='b' to='b' />" +
      "    <transition name='c' to='c' />" +
      "  </node>" +
      "  <state name='b' />" +
      "  <state name='c' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    assertSame(processDefinition.getNode("c"), processInstance.getRootToken().getNode());
  }

  public void testNullValues() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a'>" +
      "      <script>" +
      "        <expression>" +
      "        if (a==null) {" +
      "          b = null;" +
      "        }" +
      "        </expression>" +
      "        <variable name='a' access='write' />" +
      "        <variable name='b' access='read' />" +
      "      </script>" +
      "    </transition>" +
      "  </start-state>" +
      "  <state name='a' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("a", null);
    processInstance.signal();
    assertNull(processInstance.getContextInstance().getVariable("b"));
  }

  public void testScriptParsingException() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <state name='a'>" +
      "    <event type='node-enter'>" +
      "      <script>" +
      "        <variable name='a' access='write' />" +
      "        <expression>" +
      "          bebobalula" +
      "        </expression>" +
      "      </script>" +
      "    </event>" +
      "  </state>" +
      "</process-definition>"
    );
    processDefinition.addDefinition(new ContextDefinition());

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    try {
      processInstance.signal();
    } catch (DelegationException e) {
      assertEquals(ParseException.class, e.getCause().getClass());
    }
  }
  
  public static class Thrower {
    public void throwItInTheGroup() {
      throw new RuntimeException("i forgot my wedding aniversary");
    }
  }

  public void testScriptTargetError() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <state name='a'>" +
      "    <event type='node-enter'>" +
      "      <script>" +
      "        <variable name='a' access='write' />" +
      "        <expression>" +
      "          i.throwItInTheGroup()" +
      "        </expression>" +
      "      </script>" +
      "    </event>" +
      "  </state>" +
      "</process-definition>"
    );
    processDefinition.addDefinition(new ContextDefinition());

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("i", new Thrower());
    try {
      processInstance.signal();
    } catch (DelegationException e) {
      assertEquals(RuntimeException.class, e.getCause().getClass());
      assertEquals("i forgot my wedding aniversary", e.getCause().getMessage());
    }
  }

  public void testScriptEvaluation() throws Exception {
    Map inputMap = new HashMap();
    inputMap.put("a", new Integer(1));
    inputMap.put("b", new Integer(1));
    
    Set outputNames = new HashSet();
    outputNames.add("c");
    outputNames.add("d");

    Script script = new Script();
    script.setExpression(
      "c = a + b;" +
      "d = a + b + c"
    );
    Map outputMap = script.eval(inputMap, outputNames);
    
    assertEquals(2, outputMap.size());
    assertEquals(new Integer(2), outputMap.get("c"));
    assertEquals(new Integer(4), outputMap.get("d"));
  }
  
  public void testExecute() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    ExecutionContext executionContext = new ExecutionContext(token);
    ContextInstance contextInstance = executionContext.getContextInstance();
    contextInstance.setVariable("a", new Integer(1));
    contextInstance.setVariable("b", new Integer(1));
    
    Script script = new Script();
    script.addVariableAccess(new VariableAccess("a", "read,write", null));
                                                // b is READ-ONLY ! 
    script.addVariableAccess(new VariableAccess("b", "read", null));
    script.addVariableAccess(new VariableAccess("c", "write", null));
    script.setExpression(
      "if (a!=1) throw new RuntimeException(\"a is not 1\");" +
      "if (b!=1) throw new RuntimeException(\"b is not 1\");" +
      "a = 2;" +
      "b = 3;" +
      "c = 4;"
    );
    script.execute(executionContext);
    
    assertEquals(new Integer(2), contextInstance.getVariable("a"));
    // b was READ-ONLY ! 
    assertEquals(new Integer(1), contextInstance.getVariable("b"));
    assertEquals(new Integer(4), contextInstance.getVariable("c"));
  }

  
  public void testCreateInputMapWithoutContext() {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    
    Script script = new Script();
    ExecutionContext executionContext = new ExecutionContext(token);
    Map inputMap = script.createInputMap(executionContext);
    assertSame( executionContext, inputMap.get("executionContext"));
    assertSame( token, inputMap.get("token"));
    assertNull( inputMap.get("node"));
    assertNull( inputMap.get("task"));
    assertNull( inputMap.get("taskInstance"));
  }

  public void testCreateInputMapWithoutVariableAccesses() {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", new Integer(1) );
    contextInstance.setVariable("b", new Integer(1) );
    contextInstance.setVariable("c", new Integer(1) );
    
    Script script = new Script();
    Map inputMap = script.createInputMap(new ExecutionContext(token));
    assertEquals(new Integer(1), inputMap.get("a"));
    assertEquals(new Integer(1), inputMap.get("b"));
    assertEquals(new Integer(1), inputMap.get("c"));
  }

  public void testCreateInputMapWithOnlyWriteVariableAccesses() {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", new Integer(1) );
    contextInstance.setVariable("b", new Integer(1) );
    contextInstance.setVariable("c", new Integer(1) );
    
    Script script = new Script();
    script.addVariableAccess(new VariableAccess("a", "write", null));
    script.addVariableAccess(new VariableAccess("b", "write-required", null));
    Map inputMap = script.createInputMap(new ExecutionContext(token));
    assertEquals(new Integer(1), inputMap.get("a"));
    assertEquals(new Integer(1), inputMap.get("b"));
    assertEquals(new Integer(1), inputMap.get("c"));
  }

  public void testCreateInputMapWithOnlyReadVariableAccesses() {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", new Integer(1) );
    contextInstance.setVariable("b", new Integer(1) );
    contextInstance.setVariable("c", new Integer(1) );
    
    Script script = new Script();
    script.addVariableAccess(new VariableAccess("a", "read", null));
    Map inputMap = script.createInputMap(new ExecutionContext(token));
    assertEquals(new Integer(1), inputMap.get("a"));
    assertFalse(inputMap.containsKey("b"));
    assertFalse(inputMap.containsKey("c"));
  }

  public void testMappedNameTest() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", new Integer(1) );
    
    Script script = new Script();
    script.setExpression("AAA++;");
    script.addVariableAccess(new VariableAccess("a", "read-write", "AAA"));
    script.execute(new ExecutionContext(token));

    assertEquals(new Integer(2), contextInstance.getVariable("a"));
  }
}
