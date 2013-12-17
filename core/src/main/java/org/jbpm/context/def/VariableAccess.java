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

/**
 * specifies access to a variable. Variable access is used in 3 situations: 1) process-state 2)
 * script 3) task controllers
 */
public class VariableAccess implements Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  protected String variableName;
  protected Access access;
  protected String mappedName;

  // constructors /////////////////////////////////////////////////////////////

  public VariableAccess() {
  }

  public VariableAccess(String variableName, String access, String mappedName) {
    this.variableName = variableName;
    this.access = new Access(access);
    this.mappedName = mappedName;
  }

  // getters and setters //////////////////////////////////////////////////////

  /**
   * the mapped name. The mappedName defaults to the variableName in case no mapped name is
   * specified.
   */
  public String getMappedName() {
    return mappedName != null ? mappedName : variableName;
  }

  /**
   * specifies a comma separated list of access literals {read, write, required}.
   */
  public Access getAccess() {
    return access;
  }

  public String getVariableName() {
    return variableName;
  }

  public boolean isReadable() {
    return access.isReadable();
  }

  public boolean isWritable() {
    return access.isWritable();
  }

  public boolean isRequired() {
    return access.isRequired();
  }

  public boolean isLock() {
    return access.isLock();
  }
}
