package org.jbpm.enterprise.jbpm1903;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class ENCAction implements ActionHandler {

  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext executionContext) throws Exception {
    Context jndiContext = new InitialContext();
    try {
      Object queue = jndiContext.lookup("java:comp/env/jms/JobQueue");
      executionContext.setVariable("queue", queue);
    }
    finally {
      jndiContext.close();
    }
    executionContext.leaveNode();
  }

}
