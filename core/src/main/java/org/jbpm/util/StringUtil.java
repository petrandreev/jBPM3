package org.jbpm.util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/** @deprecated not in use anymore */
public class StringUtil implements Serializable {

  private static final long serialVersionUID = 1L;

  static final byte[] HEX_CHAR_TABLE = {
    (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
    (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',
    (byte) 'e', (byte) 'f'
  };

  private StringUtil() {
    // hide default constructor to prevent instantiation
  }

  public static String toHexString(byte[] bytes) {
    try {
      byte[] hex = new byte[2 * bytes.length];
      int index = 0;

      for (int i = 0; i < bytes.length; i++) {
        byte b = bytes[i];
        int v = b & 0xFF;
        hex[index++] = HEX_CHAR_TABLE[v >>> 4];
        hex[index++] = HEX_CHAR_TABLE[v & 0xF];
      }
      return new String(hex, "US-ASCII");
    }
    catch (UnsupportedEncodingException e) {
      // should not happen, US-ASCII is a standard charset
      throw new AssertionError(e);
    }
  }

  public static String toHexStringHibernate(byte[] bytes) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < bytes.length; i++) {
      String hexStr = Integer.toHexString(bytes[i] - Byte.MIN_VALUE);
      if (hexStr.length() == 1) buf.append('0');
      buf.append(hexStr);
    }
    return buf.toString();
  }
}