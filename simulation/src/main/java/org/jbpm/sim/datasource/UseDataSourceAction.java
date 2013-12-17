package org.jbpm.sim.datasource;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.jpdl.SimulationDefinition;
import org.jbpm.sim.jpdl.SimulationInstance;

/**
 * This action can be added to processes at all places where business
 * figures should be calculated. It calculates the business figure
 * and adds it to the result in the current simulation run.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class UseDataSourceAction extends Action {

  private String name;
  
  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext executionContext) throws Exception {
    SimulationDefinition simDef = (SimulationDefinition)executionContext.getDefinition(SimulationDefinition.class);
    ProcessDataSource dataSource = simDef.getDataSource( name );
        
    dataSource.addNextData(executionContext);

    if (!dataSource.hasNext()) {
      // if the data source is exhausted end the simulation
      // because this thread will finish its execution, check it
      // after we got a result. This is only a problem if a data source
      // contains absolutely no data, which should not be the case
      
      SimulationInstance instance = (SimulationInstance) executionContext.getProcessInstance().getInstance(SimulationInstance.class);
      instance.getSimulationModel().getExperiment().stop();
    }

  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
