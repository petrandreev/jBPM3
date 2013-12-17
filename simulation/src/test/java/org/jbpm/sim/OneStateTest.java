package org.jbpm.sim;

import org.jbpm.sim.def.DefaultJbpmSimulationModel;

import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.SimTime;

public class OneStateTest extends AbstractSimTestCase {

  public void testOneState() {
    Experiment exp = new Experiment(getName(), "target");
    exp.setShowProgressBar(false);

    String processXml =     
      "<process-definition>" +
      "  <start-state name='a'>" +
      "    <transition to='b'/>" +
      "    <simulation avg-duration='5' signal='straight ahead' />" +
      "  </start-state>" +
      "  <state name='b'>" +
      "    <transition to='end'/>" +
      "    <simulation avg-duration='2' signal='turn left here' />" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>";

    // make a new model with the given process
    DefaultJbpmSimulationModel model = new DefaultJbpmSimulationModel(processXml);
    // connect Experiment and Model
    model.connectToExperiment(exp);

    // now set the time this simulation should stop at 
    // let him work 1500 Minutes
    exp.stop(new SimTime(15000));

    // start the Experiment with start time 0.0
    exp.start();

    // --> now the simulation is running until it reaches its ending criteria
    // ...
    // ...
    // <-- after reaching ending criteria, the main thread returns here

    // print the report about the already existing reporters into the report file
    exp.report();

    // stop all threads still alive and close all output files
    exp.finish();    
  }
}
