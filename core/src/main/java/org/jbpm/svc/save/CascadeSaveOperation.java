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
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;

public class CascadeSaveOperation implements SaveOperation {

  private static final long serialVersionUID = 1L;

  public void save(ProcessInstance processInstance, JbpmContext jbpmContext) {
    Set cascadedProcessInstances = new HashSet();
    cascadedProcessInstances.add(processInstance);
    cascadeSave(processInstance.removeCascadeProcessInstances(), jbpmContext, cascadedProcessInstances);
  }

  private void cascadeSave(Collection cascadeProcessInstances, JbpmContext jbpmContext,
    Set cascadedProcessInstances) {
    if (cascadeProcessInstances != null) {
      for (Iterator iter = cascadeProcessInstances.iterator(); iter.hasNext();) {
        ProcessInstance cascadeInstance = (ProcessInstance) iter.next();
        saveCascadeInstance(cascadeInstance, jbpmContext, cascadedProcessInstances);
      }
    }
  }

  private void saveCascadeInstance(ProcessInstance cascadeInstance, JbpmContext jbpmContext,
    Set cascadedProcessInstances) {
    if (!cascadedProcessInstances.contains(cascadeInstance)) {
      if (log.isDebugEnabled()) log.debug("cascading save to " + cascadeInstance);
      jbpmContext.save(cascadeInstance);
      cascadedProcessInstances.add(cascadeInstance);

      Collection cascadeProcessInstances = cascadeInstance.removeCascadeProcessInstances();
      cascadeSave(cascadeProcessInstances, jbpmContext, cascadedProcessInstances);
    }
  }

  private static final Log log = LogFactory.getLog(CascadeSaveOperation.class);
}
