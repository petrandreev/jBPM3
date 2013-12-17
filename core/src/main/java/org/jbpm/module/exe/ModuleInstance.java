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
package org.jbpm.module.exe;

import java.io.Serializable;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.svc.Service;
import org.jbpm.svc.Services;

public class ModuleInstance implements Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected ProcessInstance processInstance;

  public ModuleInstance() {
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleInstance)) return false;

    ModuleInstance other = (ModuleInstance) o;
    if (id != 0 && id == other.getId()) return true;

    return getClass().getName().equals(other.getClass().getName())
      && processInstance.equals(other.getProcessInstance());
  }

  public int hashCode() {
    int result = 1849786963 + getClass().getName().hashCode();
    result = 1566965963 * result + processInstance.hashCode();
    return result;
  }

  protected Service getService(String serviceName) {
    return Services.getCurrentService(serviceName, false);
  }

  // getters and setters //////////////////////////////////////////////////////

  public long getId() {
    return id;
  }

  public ProcessInstance getProcessInstance() {
    return processInstance;
  }

  public void setProcessInstance(ProcessInstance processInstance) {
    this.processInstance = processInstance;
  }
}
