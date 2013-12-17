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
package org.jbpm.context.def;

import java.io.Serializable;

public class Access implements Serializable {

  private static final long serialVersionUID = 1L;

  private String access = "read,write";

  public Access() {
  }

  public Access(String access) {
    if (access != null) {
      if (access.length() > 0) {
        this.access = access.toLowerCase();
      }
      else {
        // oracle treats an empty string as null; store a space instead
        this.access = " ";
      }
    }
  }

  public boolean isReadable() {
    return hasAccess("read");
  }

  public boolean isWritable() {
    return hasAccess("write");
  }

  public boolean isRequired() {
    return hasAccess("required");
  }

  public boolean isLock() {
    return hasAccess("lock");
  }

  /**
   * verifies if the given accessLiteral is included in the access text.
   */
  public boolean hasAccess(String accessLiteral) {
    return access != null ? access.indexOf(accessLiteral.toLowerCase()) != -1 : false;
  }

  public boolean equals(Object object) {
    if (object == this) return true;
    if (!(object instanceof Access)) return false;

    Access other = (Access) object;
    return isReadable() == other.isReadable() && isWritable() == other.isWritable()
      && isRequired() == other.isRequired() && isLock() == other.isLock();
  }

  public int hashCode() {
    int result = 265979407 + hashCode(isReadable());
    result = 752346139 * result + hashCode(isWritable());
    result = 376739687 * result + hashCode(isRequired());
    result = 750974239 * result + hashCode(isLock());
    return result;
  }

  private static int hashCode(boolean value) {
    return value ? 1231 : 1237;
  }

  public String toString() {
    return access;
  }
}
