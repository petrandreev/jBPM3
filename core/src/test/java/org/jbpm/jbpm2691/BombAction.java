package org.jbpm.jbpm2691;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class BombAction implements ActionHandler {

  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext executionContext) throws Exception {
    throw new Exception("boom");
  }

}
