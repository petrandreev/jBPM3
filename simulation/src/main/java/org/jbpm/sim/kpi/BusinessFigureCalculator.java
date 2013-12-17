package org.jbpm.sim.kpi;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * Interface to calculate some business figures of a process.
 * Implement it to do you what you need!
 * 
 * @author bernd.ruecker@camunda.com
 */
public interface BusinessFigureCalculator {

  /**
   * This method has to return the calculated figure and can use whatever
   * it wants from the execution context
   * 
   * @param executionContext
   * @return the calculated business figure
   */
  public Number calculate(ExecutionContext executionContext); 
  
}
