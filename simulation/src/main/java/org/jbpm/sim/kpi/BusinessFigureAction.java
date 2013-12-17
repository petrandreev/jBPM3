package org.jbpm.sim.kpi;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.def.JbpmSimulationModel;
import org.jbpm.sim.jpdl.SimulationInstance;

/**
 * This action can be added to processes at all places where business
 * figures should be calculated. It calculates the business figure
 * and adds it to the result in the current simulation run.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class BusinessFigureAction extends Action {

  private String name;
  
  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext executionContext) throws Exception {
    SimulationInstance simulationInstance = (SimulationInstance)executionContext.getProcessInstance().getInstance(SimulationInstance.class);
    JbpmSimulationModel simModel = simulationInstance.getSimulationModel();

    BusinessFigure conf = simModel.getBusinessFigure(name);
    conf.calculateAndAdd(executionContext);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
