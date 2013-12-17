package org.jbpm.sim.action;

import java.util.Date;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.EndState;
import org.jbpm.sim.def.JbpmSimulationClock;
import org.jbpm.sim.jpdl.SimulationInstance;

/**
 * This action is called before the end of a process instance
 * It's purpose is to record the process cycle time of the instance
 * to the simulation model.
 * 
 * Because we only deal with simulation time, and not real run time here,
 * it doesn't matter if there are slowly computing actions afterwards or not,
 * so we don't have to think about any order of actions here.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ProcessEndAction extends Action {

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
    
    Date dateStart = executionContext.getProcessInstance().getStart();
    Date dateEnd = JbpmSimulationClock.currentTime;
    
    long duration = dateEnd.getTime() - dateStart.getTime(); 
    
    simulationInstance.getSimulationModel().reportProcessInstanceCycleTime(//
        executionContext.getProcessDefinition(),
        JbpmSimulationClock.getAsDouble(duration));

    simulationInstance.getSimulationModel().reportProcessEndState(//
        (EndState)executionContext.getToken().getNode());
    
    simulationInstance.getSimulationModel().reportFinishedProcessInstance( executionContext.getProcessInstance() );
  }

}
