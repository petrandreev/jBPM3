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
package org.jbpm.command;

import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * end the task with the given id if variables are given as a map, they are added/changed bevore
 * ending the task
 * 
 * @author ??, Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class TaskInstanceEndCommand extends AbstractBaseCommand {

  private static final long serialVersionUID = 5721341060757950369L;

  private long taskInstanceId = 0;

  private String transitionName = null;

  private Map variables;

  private TaskInstance previoustaskInstance = null;

  public TaskInstanceEndCommand() {
  }

  public TaskInstanceEndCommand(long taskInstanceId, String transitionName) {
    this.taskInstanceId = taskInstanceId;
    this.transitionName = transitionName;
  }

  public TaskInstanceEndCommand(long taskInstanceId, String transitionName, Map variables) {
    this.taskInstanceId = taskInstanceId;
    this.transitionName = transitionName;
    this.variables = variables;
  }

  public Object execute(JbpmContext jbpmContext) {
    TaskInstance taskInstance = getTaskInstance(jbpmContext);

    if (variables != null && variables.size() > 0) {
      taskInstance.getContextInstance().addVariables(variables);
    }

    if (transitionName == null) {
      taskInstance.end();
    }
    else {
      taskInstance.end(transitionName);
    }
    return taskInstance;
  }

  protected TaskInstance getTaskInstance(JbpmContext jbpmContext) {
    if (previoustaskInstance != null) {
      return previoustaskInstance;
    }
    return jbpmContext.getTaskInstance(taskInstanceId);
  }

  public long getTaskInstanceId() {
    return taskInstanceId;
  }

  public void setTaskInstanceId(long taskInstanceId) {
    this.taskInstanceId = taskInstanceId;
  }

  public String getTransitionName() {
    return transitionName;
  }

  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }

  public Map getVariables() {
    return variables;
  }

  public void setVariables(Map variables) {
    this.variables = variables;
  }

  public String getAdditionalToStringInformation() {
    return "taskInstanceId="
        + taskInstanceId
        + ";transitionName="
        + transitionName
        + ";variables="
        + variables;
  }

  // methods for fluent programming

  public TaskInstanceEndCommand taskInstanceId(long taskInstanceId) {
    setTaskInstanceId(taskInstanceId);
    return this;
  }

  public TaskInstanceEndCommand transitionName(String transitionName) {
    setTransitionName(transitionName);
    return this;
  }

  public TaskInstanceEndCommand variables(Map variables) {
    setVariables(variables);
    return this;
  }
}
