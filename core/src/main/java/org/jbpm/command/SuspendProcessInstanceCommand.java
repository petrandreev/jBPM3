package org.jbpm.command;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Suspend the specified {@link ProcessInstance}(s). See {@link AbstractProcessInstanceBaseCommand}
 * to check possibilities to specify {@link ProcessInstance}(s). With filter to all
 * {@link ProcessDefinition}s this can be used like an emergency shutdown for
 * {@link ProcessDefinition}s.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SuspendProcessInstanceCommand extends AbstractProcessInstanceBaseCommand {

  private static final long serialVersionUID = 1L;

  public ProcessInstance execute(ProcessInstance processInstance) {
    processInstance.suspend();
    return processInstance;
  }

}
