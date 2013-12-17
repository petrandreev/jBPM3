package org.jbpm.sim.event;

import org.jbpm.sim.def.JbpmSimulationModel;

import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;

/**
 * This event resets all statistical counters, can be schedules at 
 * some point in time, for example when the steady state starts
 * 
 * @author bernd.ruecker@camunda.com
 */
public class StatisticsResetEvent extends ExternalEvent {

  private JbpmSimulationModel model;

  public StatisticsResetEvent(Model owner) {
    super(owner, "reset statistical counters", true);
    this.model = (JbpmSimulationModel) owner;
  }

  /**
   * This eventRoutine describes what an object of this class
   * will do, when it becomes activated by DESMO-J
   */
  public void eventRoutine() {
    model.reset();
  }
}
