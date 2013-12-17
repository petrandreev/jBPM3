package org.jbpm.util;

public class ByteUtil {

  private ByteUtil() {
    // hide default constructor to prevent instantiation
  }

  public static String toString(byte[] bytes) {
    if (bytes == null) return "null";
    if (bytes.length == 0) return "[]";
    StringBuffer buf = new StringBuffer();
    for ( int i=0; i<bytes.length; i++ ) { 
      byte b = bytes[i];
      buf.append(toHexChar((b>>>4)&0x0F));
      buf.append(toHexChar(b&0x0F));
    }
    return buf.toString();
  }

  public static char toHexChar(int i) {
    if ((0 <= i) && (i <= 9))
      return (char) ('0' + i);
    else
      return (char) ('a' + (i - 10));
  }
  
  public static byte[] fromString(String hexString) {
    if (hexString==null) return null;
    if (hexString.length() % 2 != 0)
        throw new IllegalArgumentException("invalid hex string: odd number of hex digits");
    int byteArraySize = hexString.length()/2;
    byte[] bytes = new byte[byteArraySize];
    for (int i=0; i<bytes.length; i++) {
      int stringIndex = i*2;
      String byteString = hexString.substring(stringIndex, stringIndex+2);
      bytes[i] = (byte)Integer.parseInt(byteString, 16);
    }
    return bytes;
  } 
}
