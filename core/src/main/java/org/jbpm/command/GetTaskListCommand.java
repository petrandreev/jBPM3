package org.jbpm.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.util.ArrayUtil;

/**
 * return a {@link java.util.List} of {@link org.jbpm.taskmgmt.exe.TaskInstance}s for the given
 * actor(s). if no actor is used, the current authenticated user is taken as actor. for all actors
 * it is checked, if they are pooled or assigned actor!
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class GetTaskListCommand extends AbstractGetObjectBaseCommand {

  private static final long serialVersionUID = -1627380259541998349L;

  static final Log log = LogFactory.getLog(GetTaskListCommand.class);

  private String[] actor;

  public GetTaskListCommand(String[] actor) {
    setActor(actor);
  }

  public GetTaskListCommand(String actor, boolean includeVariables) {
    super(includeVariables, false);
    setActor(actor);
  }

  public GetTaskListCommand(String actor, String[] variablesToInclude) {
    super(variablesToInclude);
    setActor(actor);
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    setJbpmContext(jbpmContext);
    List result = null;
    if (actor == null || actor.length == 0)
      result = jbpmContext.getTaskList();
    else {
      result = new ArrayList();
      for (int i = 0; i < actor.length; i++) {
        result.addAll(jbpmContext.getTaskList(actor[i]));
      }
      result.addAll(jbpmContext.getGroupTaskList(Arrays.asList(actor)));
    }

    return retrieveTaskInstanceDetails(result);
  }

  /**
   * access everything on all TaskInstance objects, which is not in the default fetch group from
   * hibernate, but needs to be accesible from the client overwrite this, if you need more details
   * in your client
   */
  public List retrieveTaskInstanceDetails(List taskInstanceList) {
    for (Iterator iter = taskInstanceList.iterator(); iter.hasNext();) {
      retrieveTaskInstanceDetails((TaskInstance) iter.next());
    }
    return taskInstanceList;
  }

  public String[] getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = new String[] { actor };
  }

  public void setActor(String[] actor) {
    this.actor = actor;
  }

  public String getAdditionalToStringInformation() {
    return "actors=" + ArrayUtil.toString(actor);
  }

  // methods for fluent programming

  public GetTaskListCommand actor(String actor) {
    this.actor = new String[] { actor };
    return this;
  }

  public GetTaskListCommand actor(String[] actor) {
    this.actor = actor;
    return this;
  }

}
