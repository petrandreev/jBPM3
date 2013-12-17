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
package org.jbpm.bytes;

import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;

public class ByteArrayTest extends AbstractJbpmTestCase {

  public void testByteChoppingBlockCount() {
    int blockSize = JbpmConfiguration.Configs.getInt("jbpm.byte.block.size");
    for (int arrayLength = 125; arrayLength <= 1000; arrayLength *= 2) {
      ByteArray byteArray = new ByteArray(new byte[arrayLength]);
      int blockCount = arrayLength / blockSize;
      if (arrayLength % blockSize > 0) blockCount++;
      List byteBlocks = byteArray.byteBlocks;
      assertEquals(blockCount, byteBlocks.size());
    }
  }

  public void testReassembling() {
    for (int arrayLength = 125; arrayLength <= 1000; arrayLength *= 2) {
      ByteArray byteArray = new ByteArray(new byte[arrayLength]);      
      assertEquals(arrayLength, byteArray.getBytes().length);
    }
  }

  public void testEquals() {
    ByteArray left =  new ByteArray("the same bytes".getBytes());
    ByteArray right = new ByteArray("the same bytes".getBytes());
    assertTrue(left.equals(right));
    assertTrue(right.equals(left));
  }

  public void testNotEquals() {
    ByteArray left = new ByteArray("these bytes".getBytes());
    ByteArray right = new ByteArray("are not equal to these bytes".getBytes());
    assertFalse(left.equals(right));
    assertFalse(right.equals(left));
  }

  public void testEmptyByteArray() {
    ByteArray byteArray = new ByteArray(new byte[0]);
    assertNull(byteArray.byteBlocks);
    assertNull(byteArray.getBytes());
  }

  public void testNullByteArray() {
    ByteArray byteArray = new ByteArray((byte[])null);
    assertNull(byteArray.byteBlocks);
    assertNull(byteArray.getBytes());
  }

}
