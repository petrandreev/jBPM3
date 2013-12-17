package org.jbpm.job;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

public class ExecuteActionJob extends Job {

  private static final long serialVersionUID = 1L;

  private Action action;

  public ExecuteActionJob() {
  }

  public ExecuteActionJob(Token token) {
    super(token);
  }

  public boolean execute(JbpmContext jbpmContext) throws Exception {
    Token token = getToken();
    ExecutionContext executionContext = new ExecutionContext(token);
    executionContext.setAction(action);
    executionContext.setEvent(action.getEvent());

    Node node;
    if (token != null && (node = token.getNode()) != null) {
      node.executeAction(action, executionContext);
    }
    else {
      action.execute(executionContext);
    }
    return true;
  }

  public String toString() {
    return "ExecuteActionJob(" + getId() + ',' + action + ')';
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }
}
