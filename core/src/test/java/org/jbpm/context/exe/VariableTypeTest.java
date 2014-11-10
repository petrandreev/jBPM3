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

import java.util.Date;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class VariableTypeTest extends AbstractJbpmTestCase {

  ContextInstance contextInstance;

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    contextInstance = processInstance.getContextInstance();
  }

  public void testString() {
    contextInstance.setVariable("a", new String("3"));
    assertEquals("3", contextInstance.getVariable("a"));
  }

  public void testBoolean() {
    contextInstance.setVariable("a", Boolean.TRUE);
    assertEquals(Boolean.TRUE, contextInstance.getVariable("a"));
  }

  public void testCharacter() {
    contextInstance.setVariable("a", new Character(' '));
    assertEquals(new Character(' '), contextInstance.getVariable("a"));
  }

  public void testFloat() {
    contextInstance.setVariable("a", new Float(3.3));
    assertEquals(new Float(3.3), contextInstance.getVariable("a"));
  }

  public void testDouble() {
    contextInstance.setVariable("a", new Double(3.3));
    assertEquals(new Double(3.3), contextInstance.getVariable("a"));
  }

  public void testCustomTypeSerializable() {
    contextInstance.setVariable("a", new MySerializableClass(4));
    assertEquals(new MySerializableClass(4), contextInstance.getVariable("a"));
  }

  public void testBasicTypeSerializable() {
    contextInstance.setVariable("a", new Character('c'));
    assertEquals(new Character('c'), contextInstance.getVariable("a"));
  }

  public void testLong() {
    contextInstance.setVariable("a", new Long(3));
    assertEquals(new Long(3), contextInstance.getVariable("a"));
  }

  public void testByte() {
    contextInstance.setVariable("a", new Byte("3"));
    assertEquals(new Byte("3"), contextInstance.getVariable("a"));
  }

  public void testShort() {
    contextInstance.setVariable("a", new Short("3"));
    assertEquals(new Short("3"), contextInstance.getVariable("a"));
  }

  public void testInteger() {
    contextInstance.setVariable("a", new Integer(3));
    assertEquals(new Integer(3), contextInstance.getVariable("a"));
  }

  public void testDate() {
    Date now = new Date();
    contextInstance.setVariable("a", now);
    assertEquals(now, contextInstance.getVariable("a"));
  }

  public void testNullUpdate() {
    contextInstance.setVariable("a", "blablabla");
    contextInstance.setVariable("a", null);
    assertNull(contextInstance.getVariable("a"));
  }

  public void testChangeType() {
    // this one does not use a converter
    contextInstance.setVariable("a", new String("text"));
    contextInstance.setVariable("a", new Integer(3));
  }

  public void testChangeType2() {
    // this one uses a converter
    contextInstance.setVariable("a", new Integer(3));
    contextInstance.setVariable("a", new String("text"));
  }

  public void testChangeTypeWithDeleteIsAllowed() {
    contextInstance.setVariable("a", new String("3"));
    contextInstance.deleteVariable("a");
    contextInstance.setVariable("a", new Integer(3));
  }

  public void testUnsupportedType() {
    Thread thread = new Thread();
    contextInstance.setVariable("a", thread);
    assertSame(thread, contextInstance.getVariable("a"));
  }

  public void testByteArray() {
    byte[] bytes = "bits".getBytes();
    contextInstance.setVariable("b", bytes);
  }

  public void testEmptyByteArray() {
    byte[] bytes = new byte[0];
    contextInstance.setVariable("b", bytes);
  }
}
