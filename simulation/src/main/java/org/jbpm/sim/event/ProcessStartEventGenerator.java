package org.jbpm.sim.event;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.sim.def.JbpmSimulationModel;
import org.jbpm.sim.jpdl.SimulationInstance;

import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimTime;

public class ProcessStartEventGenerator extends ExternalEvent {

  private JbpmSimulationModel model;
  
  private ProcessDefinition processDefinition;  
  
	public ProcessStartEventGenerator(Model owner, ProcessDefinition processDefinition) {
		super(owner, "start events for " + processDefinition.getName(), true);
		this.model = (JbpmSimulationModel) owner;
		this.processDefinition = processDefinition;
	}
	
	/**
	 * This eventRoutine describes what an object of this class
	 * will do, when it becomes activated by DESMO-J
	 */
	public void eventRoutine() {	  
	  ProcessInstance processInstance = new ProcessInstance(processDefinition);
	  SimulationInstance simulationInstance = (SimulationInstance)processInstance.getInstance(SimulationInstance.class);
	  simulationInstance.setExperiment(model);

	  // this triggers the start of the process, which should be enough for the process,
	  // new events will be created on the "way" of the process

	  // TODO: signal is only necessary, if there is no "initial-state"!
	  processInstance.signal(); 

	  // schedule the next process start event
	  // in the time given from the according distribution
	  SimTime processStartTime = model.getProcessStartTime(processDefinition);
	  this.schedule(processStartTime);

	  // TODO: Keep track of running processes
	  // model.incRunningProcessCount( 1 );	  
	}
}
