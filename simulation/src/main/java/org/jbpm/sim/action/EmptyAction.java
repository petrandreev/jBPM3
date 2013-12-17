package org.jbpm.sim.action;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * do nothing here, this action is used for stuff, which is skipped in simulation
 * 
 * @author bernd.ruecker@camunda.com
 */
public class EmptyAction implements ActionHandler {

  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext executionContext) throws Exception {

  }

}
