package org.jbpm.command;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * This command can retrieve the matching process instances (e.g. for admin client) with the given
 * process-id, token id or task-id
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class GetProcessInstanceCommand extends AbstractGetObjectBaseCommand {

  private static final long serialVersionUID = -8436697080972165601L;

  private long processInstanceId;

  private long tokenId;

  private long taskInstanceId;

  public GetProcessInstanceCommand() {
  }

  public GetProcessInstanceCommand(long processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public GetProcessInstanceCommand(long processInstanceId, boolean includeVariables,
      boolean includeLogs) {
    super(true, true);
    this.processInstanceId = processInstanceId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    setJbpmContext(jbpmContext);

    ProcessInstance processInstance = null;
    if (processInstanceId != 0)
      processInstance = jbpmContext.getProcessInstance(processInstanceId);
    else if (tokenId != 0)
      processInstance = jbpmContext.getToken(tokenId).getProcessInstance();
    else if (taskInstanceId != 0)
      processInstance = jbpmContext.getTaskInstance(taskInstanceId).getProcessInstance();

    if (processInstance != null) {
      processInstance = retrieveProcessInstance(processInstance);
    }
    return processInstance;
  }

  public long getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(long processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public long getTaskInstanceId() {
    return taskInstanceId;
  }

  public void setTaskInstanceId(long taskInstanceId) {
    this.taskInstanceId = taskInstanceId;
  }

  public long getTokenId() {
    return tokenId;
  }

  public void setTokenId(long tokenId) {
    this.tokenId = tokenId;
  }

  public String getAdditionalToStringInformation() {
    return "processInstanceId="
        + processInstanceId
        + ";tokenId="
        + tokenId
        + ";taskInstanceId="
        + taskInstanceId;
  }

  // methods for fluent programming

  public GetProcessInstanceCommand processInstanceId(long processInstanceId) {
    setProcessInstanceId(processInstanceId);
    return this;
  }

  public GetProcessInstanceCommand taskInstanceId(long taskInstanceId) {
    setTaskInstanceId(taskInstanceId);
    return this;
  }

  public GetProcessInstanceCommand tokenId(long tokenId) {
    setTokenId(tokenId);
    return this;
  }
}
