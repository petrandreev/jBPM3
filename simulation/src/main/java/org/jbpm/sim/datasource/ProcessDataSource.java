package org.jbpm.sim.datasource;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * A VariableSource implementation generates process variables
 * for simulation runs. The simulation just queries the process variables
 * it needs and create the next one as soon the next process instance
 * is created.
 * 
 * 
 * @author bernd.ruecker@camunda.com
 */
public interface ProcessDataSource {

  /**
   * adds the next generated data of the data source
   * to the process context
   * 
   * @param ctx
   */
  public void addNextData(ExecutionContext ctx);
  
  public void reset();

  /**
   * if false, no more data can be queried from this data source
   * 
   * TODO: It should be possible, that the simulation ends 
   *       when the data source stops sending data 
   */
  public boolean hasNext();
  
}
