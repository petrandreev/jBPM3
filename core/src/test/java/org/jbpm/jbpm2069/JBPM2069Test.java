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
package org.jbpm.jbpm2069;

import java.util.BitSet;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * IntegerToLongConverter not consistent when dealing with null-values
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-2069">JBPM-2069</a>
 * @author Alejandro Guizar
 */
public class JBPM2069Test extends AbstractJbpmTestCase {

  ContextInstance contextInstance;

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = processDefinition.createProcessInstance();
    contextInstance = processInstance.getContextInstance();
  }

  public void testIntegerVariable() {
    contextInstance.setVariable("i", new Integer(Integer.MAX_VALUE));
    contextInstance.setVariable("i", null);

    assertNull(contextInstance.getVariable("i"));
  }

  public void testShortVariable() {
    contextInstance.setVariable("s", new Short(Short.MAX_VALUE));
    contextInstance.setVariable("s", null);

    assertNull(contextInstance.getVariable("s"));
  }

  public void testByteVariable() {
    contextInstance.setVariable("b", new Short(Byte.MAX_VALUE));
    contextInstance.setVariable("b", null);

    assertNull(contextInstance.getVariable("b"));
  }

  public void testFloatVariable() {
    contextInstance.setVariable("f", new Float(Float.MAX_VALUE));
    contextInstance.setVariable("f", null);

    assertNull(contextInstance.getVariable("f"));
  }

  public void testBooleanVariable() {
    contextInstance.setVariable("z", Boolean.TRUE);
    contextInstance.setVariable("z", null);

    assertNull(contextInstance.getVariable("z"));
  }

  public void testCharacterVariable() {
    contextInstance.setVariable("c", new Character(Character.MAX_VALUE));
    contextInstance.setVariable("c", null);

    assertNull(contextInstance.getVariable("c"));
  }

  public void testSerializableVariable() {
    contextInstance.setVariable("l", new BitSet());
    contextInstance.setVariable("l", null);

    assertNull(contextInstance.getVariable("l"));
  }
}
