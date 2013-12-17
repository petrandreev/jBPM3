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

import javax.naming.directory.BasicAttribute;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.exe.converter.BooleanToStringConverter;
import org.jbpm.context.exe.converter.ByteToLongConverter;
import org.jbpm.context.exe.converter.CharacterToStringConverter;
import org.jbpm.context.exe.converter.FloatToDoubleConverter;
import org.jbpm.context.exe.converter.IntegerToLongConverter;
import org.jbpm.context.exe.converter.SerializableToByteArrayConverter;
import org.jbpm.context.exe.converter.ShortToLongConverter;
import org.jbpm.context.exe.variableinstance.ByteArrayInstance;
import org.jbpm.context.exe.variableinstance.DateInstance;
import org.jbpm.context.exe.variableinstance.DoubleInstance;
import org.jbpm.context.exe.variableinstance.LongInstance;
import org.jbpm.context.exe.variableinstance.StringInstance;

public class JbpmTypesTest extends AbstractJbpmTestCase {

  public void testString() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance("hello");
    assertEquals(StringInstance.class, variableInstance.getClass());
    assertNull(variableInstance.converter);
  }

  public void testDate() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Date());
    assertEquals(DateInstance.class, variableInstance.getClass());
    assertNull(variableInstance.converter);
  }

  public void testBoolean() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(Boolean.TRUE);
    assertEquals(StringInstance.class, variableInstance.getClass());
    assertEquals(BooleanToStringConverter.class, variableInstance.converter.getClass());
  }

  public void testCharacter() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Character(' '));
    assertEquals(StringInstance.class, variableInstance.getClass());
    assertEquals(CharacterToStringConverter.class, variableInstance.converter.getClass());
  }

  public void testFloat() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Float(3.3));
    assertEquals(DoubleInstance.class, variableInstance.getClass());
    assertEquals(FloatToDoubleConverter.class, variableInstance.converter.getClass());
  }

  public void testDouble() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Double(3.3));
    assertEquals(DoubleInstance.class, variableInstance.getClass());
    assertNull(variableInstance.converter);
  }

  public void testSerializable() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new BasicAttribute("i am serializable"));
    assertEquals(ByteArrayInstance.class, variableInstance.getClass());
    assertEquals(SerializableToByteArrayConverter.class, variableInstance.converter.getClass());
  }

  public void testLong() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Long(3));
    assertEquals(LongInstance.class, variableInstance.getClass());
    assertNull(variableInstance.converter);
  }

  public void testByte() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Byte("3"));
    assertEquals(LongInstance.class, variableInstance.getClass());
    assertEquals(ByteToLongConverter.class, variableInstance.converter.getClass());
  }

  public void testShort() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Short("3"));
    assertEquals(LongInstance.class, variableInstance.getClass());
    assertEquals(ShortToLongConverter.class, variableInstance.converter.getClass());
  }

  public void testInteger() {
    VariableInstance variableInstance = VariableInstance.createVariableInstance(new Integer(3));
    assertEquals(LongInstance.class, variableInstance.getClass());
    assertEquals(IntegerToLongConverter.class, variableInstance.converter.getClass());
  }
}
