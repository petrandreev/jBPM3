package org.jbpm.sim.jpdl;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * If an ActionHandler implements this interface, this method is executed instead
 * of the ActionHandler.execute method.
 * 
 * <b>Please note: Even if a action is not configured with 'simulation=skip',
 *  the simExecute method is executed, if your ActionHandler implements the 
 *  this interface.</b>
 * 
 * @author bernd.ruecker@camunda.com
 */
public interface SimulationHandler {

  public void simExecute(ExecutionContext executionContext);
  
}
