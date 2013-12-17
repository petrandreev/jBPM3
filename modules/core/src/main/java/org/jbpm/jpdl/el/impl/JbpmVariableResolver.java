package org.jbpm.jpdl.el.impl;

// $Id: JbpmVariableResolver.java 3651 2009-01-15 16:26:31Z thomas.diesler@jboss.com $

import org.jbpm.JbpmConfiguration;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.ELException;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

public class JbpmVariableResolver implements VariableResolver
{

  public Object resolveVariable(String name) throws ELException
  {
    ExecutionContext executionContext = ExecutionContext.currentExecutionContext();
    Object value = null;

    if ("taskInstance".equals(name))
    {
      value = executionContext.getTaskInstance();

    }
    else if ("processInstance".equals(name))
    {
      value = executionContext.getProcessInstance();

    }
    else if ("processDefinition".equals(name))
    {
      value = executionContext.getProcessDefinition();

    }
    else if ("token".equals(name))
    {
      value = executionContext.getToken();

    }
    else if ("taskMgmtInstance".equals(name))
    {
      value = executionContext.getTaskMgmtInstance();

    }
    else if ("contextInstance".equals(name))
    {
      value = executionContext.getContextInstance();

    }
    else if ((executionContext.getTaskInstance() != null) && (executionContext.getTaskInstance().hasVariableLocally(name)))
    {
      value = executionContext.getTaskInstance().getVariable(name);

    }
    else
    {
      ContextInstance contextInstance = executionContext.getContextInstance();
      TaskMgmtInstance taskMgmtInstance = executionContext.getTaskMgmtInstance();
      Token token = executionContext.getToken();

      if ((contextInstance != null) && (contextInstance.hasVariable(name, token)))
      {
        value = contextInstance.getVariable(name, token);
      }
      else if ((contextInstance != null) && (contextInstance.hasTransientVariable(name)))
      {
        value = contextInstance.getTransientVariable(name);
      }
      else if ((taskMgmtInstance != null) && (taskMgmtInstance.getSwimlaneInstances() != null) && (taskMgmtInstance.getSwimlaneInstances().containsKey(name)))
      {
        SwimlaneInstance swimlaneInstance = taskMgmtInstance.getSwimlaneInstance(name);
        value = (swimlaneInstance != null ? swimlaneInstance.getActorId() : null);

      }
      else if (JbpmConfiguration.Configs.hasObject(name))
      {
        value = JbpmConfiguration.Configs.getObject(name);
      }
    }

    return value;
  }
}
