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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class VariableScopingTest extends AbstractJbpmTestCase {

  private static ProcessDefinition pd = createProcessDefinition();
  private ProcessInstance pi = new ProcessInstance(pd);
  private ContextInstance ci = pi.getContextInstance();
  private Token rootToken = pi.getRootToken();
  private Token tokenA = new Token(rootToken, "a");
  private Token tokenAB = new Token(tokenA, "ab");
  private Token tokenAC = new Token(tokenA, "ac");

  public static ProcessDefinition createProcessDefinition() {
    ProcessDefinition pd = new ProcessDefinition();
    pd.addDefinition(new ContextDefinition());
    return pd;
  }

  public void testScopeOverriding() {
    ci.createVariable("a", "one", rootToken);
    ci.createVariable("a", "two", tokenA);
    ci.createVariable("a", "three", tokenAB);

    assertEquals("one", ci.getVariable("a", rootToken));
    assertEquals("two", ci.getVariable("a", tokenA));
    assertEquals("three", ci.getVariable("a", tokenAB));
    assertEquals("two", ci.getVariable("a", tokenAC));
  }

  public void testScopePermeability() {
  	ci.createVariable("a", "one", rootToken);
  	ci.createVariable("b", "two", tokenA);
    assertEquals("one", ci.getVariable("a", rootToken));
    assertEquals("one", ci.getVariable("a", tokenA));
    assertNull(ci.getVariable("b", rootToken));
    assertEquals("two", ci.getVariable("b", tokenA));
  }
  
  public void testVariableOverloading() {
    ci.createVariable("a", "one", tokenAB);
    ci.createVariable("a", "two", tokenAC);

    assertEquals("one", ci.getVariable("a", tokenAB));
    assertEquals("two", ci.getVariable("a", tokenAC));
  }
  
  public void testRootTokenValue() {
    ci.setVariable("a", new Integer(3));
    assertNotNull(ci.getVariable("a"));
    assertEquals(Integer.class, ci.getVariable("a").getClass());
    assertEquals(new Integer(3), ci.getVariable("a"));
  }

  public void testRootTokenMap() {
    Map variables = new HashMap();
    variables.put("a", new Integer(3));
    variables.put("b", new Integer(4));
    ci.addVariables(variables);
    variables = ci.getVariables();
    assertEquals(new Integer(3), variables.get("a"));
    assertEquals(new Integer(4), variables.get("b"));
  }

  public void testChildTokenValueDefaultCreation() {
    // this variable is actually set on the root token
    ci.setVariable("a", new Integer(3), tokenA);
    assertEquals(new Integer(3), ci.getVariable("a", tokenA));
    assertEquals(new Integer(3), ci.getVariable("a", tokenAB));
    assertEquals(new Integer(3), ci.getVariable("a", tokenAC));
    assertEquals(new Integer(3), ci.getVariable("a", rootToken));
  }

  public void testChildTokenValueTokenCreate() {
    ci.createVariable("a", new Integer(3), tokenA);
    assertEquals(new Integer(3), ci.getVariable("a", tokenA));
    assertEquals(new Integer(3), ci.getVariable("a", tokenAB));
    assertEquals(new Integer(3), ci.getVariable("a", tokenAC));
    assertNull(ci.getVariable("a", rootToken));
  }

  public void testChildTokenLocalValue() {
    ci.createVariable("a", new Integer(3), tokenA);
    assertEquals(new Integer(3), ci.getLocalVariable("a", tokenA));
    assertNull(ci.getLocalVariable("a", rootToken));
    assertNull(ci.getLocalVariable("a", tokenAB));
    assertNull(ci.getLocalVariable("a", tokenAC));
  }
  
  public void testChildTokenMapDefaultCreation() {
    Map variables = new HashMap();
    variables.put("a", new Integer(3));
    variables.put("b", new Integer(4));
    ci.addVariables(variables, tokenA);

    Map rootTokenVariables = ci.getVariables(rootToken);
    Map tokenAVariables = ci.getVariables(tokenA);
    Map tokenABVariables = ci.getVariables(tokenAB);
    Map tokenACVariables = ci.getVariables(tokenAC);

    assertEquals(variables, rootTokenVariables);
    assertEquals(variables, tokenAVariables);
    assertEquals(variables, tokenABVariables);
    assertEquals(variables, tokenACVariables);
  }

}
