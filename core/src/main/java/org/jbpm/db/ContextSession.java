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
package org.jbpm.db;

import org.hibernate.Session;

/**
 * contains queries to search the database for process instances and tokens
 * based on process variableInstances.
 * <p><b>NOTE: TODO</b></p>
 */
public class ContextSession {

  final Session session;
  /** @deprecated */
  final JbpmSession jbpmSession;

  /**
   * @deprecated use {@link #ContextSession(Session)} instead
   */
  public ContextSession(JbpmSession jbpmSession) {
    this.jbpmSession = jbpmSession;
    this.session = jbpmSession.getSession();
  }

  public ContextSession(Session session) {
    this.session = session;
    this.jbpmSession = null;
  }

  /*
   * converts all newly created variable values that are inside the 
   * variableUpdates into {@link VariableInstance}s
   * as a preparation for storage in the database.
  public void mergeVariableUpdates(Map variableUpdates, Map variableInstances, TokenVariableMap tokenVariableMap, TaskInstance taskInstance, Token token) {
    if (variableUpdates!=null) {
      Iterator iter = variableUpdates.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        String name = (String) entry.getKey();
        Object value = entry.getValue();

        VariableInstance variableInstance = (variableInstances!=null ? (VariableInstance) variableInstances.get(name) : null);
        if ( (variableInstance!=null) 
             && (!variableInstance.supports(value))
           ) {
          // delete the old variable instance
          if (taskInstance!=null) {
            log.debug("task variable type change. deleting '"+name+"' from task instance '"+taskInstance+"'");
            taskInstance.deleteVariable(name);
          } else if (tokenVariableMap!=null) {
            log.debug("process variable type change. deleting '"+name+"' from token '"+token+"'");
            tokenVariableMap.deleteVariable(name); 
          }
          // make sure the variable instance gets recreated below
          variableInstances.remove(name);
        }

        if ( (variableInstances==null)
             || (! variableInstances.containsKey(name))
           ) {
          // it's a creation
          variableInstance = VariableInstance.create(token, name, value);
          
          // set the apropriate backpointers
          if (tokenVariableMap!=null) {
            log.debug("created variable instance '"+name+"' for token '"+token+"': "+value);
            tokenVariableMap.addVariableInstance(variableInstance);
          }
          if (taskInstance!=null) {
            log.debug("created variable instance '"+name+"' for task instance '"+taskInstance+"': "+value);
            taskInstance.addVariableInstance(variableInstance);
          }

        } else if (variableInstance!=null) {
          // it's an update
          variableInstance = (VariableInstance) variableInstances.get(name);
          if (taskInstance!=null) {
            log.debug("updated task variable instance '"+name+"' to '"+value+"' for '"+taskInstance+"'");
          } else {
            log.debug("updated process variable instance '"+name+"' to '"+value+"' for '"+token+"'");
          }
          variableInstance.setValue(value);
        }
      }
    }
  }

  public void updateProcessContextVariables(ContextInstance contextInstance) {
    if (contextInstance!=null) {
      Map tokenVariableMaps = contextInstance.getTokenVariableMaps();
      if (tokenVariableMaps!=null) {
        Iterator iter = tokenVariableMaps.values().iterator();
        while (iter.hasNext()) {
          TokenVariableMap tokenVariableMap = (TokenVariableMap) iter.next();
          mergeVariableUpdates(tokenVariableMap.getVariableUpdates(), tokenVariableMap.getVariableInstances(), tokenVariableMap, null, tokenVariableMap.getToken());
        }
      }
    }
  }

  public void updateTaskInstanceVariables(TaskMgmtInstance taskMgmtInstance) {
    if (taskMgmtInstance!=null) {
      Collection updatedTaskInstances = taskMgmtInstance.getTaskInstancesWithVariableUpdates();
      if (updatedTaskInstances!=null) {
        Iterator iter = updatedTaskInstances.iterator();
        while (iter.hasNext()) {
          TaskInstance taskInstance = (TaskInstance) iter.next();
          mergeVariableUpdates(taskInstance.getVariableUpdates(), taskInstance.getVariableInstances(), null, taskInstance, taskInstance.getToken());
        }
      }
    }
  }
   */
  
  // private static final Log log = LogFactory.getLog(ContextSession.class);
}
