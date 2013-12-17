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
package org.jbpm.examples.taskinstance;

// import java.io.Serializable;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class CustomTaskControllerHandler implements TaskControllerHandler
{

  /**
   * extracts all information from the process context (optionally indirect) and initializes the task instance variables.
   * <p>
   * Use {@link TaskInstance#setVariable(String, Object)} to set variables in the task instance and use {@link ContextInstance#getVariable(String, Token)} to get access
   * to the process variables.
   * </p>
   * <p>
   * The task instance variable can be
   * <ul>
   * <li>A copy of process variable value</li>
   * <li>A reference to a process variable</li>
   * <li>Any object that can be persisted as a variable. This is usefull in case a {@link TaskInstance} variable is a function of other process instance variables.</li>
   * </ul>
   * </p>
   * <p>
   * In order to create a reference to an existing process variable, insert a {@link org.jbpm.context.exe.VariableInstance} as a value in the returned map. If the
   * TaskInstance is to have copies of the process instance variables, just insert POJO's (non-VariableInstance classes) as values for the TaskInstance variables.
   * </p>
   */

  private static final long serialVersionUID = 1L;

  public void initializeTaskVariables(TaskInstance taskInstance, ContextInstance contextInstance, Token token)
  {

    CustomTaskInstance customTaskInstance = (CustomTaskInstance)taskInstance;

    String customId = (String)contextInstance.getVariable("customId");

    customTaskInstance.setCustomId(customId);
    // System.out.println("set customId: " + customId + " on taskInstance: " + taskInstance.getId());
  }

  /**
   * is called when a task completes. The task controller is given the opportunity to update the process context variables with the data that is submitted entered in
   * the task instance.
   * <p>
   * Use {@link TaskInstance#getVariable(String)} to get variables from the task instance context and use {@link ContextInstance#setVariable(String, Object, Token)} to
   * update the process variables.
   * </p>
   */
  public void submitTaskVariables(TaskInstance taskInstance, ContextInstance contextInstance, Token token)
  {

  }

}
