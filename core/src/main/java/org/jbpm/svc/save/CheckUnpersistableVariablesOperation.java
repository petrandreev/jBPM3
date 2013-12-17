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
import java.util.Iterator;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.VariableContainer;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.variableinstance.UnpersistableInstance;
import org.jbpm.graph.exe.ProcessInstance;

public class CheckUnpersistableVariablesOperation implements SaveOperation {

  private static final long serialVersionUID = 1L;

  public void save(ProcessInstance processInstance, JbpmContext jbpmContext) {
    Collection variableContainers = processInstance.getContextInstance()
      .getUpdatedVariableContainers();
    // iterate over variable containers
    if (variableContainers != null) {
      for (Iterator containerIter = variableContainers.iterator(); containerIter.hasNext();) {
        VariableContainer variableContainer = (VariableContainer) containerIter.next();
        Map variableInstances = variableContainer.getVariableInstances();
        // iterate over variable instances
        if (variableInstances != null) {
          for (Iterator instanceIter = variableInstances.values().iterator(); instanceIter.hasNext();) {
            VariableInstance variableInstance = (VariableInstance) instanceIter.next();
            // if the variable cannot be persisted... boom!
            if (variableInstance instanceof UnpersistableInstance) {
              throw new JbpmException(variableInstance + " cannot be persisted");
            }
          }
        }
      }
    }
  }

}
