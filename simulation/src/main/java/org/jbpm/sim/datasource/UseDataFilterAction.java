package org.jbpm.sim.datasource;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.jpdl.SimulationDefinition;

/**
 * This action can be added to processes at all places where business
 * figures should be calculated. It calculates the business figure
 * and adds it to the result in the current simulation run.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class UseDataFilterAction extends Action {

  private String name;
  
  private static final long serialVersionUID = 1L;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void execute(ExecutionContext executionContext) throws Exception {
    SimulationDefinition simDef = (SimulationDefinition)executionContext.getDefinition(SimulationDefinition.class);
    ProcessDataFilter dataFilter = simDef.getDataFilter( name );
        
    dataFilter.changeProcessData(executionContext);
  }

}
