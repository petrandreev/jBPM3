package org.jbpm.util;

import java.util.Arrays;

import org.jbpm.AbstractJbpmTestCase;

public class ByteUtilTest extends AbstractJbpmTestCase {

  public void testBytesToString() {
    byte[] bytes = new byte[] {
      (byte) 0x00, (byte) 0x01, (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x21,
      (byte) 0x99, (byte) 0x9a, (byte) 0xa9, (byte) 0xbc, (byte) 0xde, (byte) 0xff,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    assertEquals("000110111221999aa9bcdeff00000000", ByteUtil.toString(bytes));
  }

  public void testBytesFromString() {
    byte[] bytes = new byte[] {
      (byte) 0x00, (byte) 0x01, (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x21,
      (byte) 0x99, (byte) 0x9a, (byte) 0xa9, (byte) 0xbc, (byte) 0xde, (byte) 0xff,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    assertTrue(Arrays.equals(bytes, ByteUtil.fromString("000110111221999aa9bcdeff00000000")));
  }
}
