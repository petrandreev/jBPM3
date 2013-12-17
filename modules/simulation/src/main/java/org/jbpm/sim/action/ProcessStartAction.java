package org.jbpm.sim.action;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.def.JbpmSimulationClock;
import org.jbpm.sim.jpdl.SimulationInstance;

/**
 * This action is called after the start of a process instance
 * It's purpose is to record the count of started process instances.
 *
 * @author bernd.ruecker@camunda.com
 */
public class ProcessStartAction extends Action {

  private static final long serialVersionUID = 6997732702890810365L;

  public void execute(ExecutionContext executionContext) throws Exception {
    SimulationInstance simulationInstance = (SimulationInstance)executionContext.getProcessInstance().getInstance(SimulationInstance.class);
    
    // We are probably running inside a unit test, if
    // - there is no model configured or
    // - there is time set (maybe we created a model, but start a process instance by hand
    //   before running simulation)
    // so ignore this action
    if (simulationInstance.getSimulationModel()==null || JbpmSimulationClock.currentTime==null)
      return;
    
    simulationInstance.getSimulationModel().reportProcessStart(//
        executionContext.getProcessDefinition());    
  }

}
