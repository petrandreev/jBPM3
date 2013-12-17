package org.jbpm.sim.exe;

/**
 * Interface to observe the simulation run, maybe for showing
 * some progress bar or something similar.
 * 
 * @author bernd.ruecker@camunda.com
 */
public interface SimulationProgressListener {

  public void newScenario(String scenarioName, int scenarioNumber, int scenarioCount);
  
  public void progressChanged(double currentScenarioPercentage);
  
  public void experimentFinished();
}
