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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.Delegation;
import org.jbpm.instantiation.UserCodeInterceptor;
import org.jbpm.instantiation.UserCodeInterceptorConfig;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * is a controller for one task. this object either delegates to a custom
 * {@link org.jbpm.taskmgmt.def.TaskControllerHandler} or it is configured with
 * {@link org.jbpm.context.def.VariableAccess}s to perform the default behaviour of the
 * controller functionality for a task.
 */
public class TaskController implements Serializable {

  private static final long serialVersionUID = 1L;

  long id;

  /**
   * allows the user to specify a custom task controller handler. if this is specified, the
   * other member variableInstances are ignored. so either a taskControllerDelegation is
   * specified or the variable- and signalMappings are specified, but not both.
   */
  private Delegation taskControllerDelegation;

  /**
   * maps process variable names (java.lang.String) to VariableAccess objects.
   */
  private List variableAccesses;

  public TaskController() {
  }

  /**
   * extract the list of information from the process variables and make them available locally.
   * Note that if no task instance variables are specified, the full process variables scope
   * will be visible (that means that the user did not specify a special task instance scope).
   */
  public void initializeVariables(TaskInstance taskInstance) {
    ClassLoader surroundingClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      // set context class loader correctly for delegation class
      // https://jira.jboss.org/jira/browse/JBPM-1448
      ClassLoader classLoader = JbpmConfiguration.getProcessClassLoader(taskInstance.getTask()
        .getProcessDefinition());
      Thread.currentThread().setContextClassLoader(classLoader);

      if (taskControllerDelegation != null) {
        TaskControllerHandler taskControllerHandler = (TaskControllerHandler) taskControllerDelegation.instantiate();
        ProcessInstance processInstance = taskInstance.getTaskMgmtInstance()
          .getProcessInstance();
        ContextInstance contextInstance = (processInstance != null ? processInstance.getContextInstance()
          : null);
        Token token = taskInstance.getToken();

        UserCodeInterceptor userCodeInterceptor = UserCodeInterceptorConfig.getUserCodeInterceptor();
        if (userCodeInterceptor != null) {
          userCodeInterceptor.executeTaskControllerInitialization(taskControllerHandler, taskInstance, contextInstance, token);
        }
        else {
          taskControllerHandler.initializeTaskVariables(taskInstance, contextInstance, token);
        }
      }
      else {
        Token token = taskInstance.getToken();
        ProcessInstance processInstance = token.getProcessInstance();
        ContextInstance contextInstance = processInstance.getContextInstance();

        if (variableAccesses != null) {
          boolean debug = log.isDebugEnabled();
          for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
            VariableAccess variableAccess = (VariableAccess) iter.next();
            String mappedName = variableAccess.getMappedName();
            if (variableAccess.isReadable()) {
              String variableName = variableAccess.getVariableName();
              Object value = contextInstance.getVariable(variableName, token);
              if (debug) {
                log.debug(taskInstance + " reads '" + variableName + "' into '" + mappedName
                  + '\'');
              }
              taskInstance.setVariableLocally(mappedName, value);
            }
            else {
              if (debug) log.debug(token + " initializes '" + mappedName + " to null");
              taskInstance.setVariableLocally(mappedName, null);
            }
          }
        }
      }
    }
    finally {
      Thread.currentThread().setContextClassLoader(surroundingClassLoader);
    }
  }

  /**
   * update the process variables from the the task-instance variables.
   */
  public void submitParameters(TaskInstance taskInstance) {
    if (taskControllerDelegation != null) {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      try {
        // set context class loader correctly for delegation class
        // https://jira.jboss.org/jira/browse/JBPM-1448
        ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(taskInstance.getTask()
          .getProcessDefinition());
        Thread.currentThread().setContextClassLoader(processClassLoader);

        TaskControllerHandler taskControllerHandler = (TaskControllerHandler) taskControllerDelegation.instantiate();
        Token token = taskInstance.getToken();
        ContextInstance contextInstance = token != null ? token.getProcessInstance()
          .getContextInstance() : null;

        UserCodeInterceptor userCodeInterceptor = UserCodeInterceptorConfig.getUserCodeInterceptor();
        if (userCodeInterceptor != null) {
          userCodeInterceptor.executeTaskControllerSubmission(taskControllerHandler, taskInstance, contextInstance, token);
        }
        else {
          taskControllerHandler.submitTaskVariables(taskInstance, contextInstance, token);
        }
      }
      finally {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
    }
    else if (variableAccesses != null) {
      List missingTaskVariables = null;
      for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
        VariableAccess variableAccess = (VariableAccess) iter.next();
        String mappedName = variableAccess.getMappedName();
        // first check if the required variableInstances are present
        if (variableAccess.isRequired() && !taskInstance.hasVariableLocally(mappedName)) {
          if (missingTaskVariables == null) missingTaskVariables = new ArrayList();
          missingTaskVariables.add(mappedName);
        }
      }

      // if there are missing, required parameters, puke
      if (missingTaskVariables != null) {
        throw new IllegalArgumentException("missing task variables: " + missingTaskVariables);
      }

      Token token = taskInstance.getToken();
      ContextInstance contextInstance = token.getProcessInstance().getContextInstance();
      boolean debug = log.isDebugEnabled();

      for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
        VariableAccess variableAccess = (VariableAccess) iter.next();
        if (variableAccess.isWritable()) {
          String mappedName = variableAccess.getMappedName();
          Object value = taskInstance.getVariable(mappedName);
          if (value != null) {
            String variableName = variableAccess.getVariableName();
            if (debug) {
              log.debug(taskInstance + " writes '" + variableName + "' from '" + mappedName
                + '\'');
            }
            contextInstance.setVariable(variableName, value, token);
          }
        }
      }
    }
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TaskController)) return false;

    // task controller has no notion of equality beyond identity
    TaskController other = (TaskController) o;
    return id != 0 && id == other.getId();
  }

  // getters and setters //////////////////////////////////////////////////////

  public List getVariableAccesses() {
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

  public void setVariableAccesses(List variableAccesses) {
    this.variableAccesses = variableAccesses;
  }

  private static final Log log = LogFactory.getLog(TaskController.class);
}
