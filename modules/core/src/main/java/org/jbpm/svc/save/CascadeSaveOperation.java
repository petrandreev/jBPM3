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
package org.jbpm.svc.save;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;

public class CascadeSaveOperation implements SaveOperation {

  private static final long serialVersionUID = 1L;

  public void save(ProcessInstance processInstance, JbpmContext jbpmContext) {
    log.debug("cascading save of '" + processInstance + "'");
    Set<ProcessInstance> cascadedProcessInstances = new HashSet<ProcessInstance>();
    cascadedProcessInstances.add(processInstance);
    cascadeSave(processInstance.removeCascadeProcessInstances(), jbpmContext,
        cascadedProcessInstances);
  }

  void cascadeSave(Collection<ProcessInstance> cascadeProcessInstances, JbpmContext jbpmContext,
      Set<ProcessInstance> cascadedProcessInstances) {
    if (cascadeProcessInstances != null) {
      for (ProcessInstance cascadeInstance : cascadeProcessInstances) {
        saveCascadeInstance(cascadeInstance, jbpmContext, cascadedProcessInstances);
      }
    }
  }

  void saveCascadeInstance(ProcessInstance cascadeInstance, JbpmContext jbpmContext,
      Set<ProcessInstance> cascadedProcessInstances) {
    if (!cascadedProcessInstances.contains(cascadeInstance)) {
      Collection<ProcessInstance> cascadeProcessInstances = cascadeInstance.removeCascadeProcessInstances();
      log.debug("cascading save to process instance '" + cascadeInstance + "'");
      jbpmContext.save(cascadeInstance);
      cascadedProcessInstances.add(cascadeInstance);
      cascadeSave(cascadeProcessInstances, jbpmContext, cascadedProcessInstances);
    }
  }

  private static Log log = LogFactory.getLog(CascadeSaveOperation.class);
}
