package org.jbpm.sim.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.State;
import org.jbpm.sim.entity.TaskInstanceEntity;
import org.jbpm.sim.entity.TokenEntity;
import org.jbpm.sim.jpdl.SimulationInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * The StartTaskAndPlanCompletion Action is added by the simulation to all Nodes
 * as node-enter event
 * 
 * It applies only on states and starts the simulation stuff for them
 * (acquiring resources, maybe queue up, schedule end of state, ...)
 * 
 * @author bernd.ruecker@camunda.com
 */
public class StartWorkOnStateAndPlanCompletion extends Action {

  private static final long serialVersionUID = 1L;
  private static Log log = LogFactory.getLog(StartWorkOnStateAndPlanCompletion.class);

  public void execute(ExecutionContext executionContext) throws Exception {
    // applies only on states!
    if (!State.class.isAssignableFrom( executionContext.getNode().getClass() ))
      return;

    SimulationInstance simulationInstance = (SimulationInstance)executionContext.getProcessInstance().getInstance(SimulationInstance.class);
    Token token = executionContext.getToken();

    TokenEntity entity = new TokenEntity( simulationInstance.getSimulationModel().getModel(), token );
    entity.acquireResourcesAndStart();
  }

}
