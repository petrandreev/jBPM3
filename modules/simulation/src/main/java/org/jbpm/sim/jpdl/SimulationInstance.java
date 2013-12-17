package org.jbpm.sim.jpdl;

import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.sim.def.JbpmSimulationModel;

/**
 * each ProcessInstance is connected to exactly one SimulationInstance
 * which makes the connection to the simulation
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SimulationInstance extends ModuleInstance {
  
  private static final long serialVersionUID = 1L;

  SimulationDefinition simulationDefinition;
  JbpmSimulationModel simulationModel = null;

  public SimulationInstance(SimulationDefinition simulationDefinition) {
    this.simulationDefinition = simulationDefinition;
  }
  
  public JbpmSimulationModel getSimulationModel() {
    return simulationModel;
  }
  public void setExperiment(JbpmSimulationModel simulationModel) {
    this.simulationModel = simulationModel;
  }
}