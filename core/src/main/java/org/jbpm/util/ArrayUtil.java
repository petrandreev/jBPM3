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
package org.jbpm.util;

import java.util.List;

/**
 * Various methods for manipulating arrays.
 */
public class ArrayUtil {

  private ArrayUtil() {
    // hide default constructor to prevent instantiation
  }

  /**
   * Returns a hash code based on the contents of the specified array. For any
   * two <tt>byte</tt> arrays <tt>a</tt> and <tt>b</tt> such that
   * <tt>Arrays.equals(a, b)</tt>, it is also the case that
   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
   * 
   * <p>
   * The value returned by this method is the same value that would be obtained
   * by invoking the {@link List#hashCode() <tt>hashCode</tt>} method on a
   * {@link List} containing a sequence of {@link Byte} instances representing
   * the elements of <tt>a</tt> in the same order. If <tt>a</tt> is
   * <tt>null</tt>, this method returns 0.
   * 
   * @param a the array whose hash value to compute
   * @return a content-based hash code for <tt>a</tt>
   */
  public static int hashCode(byte a[]) {
    if (a == null) {
      return 0;
    }

    int result = 1;
    for (int i = 0; i < a.length; i++) {
      result = 31 * result + a[i];
    }

    return result;
  }

  /**
   * Returns a string representation of the contents of the specified array. If
   * the array contains other arrays as elements, they are converted to strings
   * by the {@link Object#toString} method inherited from <tt>Object</tt>, which
   * describes their <i>identities</i> rather than their contents.
   * <p>
   * The value returned by this method is equal to the value that would be
   * returned by <tt>Arrays.asList(a).toString()</tt>, unless the array is
   * <tt>null</tt>, in which case <tt>"null"</tt> is returned.
   * 
   * @param a the array whose string representation to return
   * @return a string representation of <tt>a</tt>
   * @see <a
   * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Arrays.html#toString(Object[])">
   * java.util.Arrays.toString(Object[])</a>
   */
  public static String toString(Object[] a) {
    if (a == null) return "null";
    if (a.length == 0) return "[]";

    StringBuffer buf = new StringBuffer();
    buf.append('[').append(a[0]);

    for (int i = 1; i < a.length; i++) {
      buf.append(", ").append(a[i]);
    }

    return buf.append(']').toString();
  }

  /**
   * Returns a string representation of the contents of the specified array. The
   * string representation consists of a list of the array's elements, enclosed
   * in square brackets ( <tt>"[]"</tt>). Adjacent elements are separated by the
   * characters <tt>", "</tt> (a comma followed by a space). Elements are
   * converted to strings by <tt>String.valueOf(long)</tt>. Returns
   * <tt>"null"</tt> if the array is <tt>null</tt>.
   * 
   * @param a the array whose string representation to return
   * @return a string representation of <tt>a</tt>
   * @see <a
   * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Arrays.html#toString(long[])">
   * java.util.Arrays.toString(long[])</a>
   */
  public static String toString(long[] a) {
    if (a == null) return "null";
    if (a.length == 0) return "[]";

    StringBuffer buf = new StringBuffer();
    buf.append('[');
    buf.append(a[0]);

    for (int i = 1; i < a.length; i++) {
      buf.append(", ").append(a[i]);
    }

    return buf.append(']').toString();
  }

  /**
   * Returns the index in the given array of the first occurrence of the
   * specified element, or -1 if the array does not contain this element.
   * 
   * @param o element to search for.
   * @return the index of the first occurrence of the specified element, or -1
   * if the array does not contain this element.
   */
  public static int indexOf(Object[] a, Object o) {
    if (o == null) {
      for (int i = 0; i < a.length; i++)
        if (a[i] == null) return i;
    }
    else {
      for (int i = 0; i < a.length; i++)
        if (o.equals(a[i])) return i;
    }
    return -1;
  }

  /**
   * Tells whether the given array contains the specified element.
   * 
   * @param o element whose presence in the array is to be tested.
   * @return <tt>true</tt> if the array contains the specified element.
   */
  public static boolean contains(Object[] a, Object o) {
    return indexOf(a, o) != -1;
  }
}
