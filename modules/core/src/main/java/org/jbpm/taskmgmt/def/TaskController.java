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
package org.jbpm.taskmgmt.def;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.Delegation;
import org.jbpm.instantiation.UserCodeInterceptorConfig;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.util.EqualsUtil;

/**
 * is a controller for one task. this object either delegates to a custom
 * {@link org.jbpm.taskmgmt.def.TaskControllerHandler} or it is configured
 * with {@link org.jbpm.context.def.VariableAccess}s to perform the default 
 * behaviour of the controller functionality for a task.
 */
public class TaskController implements Serializable {

  private static final long serialVersionUID = 1L;
  
  long id = 0;

  /**
   * allows the user to specify a custom task controller handler. if this is
   * specified, the other member variableInstances are ignored. so either a
   * taskControllerDelegation is specified or the variable- and signalMappings
   * are specified, but not both.
   */
  Delegation taskControllerDelegation = null;

  /**
   * maps process variable names (java.lang.String) to VariableAccess objects.
   */
  List<VariableAccess> variableAccesses = null;
  
  public TaskController() {
  }
  
  /**
   * extract the list of information from the process variables and make them available locally.
   * Note that if no task instance variables are specified, the full process variables scope will be 
   * visible (that means that the user did not specify a special task instance scope). 
   */
  public void initializeVariables(TaskInstance taskInstance) {
    ClassLoader surroundingClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      // set context class loader correctly for delegation class (https://jira.jboss.org/jira/browse/JBPM-1448)      
      Thread.currentThread().setContextClassLoader(JbpmConfiguration.getProcessClassLoader(taskInstance.getTask().getProcessDefinition()));

      if (taskControllerDelegation != null) {
        TaskControllerHandler taskControllerHandler = (TaskControllerHandler) taskControllerDelegation.instantiate();
        ProcessInstance processInstance = taskInstance.getTaskMgmtInstance().getProcessInstance();
        ContextInstance contextInstance = (processInstance != null ? processInstance.getContextInstance() : null);
        Token token = taskInstance.getToken();

        if (UserCodeInterceptorConfig.userCodeInterceptor != null) {
          UserCodeInterceptorConfig.userCodeInterceptor.executeTaskControllerInitialization(taskControllerHandler, taskInstance, contextInstance, token);
        } else {
          taskControllerHandler.initializeTaskVariables(taskInstance, contextInstance, token);
        }

      } else {
        Token token = taskInstance.getToken();
        ProcessInstance processInstance = token.getProcessInstance();
        ContextInstance contextInstance = processInstance.getContextInstance();

        if (variableAccesses != null) {
          for (VariableAccess variableAccess : variableAccesses) {
            String mappedName = variableAccess.getMappedName();
            if (variableAccess.isReadable()) {
              String variableName = variableAccess.getVariableName();
              Object value = contextInstance.getVariable(variableName, token);
              log.debug("creating task instance variable '" + mappedName + "' from process variable '" + variableName + "', value '" + value + "'");
              taskInstance.setVariableLocally(mappedName, value);
            } else {
              log.debug("creating task instance local variable '" + mappedName + "'. initializing with null value.");
              taskInstance.setVariableLocally(mappedName, null);
            }
          }
        }
      }
    } finally {
      Thread.currentThread().setContextClassLoader(surroundingClassLoader);
    }     
  }

  /**
   * update the process variables from the the task-instance variables. 
   */
  public void submitParameters(TaskInstance taskInstance) {
    ClassLoader surroundingClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      // set context class loader correctly for delegation class (https://jira.jboss.org/jira/browse/JBPM-1448) 
      Thread.currentThread().setContextClassLoader(JbpmConfiguration.getProcessClassLoader(taskInstance.getTask().getProcessDefinition()));

      if (taskControllerDelegation != null) {
        TaskControllerHandler taskControllerHandler = (TaskControllerHandler) taskControllerDelegation.instantiate();
        ProcessInstance processInstance = taskInstance.getTaskMgmtInstance().getProcessInstance();
        ContextInstance contextInstance = (processInstance != null ? processInstance.getContextInstance() : null);
        Token token = taskInstance.getToken();

        if (UserCodeInterceptorConfig.userCodeInterceptor != null) {
          UserCodeInterceptorConfig.userCodeInterceptor.executeTaskControllerSubmission(taskControllerHandler, taskInstance, contextInstance, token);
        } else {
          taskControllerHandler.submitTaskVariables(taskInstance, contextInstance, token);
        }

      } else {

        Token token = taskInstance.getToken();
        ProcessInstance processInstance = token.getProcessInstance();
        ContextInstance contextInstance = processInstance.getContextInstance();

        if (variableAccesses != null) {
          String missingTaskVariables = null;
          for (VariableAccess variableAccess : variableAccesses) {
            String mappedName = variableAccess.getMappedName();
            // first check if the required variableInstances are present
            if ((variableAccess.isRequired()) && (!taskInstance.hasVariableLocally(mappedName))) {
              if (missingTaskVariables == null) {
                missingTaskVariables = mappedName;
              } else {
                missingTaskVariables += ", " + mappedName;
              }
            }
          }

          // if there are missing, required parameters, throw an
          // IllegalArgumentException
          if (missingTaskVariables != null) {
            throw new IllegalArgumentException("missing task variables: " + missingTaskVariables);
          }

          for (VariableAccess variableAccess : variableAccesses) {
            String mappedName = variableAccess.getMappedName();
            String variableName = variableAccess.getVariableName();
            if (variableAccess.isWritable()) {
              Object value = taskInstance.getVariable(mappedName);
              if (value != null) {
                log.debug("submitting task variable '" + mappedName + "' to process variable '" + variableName + "', value '" + value + "'");
                contextInstance.setVariable(variableName, value, token);
              }
            }
          }
        }
      }
    } finally {
      Thread.currentThread().setContextClassLoader(surroundingClassLoader);
    }
  }

  // equals ///////////////////////////////////////////////////////////////////
  // hack to support comparing hibernate proxies against the real objects
  // since this always falls back to ==, we don't need to overwrite the hashcode
  public boolean equals(Object o) {
    return EqualsUtil.equals(this, o);
  }
  
  // getters and setters //////////////////////////////////////////////////////

  public List<VariableAccess> getVariableAccesses() {
    return variableAccesses;
  }
  public Delegation getTaskControllerDelegation() {
    return taskControllerDelegation;
  }
  public void setTaskControllerDelegation(Delegation taskControllerDelegation) {
    this.taskControllerDelegation = taskControllerDelegation;
  }
  public long getId() {
    return id;
  }

  public void setVariableAccesses(List<VariableAccess> variableAccesses) {
    this.variableAccesses = variableAccesses;
  }
  
  private static Log log = LogFactory.getLog(TaskController.class);
}
