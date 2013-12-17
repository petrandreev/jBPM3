package org.jbpm.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * The current authorizes actor starts to work on the TaskInstance so the actor is set to the given
 * actor
 * 
 * @author Bernd Ruecker
 */
public class StartWorkOnTaskCommand extends AbstractBaseCommand {

  private static final long serialVersionUID = 53004484398726736L;

  private static final Log log = LogFactory.getLog(StartWorkOnTaskCommand.class);

  private long taskInstanceId;

  private boolean overwriteSwimlane = false;

  private String actorId;

  public StartWorkOnTaskCommand(long taskInstanceId, boolean overwriteSwimlane) {
    this.taskInstanceId = taskInstanceId;
    this.overwriteSwimlane = overwriteSwimlane;
  }

  public StartWorkOnTaskCommand() {
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    String actor = this.actorId == null ? jbpmContext.getActorId() : this.actorId;
    TaskInstance taskInstance = jbpmContext.getTaskInstance(taskInstanceId);

    if (taskInstance.getStart() != null) {
      log.warn("Force stop on task " + taskInstance.getId() + ". Will be restarted.");
      taskInstance.setStart(null); // strange, but means isNotStarted()
    }

    taskInstance.start(actor, overwriteSwimlane);

    return null;
  }

  public boolean isOverwriteSwimlane() {
    return overwriteSwimlane;
  }

  public void setOverwriteSwimlane(boolean overwriteSwimlane) {
    this.overwriteSwimlane = overwriteSwimlane;
  }

  public long getTaskInstanceId() {
    return taskInstanceId;
  }

  public void setTaskInstanceId(long taskInstanceId) {
    this.taskInstanceId = taskInstanceId;
  }

  public void setActorId(String actorId) {
    this.actorId = actorId;
  }

  public String getActorId() {
    return actorId;
  }

  public String getAdditionalToStringInformation() {
    return "tokenId="
        + taskInstanceId
        + ";transitionName="
        + actorId
        + ";processDefinitionName="
        + overwriteSwimlane;
  }

  // methods for fluent programming

  public StartWorkOnTaskCommand overwriteSwimlane(boolean overwriteSwimlane) {
    setOverwriteSwimlane(overwriteSwimlane);
    return this;
  }

  public StartWorkOnTaskCommand taskInstanceId(long taskInstanceId) {
    setTaskInstanceId(taskInstanceId);
    return this;
  }

  public StartWorkOnTaskCommand actorId(String actorId) {
    setActorId(actorId);
    return this;
  }
}
