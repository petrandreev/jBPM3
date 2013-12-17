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

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.svc.Service;
import org.jbpm.util.EqualsUtil;

public class ModuleInstance implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  long id = 0;
  int version = 0;
  protected ProcessInstance processInstance = null;
  
  public ModuleInstance() {
  }

  // equals ///////////////////////////////////////////////////////////////////
  // hack to support comparing hibernate proxies against the real objects
  // since this always falls back to ==, we don't need to overwrite the hashcode
  public boolean equals(Object o) {
    return EqualsUtil.equals(this, o);
  }
  
  protected Service getService(String serviceName) {
    Service service = null;
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext!=null) {
      service = jbpmContext.getServices().getService(serviceName);
    }
    return service; 
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
