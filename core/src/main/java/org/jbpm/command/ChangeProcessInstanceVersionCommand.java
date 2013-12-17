package org.jbpm.command;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;

import org.jbpm.JbpmException;
import org.jbpm.db.GraphSession;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Node.NodeType;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.ProcessState;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.job.Job;
import org.jbpm.job.Timer;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * Migrate a process instance to a different version of its process definition.
 * <p>
 * Migration works only if the active nodes are also available in the target process definition
 * version or a node name mapping is provided.
 * </p>
 * <h3>Known limitations</h3>
 * <ul>
 * <li>{@link Task} definitions cannot move to another node. If an active {@link TaskInstance}
 * exists, the {@link Task} definition must exist in the {@link TaskNode} with the same (or
 * mapped) name. Otherwise the right node cannot be found easily because it may be ambiguous.</li>
 * <li>Subprocesses are not tested yet. Since the {@link ProcessState} is a {@link Node} like
 * any other, it should work anyway.</li>
 * <li>Migration can have <b>negative impact on referential integrity</b> because the
 * {@link ProcessInstance} may have {@link ProcessLog}s pointing to the old
 * {@link ProcessDefinition}. Hence, deleting a {@link ProcessDefinition} may not work and throw
 * constraint violation exceptions.</li>
 * <li>The JBoss ESB uses {@link Token}.id <b>and</b> {@link Node}.id as correlation identifier.
 * After changing the version of a {@link ProcessInstance} the node identifier has changed, so a
 * signal from ESB will result in an exception and has to be corrected manually.</li>
 * </ul>
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class ChangeProcessInstanceVersionCommand extends AbstractProcessInstanceBaseCommand {

  private static final long serialVersionUID = 2277080393930008224L;
  private static final Log log = LogFactory.getLog(ChangeProcessInstanceVersionCommand.class);

  /**
   * new process definition version. if <=0, the latest process definition is used
   */
  private int newVersion = -1;

  /**
   * maps node names in the old process definition to node names in the new process definition.
   * if there is no entry for a node, the old node name is applied.
   */
  private Map nodeNameMapping;

  /**
   * maps task names in the old process definition to tasks names in the new process definition.
   * if there is no entry for a task, the old task name is applied.
   */
  private Map taskNameMapping;

  public ChangeProcessInstanceVersionCommand() {
  }

  public ChangeProcessInstanceVersionCommand(long processInstanceId, int newVersion) {
    super.setProcessInstanceId(processInstanceId);
    this.newVersion = newVersion;
  }

  public String getAdditionalToStringInformation() {
    return ";newVersion=" + newVersion;
  }

  private ProcessDefinition findNewProcessDefinition(String processName) {
    GraphSession graphSession = getJbpmContext().getGraphSession();
    return newVersion <= 0 ? graphSession.findLatestProcessDefinition(processName)
      : graphSession.findProcessDefinition(processName, newVersion);
  }

  public ProcessInstance execute(ProcessInstance pi) {
    ProcessDefinition oldDef = pi.getProcessDefinition();
    ProcessDefinition newDef = findNewProcessDefinition(oldDef.getName());

    boolean debug = log.isDebugEnabled();
    if (debug) {
      log.debug("migrating " + pi + " from version " + oldDef.getVersion() + " to "
        + newDef.getVersion());
    }
    pi.setProcessDefinition(newDef);
    changeTokenVersion(pi.getRootToken());

    if (debug) log.debug(pi + " migrated to version " + newDef.getVersion());
    return pi;
  }

  private void changeTokenVersion(Token token) {
    // change node reference on token (current node)
    Node oldNode = token.getNode();
    ProcessDefinition newDef = token.getProcessInstance().getProcessDefinition();
    Node newNode = findReplacementNode(newDef, oldNode);
    token.setNode(newNode);

    // Change timers too!
    adjustTimersForToken(token);

    // change tasks
    adjustTaskInstancesForToken(token);

    // change children recursively
    Map children = token.getChildren();
    if (children != null) {
      for (Iterator i = children.values().iterator(); i.hasNext();) {
        changeTokenVersion((Token) i.next());
      }
    }
  }

  private void adjustTaskInstancesForToken(Token token) {
    ProcessDefinition newDef = token.getProcessInstance().getProcessDefinition();
    boolean debug = log.isDebugEnabled();

    for (Iterator i = getTasksForToken(token).iterator(); i.hasNext();) {
      TaskInstance ti = (TaskInstance) i.next();

      // find new task
      Task oldTask = ti.getTask();
      Node oldNode = oldTask.getTaskNode();

      Task newTask = findReplacementTask(newDef, oldNode, oldTask);
      ti.setTask(newTask);
      if (debug) log.debug("adjusted " + ti);
    }
  }

  private void adjustTimersForToken(Token token) {
    ProcessDefinition newDef = token.getProcessInstance().getProcessDefinition();
    List jobs = getJbpmContext().getJobSession().findJobsByToken(token);
    for (Iterator i = jobs.iterator(); i.hasNext();) {
      Job job = (Job) i.next();
      if (job instanceof Timer) {
        // check all timers if connected to a GraphElement
        Timer timer = (Timer) job;
        if (timer.getGraphElement() != null) {
          // and change the reference (take name mappings into account!)
          if (timer.getGraphElement() instanceof Task) {
            // change to new task definition
            Task oldTask = (Task) timer.getGraphElement();
            TaskNode oldNode = oldTask.getTaskNode();
            timer.setGraphElement(findReplacementTask(newDef, oldNode, oldTask));
          }
          else {
            // change to new node
            GraphElement oldNode = timer.getGraphElement();
            // TODO: What about other GraphElements?
            timer.setGraphElement(findReplacementNode(newDef, oldNode));
          }
        }
      }
    }
  }

  private Node findReplacementNode(ProcessDefinition newDef, GraphElement oldNode) {
    String name = getReplacementNodeName(oldNode);
    Node newNode = newDef.findNode(name);
    if (newNode == null) {
      throw new JbpmException("could not find node '" + name + "' in " + newDef);
    }
    return newNode;
  }

  private Task findReplacementTask(ProcessDefinition newDef, Node oldNode, Task oldTask) {
    Node newNode = findReplacementNode(newDef, oldNode);
    if (newNode.getNodeType() != NodeType.Task) {
      throw new JbpmException("expected" + newNode + " to be a task node");
    }

    TaskNode newTaskNode;
    if (newNode instanceof TaskNode) {
      newTaskNode = (TaskNode) newNode;
    }
    else {
      // acquire proxy of the proper type
      newTaskNode = (TaskNode) getJbpmContext().getSession().load(TaskNode.class,
        new Long(newNode.getId()));
    }

    String newTaskName = getReplacementTaskName(oldTask);
    Task newTask = newTaskNode.getTask(newTaskName);

    if (newTask == null) {
      throw new JbpmException("could not find task '" + newTaskName + "' for node '"
        + newTaskNode.getName() + "' in " + newDef);
    }
    return newTask;
  }

  /**
   * @return the name of the replacement node, if one is given in the node name mapping, or the
   * old node name
   */
  private String getReplacementNodeName(GraphElement oldNode) {
    String oldName = oldNode instanceof Node ? ((Node) oldNode).getFullyQualifiedName()
      : oldNode.getName();
    if (nodeNameMapping != null && nodeNameMapping.containsKey(oldName)) {
      return (String) nodeNameMapping.get(oldName);
    }
    // return new node name = old node name as default
    return oldName;
  }

  /**
   * @return the name of the replacement task, if one is given in the task name mapping, or the
   * old task name
   */
  private String getReplacementTaskName(Task oldTask) {
    String oldName = oldTask.getName();
    if (taskNameMapping != null && taskNameMapping.containsKey(oldName)) {
      return (String) taskNameMapping.get(oldName);
    }
    // return new node name = old node name as default
    return oldName;
  }

  /**
   * There may still be open tasks, even though their parent tokens have been ended. So we'll
   * simply get all tasks from this process instance and cancel them if they are still active.
   */
  private List getTasksForToken(Token token) {
    Query query = getJbpmContext().getSession()
      .getNamedQuery("TaskMgmtSession.findTaskInstancesByTokenId");
    query.setLong("tokenId", token.getId());
    return query.list();
  }

  public Map getNodeNameMapping() {
    return nodeNameMapping;
  }

  public void setNodeNameMapping(Map nameMapping) {
    nodeNameMapping = nameMapping;
  }

  public int getNewVersion() {
    return newVersion;
  }

  public void setNewVersion(int newVersion) {
    this.newVersion = newVersion;
  }

  public Map getTaskNameMapping() {
    return taskNameMapping;
  }

  public void setTaskNameMapping(Map nameMapping) {
    taskNameMapping = nameMapping;
  }

  /**
   * @deprecated use getProcessInstanceId instead
   */
  public long getProcessId() {
    if (getProcessInstanceIds() != null && getProcessInstanceIds().length > 0)
      return getProcessInstanceIds()[0];
    else
      return 0;
  }

  /**
   * @deprecated use setProcessInstanceId instead
   */
  public void setProcessId(long processId) {
    super.setProcessInstanceId(processId);
  }

  /**
   * @deprecated use getNodeNameMapping instead
   */
  public Map getNameMapping() {
    return getNodeNameMapping();
  }

  /**
   * @deprecated use setNodeNameMapping instead
   */
  public void setNameMapping(Map nameMapping) {
    setNodeNameMapping(nameMapping);
  }

  // methods for fluent programming

  public ChangeProcessInstanceVersionCommand nodeNameMapping(Map nameMapping) {
    setNodeNameMapping(nameMapping);
    return this;
  }

  public ChangeProcessInstanceVersionCommand newVersion(int newVersion) {
    setNewVersion(newVersion);
    return this;
  }

  public ChangeProcessInstanceVersionCommand taskNameMapping(Map nameMapping) {
    setTaskNameMapping(nameMapping);
    return this;
  }

  public ChangeProcessInstanceVersionCommand nodeNameMappingAdd(String oldNodeName,
    String newNodeName) {
    if (nodeNameMapping == null) nodeNameMapping = new HashMap();
    nodeNameMapping.put(oldNodeName, newNodeName);
    return this;
  }

  public ChangeProcessInstanceVersionCommand taskNameMappingAdd(String oldTaskName,
    String newNodeName) {
    if (taskNameMapping == null) taskNameMapping = new HashMap();
    taskNameMapping.put(oldTaskName, newNodeName);
    return this;
  }
}
