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

/**
 * Helper methods for dealing with classes.
 * 
 * @author Alejandro Guizar
 */
public class ClassUtil {

  private ClassUtil() {
    // hide default constructor to prevent instantiation
  }

  /**
   * Returns the simple name of the given class as specified in the source code. Returns an empty
   * string if the underlying class is anonymous.
   * <p>
   * The simple name of an array is the simple name of the component type with "[]" appended. In
   * particular the simple name of an array whose component type is anonymous is "[]".
   * 
   * @return the simple name of the underlying class
   * @see <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Class.html#getSimpleName()">
   *      java.lang.Class.getSimpleName()</a>
   */
  public static String getSimpleName(Class clazz) {
    if (clazz.isArray()) return getSimpleName(clazz.getComponentType()) + "[]";

    String simpleName = getSimpleBinaryName(clazz);
    if (simpleName == null) { // top level class
      simpleName = clazz.getName();
      return simpleName.substring(simpleName.lastIndexOf(".") + 1); // strip the package name
    }

    // Remove leading "\$[0-9]*" from the name
    int length = simpleName.length();
    if (length < 1 || simpleName.charAt(0) != '$') throw new InternalError("Malformed class name");
    int index = 1;
    while (index < length && isAsciiDigit(simpleName.charAt(index)))
      index++;
    // Eventually, this is the empty string iff this is an anonymous class
    return simpleName.substring(index);
  }

  /**
   * Returns the "simple binary name" of the given class, i.e., the binary name without the leading
   * enclosing class name. Returns <tt>null</tt> if the underlying class is a top level class.
   */
  private static String getSimpleBinaryName(Class clazz) {
    Class enclosingClass = clazz.getDeclaringClass();
    if (enclosingClass == null) // top level class
      return null;
    // Otherwise, strip the enclosing class' name
    try {
      return clazz.getName().substring(enclosingClass.getName().length());
    }
    catch (IndexOutOfBoundsException ex) {
      throw new InternalError("Malformed class name");
    }
  }

  /**
   * Character.isDigit answers <tt>true</tt> to some non-ascii digits. This one does not.
   */
  private static boolean isAsciiDigit(char c) {
    return '0' <= c && c <= '9';
  }
}
