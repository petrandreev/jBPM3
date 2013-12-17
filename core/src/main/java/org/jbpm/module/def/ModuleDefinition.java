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
package org.jbpm.module.def;

import java.io.Serializable;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.module.exe.ModuleInstance;

public abstract class ModuleDefinition implements Serializable {

  long id;
  protected String name = getClass().getName();
  protected ProcessDefinition processDefinition;

  private static final long serialVersionUID = 1L;

  protected ModuleDefinition() {
    // for invocation by subclasses
  }

  public abstract ModuleInstance createInstance();

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleDefinition)) return false;

    ModuleDefinition other = (ModuleDefinition) o;
    if (id != 0 && id == other.getId()) return true;

    return name.equals(other.getName())
      && processDefinition.equals(other.getProcessDefinition());
  }

  public int hashCode() {
    int result = 2122701961 + name.hashCode();
    result = 1574886923 * result + processDefinition.hashCode();
    return result;
  }

  // getters and setters //////////////////////////////////////////////////////

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public ProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    this.processDefinition = processDefinition;
  }
}
