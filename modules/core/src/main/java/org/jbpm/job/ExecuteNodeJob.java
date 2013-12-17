package org.jbpm.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

public class ExecuteNodeJob extends Job {

  private static final long serialVersionUID = 1L;

  Node node;

  public ExecuteNodeJob() {
  }

  public ExecuteNodeJob(Token token) {
    super(token);
  }

  public boolean execute(JbpmContext jbpmContext) throws Exception {
    log.debug("job[" + id + "] executes " + node);

    // register process instance for automatic save
    // see https://jira.jboss.org/jira/browse/JBPM-1015
    jbpmContext.addAutoSaveProcessInstance(processInstance);

    // unlock token in case it leaves the node
    token.unlock(this.toString());
    // prepare execution context
    ExecutionContext executionContext = new ExecutionContext(token);
    // then execute the node
    node.execute(executionContext);

    return true;
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }

  private static Log log = LogFactory.getLog(ExecuteNodeJob.class);
}
