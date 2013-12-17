package org.jbpm.sim.action;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.entity.TaskInstanceEntity;
import org.jbpm.sim.jpdl.SimulationInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * The StartTaskAndPlanCompletion Action is added by the simulation to all TaskInstances
 * as task-create event
 * 
 * It starts the simulation stuff (acquiring resources, maybe queue up, schedule end of task, ...)
 * 
 * @author bernd.ruecker@camunda.com
 */
public class StartTaskAndPlanCompletion extends Action {

  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext executionContext) throws Exception {   
    SimulationInstance simulationInstance = (SimulationInstance)executionContext.getProcessInstance().getInstance(SimulationInstance.class);
    TaskInstance taskInstance = executionContext.getTaskInstance();

    TaskInstanceEntity entity = new TaskInstanceEntity( simulationInstance.getSimulationModel().getModel(), taskInstance );
    entity.acquireResourcesAndStart();
  }

}
