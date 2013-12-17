package org.jbpm.command;

import org.jbpm.graph.exe.ProcessInstance;

/**
 * Resume the specified {@link ProcessInstance}(s). See {@link AbstractProcessInstanceBaseCommand}
 * to check possibilities to specify {@link ProcessInstance}(s).
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ResumeProcessInstanceCommand extends AbstractProcessInstanceBaseCommand {

  private static final long serialVersionUID = 1L;

  public ProcessInstance execute(ProcessInstance processInstance) {
    processInstance.resume();
    return processInstance;
  }
}
