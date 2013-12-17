package org.jbpm.sim.tutorial.business;

import org.jbpm.sim.exe.JbpmSimulationExperimentRunner;

public class UC1ExperimentRunner {

  public static void main(String[] args) {
    String experimentConf = "/org/jbpm/sim/tutorial/business/simulationExperiment.xml";        
    new JbpmSimulationExperimentRunner().run(experimentConf);
  }
}
