package org.jbpm.sim;

import java.util.Iterator;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.sim.def.DefaultJbpmSimulationModel;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.SimTime;
import junit.framework.TestCase;

public class OneTaskTest extends TestCase {

  public void testOneState() {
    Experiment exp = new Experiment("Test");
    
    String processXml =     
      "<process-definition>" +
      
      "  <swimlane name='tester' pool-size='1' />" +

      "  <start-state name='start'>" +
      "    <transition to='test' />" +
      "  </start-state>" +
      "  <task-node name='test'>" +
      "    <task swimlane='tester' />" +
      "    <transition to='end' />" +
      "  </task-node>" +
      "  <end-state name='end'/>" +

      "</process-definition>" ;

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
  
  public TaskInstance findTaskInstance(String taskName, ProcessInstance processInstance) {
    TaskMgmtInstance tmi = processInstance.getTaskMgmtInstance();
    Iterator iter = tmi.getTaskInstances().iterator();
    while (iter.hasNext()) {
      TaskInstance taskInstance = (TaskInstance) iter.next();
      if ( (taskName.equals(taskInstance.getName())
           && (!taskInstance.hasEnded())
           && (taskInstance.getStart()!=null))
         ) {
        return taskInstance;
      }
    }
    return null;
  }
  
  
  // old test:
//  setCurrentTime("10:29");
//  pi1.signal();
//
//  setCurrentTime("10:35");
//  exp.start();
//  ProcessInstance pi2 = new ProcessInstance(processDefinition);
//  simulationInstance = (SimulationInstance)pi2.getInstance(SimulationInstance.class);
//  simulationInstance.setExperiment(simulationModel);
//
//  setCurrentTime("10:46");
//  pi2.signal();
//  
//  assertNull(findTaskInstance("test", pi2));
//
//  setCurrentTime("10:55");
//  TaskInstance taskInstance = findTaskInstance("test", pi1);
//  taskInstance.end();
//  
//  produceReports(pi1);
}
