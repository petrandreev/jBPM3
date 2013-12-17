package org.jbpm.sim.jpdl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.Decision;
import org.jbpm.sim.def.JbpmSimulationModel;

public class SimDecision extends Decision {

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(SimDecision.class);

  public void execute(ExecutionContext executionContext) {
    SimulationInstance simulationInstance = (SimulationInstance) executionContext.getProcessInstance()
      .getInstance(SimulationInstance.class);
    JbpmSimulationModel simulationModel = simulationInstance.getSimulationModel();

    // if we have probabilities configured for the leaving transitions
    // don't execute the decision, but let the simulation framework decide
    if (simulationModel.hasLeavingTransitionProbabilitiesConfigured(this)) {
      Transition transition = simulationModel.getLeavingTransition(this);

      log.debug(executionContext.getToken() + " leaves " + this + " over " + transition);
      executionContext.leaveNode(transition);
    }
    else {
      // if no probabilities are configured, behave "normal"
      super.execute(executionContext);
    }
  }

}
