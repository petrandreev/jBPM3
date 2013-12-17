package org.jbpm.jpdl.par;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class InstantiateAction implements ActionHandler {

  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext executionContext) throws Exception {
    new InstantiateClass();    
  }

}
