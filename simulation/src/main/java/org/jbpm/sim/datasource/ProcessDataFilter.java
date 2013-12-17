package org.jbpm.sim.datasource;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * A VariableModification implementation changes process variables
 * in simulation runs.  
 * 
 * @author bernd.ruecker@camunda.com
 */
public interface ProcessDataFilter {

  public void changeProcessData(ExecutionContext ctx);

  public void reset();
  
}
