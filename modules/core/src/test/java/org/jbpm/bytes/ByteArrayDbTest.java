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

import java.util.Arrays;

import org.jbpm.db.AbstractDbTestCase;

public class ByteArrayDbTest extends AbstractDbTestCase {

  public void testManyBlocks() {
    byte[] bytes = getMultipleBlockBytes();
    ByteArray byteArray = new ByteArray(bytes);
    session.save(byteArray);

    newTransaction();
    ByteArray retrievedByteArray = (ByteArray) session.load(ByteArray.class, new Long(
        byteArray.getId()));
    assertEquals(byteArray.byteBlocks.size(), retrievedByteArray.getByteBlocks().size());
    assertTrue(Arrays.equals(byteArray.getBytes(), retrievedByteArray.getBytes()));

    session.delete(retrievedByteArray);
  }

  public void testEmptyByteArray() {
    byte[] bytes = new byte[0];
    ByteArray byteArray = new ByteArray(bytes);
    session.save(byteArray);

    newTransaction();
    ByteArray retrievedByteArray = (ByteArray) session.load(ByteArray.class, new Long(
        byteArray.getId()));
    assertNull(retrievedByteArray.getBytes());

    session.delete(retrievedByteArray);
  }

  public void testByteArrayCopy() {
    byte[] bytes = getMultipleBlockBytes();
    ByteArray byteArray = new ByteArray(bytes);
    session.save(byteArray);

    newTransaction();
    ByteArray retrievedByteArray = (ByteArray) session.load(ByteArray.class, new Long(
        byteArray.getId()));
    ByteArray copiedByteArray = new ByteArray(retrievedByteArray);
    session.save(copiedByteArray);

    newTransaction();
    retrievedByteArray = (ByteArray) session.load(ByteArray.class, new Long(
        retrievedByteArray.getId()));
    copiedByteArray = (ByteArray) session.load(ByteArray.class, new Long(copiedByteArray.getId()));
    assertNotSame(retrievedByteArray.getByteBlocks(), copiedByteArray.getByteBlocks());

    for (int i = 0; i < retrievedByteArray.getByteBlocks().size(); i++) {
      byte[] retrievedBytes = (byte[]) retrievedByteArray.getByteBlocks().get(i);
      byte[] copiedBytes = (byte[]) copiedByteArray.getByteBlocks().get(i);

      assertNotSame(retrievedBytes, copiedBytes);
      assertTrue(Arrays.equals(retrievedBytes, copiedBytes));
    }

    session.delete(copiedByteArray);
    session.delete(retrievedByteArray);
  }

  public void testNullByteArray() {
    byte[] bytes = null;
    ByteArray byteArray = new ByteArray(bytes);
    session.save(byteArray);

    newTransaction();
    ByteArray retrievedByteArray = (ByteArray) session.load(ByteArray.class, new Long(
        byteArray.getId()));
    assertNull(retrievedByteArray.getBytes());

    session.delete(retrievedByteArray);
  }

  private static byte[] getMultipleBlockBytes() {
    byte[] bytes = new byte[2560];
    Arrays.fill(bytes, 0, 512, (byte) 67);
    Arrays.fill(bytes, 512, 1024, (byte) 65);
    Arrays.fill(bytes, 1024, 1536, (byte) 71);
    Arrays.fill(bytes, 1536, 2048, (byte) 70);
    Arrays.fill(bytes, 2048, 2560, (byte) 66);
    return bytes;
  }
}
