package org.jbpm.command;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * The current authorizes actor starts to work on the TaskInstance so the actor is set to the given
 * actor see some more information why we need that in the <a
 * href="http://www.jboss.com/index.html?module=bb&op=viewtopic&p=4018785">jbpm forum</a>
 * 
 * @author Bernd Ruecker
 */
public class CancelWorkOnTaskCommand extends AbstractBaseCommand {

  private static final long serialVersionUID = -172457633891242288L;

  private long taskInstanceId;

  public CancelWorkOnTaskCommand(long taskInstanceId) {
    this.taskInstanceId = taskInstanceId;
  }

  public CancelWorkOnTaskCommand() {
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    TaskInstance ti = jbpmContext.getTaskInstance(taskInstanceId);
    ti.setActorId(null);
    ti.setStart(null);
    return null;
  }

  public long getTaskInstanceId() {
    return taskInstanceId;
  }

  public void setTaskInstanceId(long taskInstanceId) {
    this.taskInstanceId = taskInstanceId;
  }

  public String getAdditionalToStringInformation() {
    return ";taskInstanceId=" + taskInstanceId;
  }

  // methods for fluent programming

  public CancelWorkOnTaskCommand taskInstanceId(long taskInstanceId) {
    setTaskInstanceId(taskInstanceId);
    return this;
  }

}
