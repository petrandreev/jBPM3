package org.jbpm.sim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.sim.def.DefaultJbpmSimulationModel;
import org.jbpm.sim.jpdl.SimulationInstance;
import org.jbpm.sim.jpdl.SimulationJpdlXmlReader;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.SimTime;

public class LanguageDefinitionTest extends AbstractSimTestCase {

  public void testDistributionsOnProcessElements() {
    Experiment exp = new Experiment(getName(), "target");
    exp.setShowProgressBar(false);

    // distributions: type=real constant|empirical|erlang|exponential|normal|uniform
    // distributions: type=int constant|empirical|poison|uniform
    // distributions: type=boolean constant|bernoulli       true-probability='0.3' value='true' 
    // min='5' max='10' mean='7' value='10' 
    // <type='empirical>
    //    <sample value='10' frequency='0.3' />

    String processXml =     
      "<process-definition name='test' start-distribution='start new process instances of test'>" +

      "  <distribution name='start new process instances of test' sample-type='real' type='constant' value='20' /> " +
      "  <distribution name='time required for task one'          sample-type='real' type='normal'   mean='25' standardDeviation='10' /> " +      
      "  <distribution name='time required for task two'          sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      
      "  <distribution name='time required for automated state'   sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      

      "  <resource-pool name='tester'      pool-size='2' />" +      
      "  <resource-pool name='big machine' pool-size='3' />" +      

      // TODO: think about shifts for resource pools and implement
//      "  <resource-pool name='big machine'>" + 
//      "    <shift from='xx' till='xx' pool-size='3' />" + 
//      "  </resource-pool>" + 
      "  <swimlane name='tester' pool-size='1' />" +

      "  <start-state name='start'>" +
      "    <transition to='task one' />" +
      "  </start-state>" +
      
      "  <task-node name='task one'>" +
      "    <task swimlane='tester' time-distribution='time required for task one' />" +
      "    <transition to='task two' />" +
      "  </task-node>" +

      "  <task-node name='task two'>" +
      "    <task swimlane='tester' time-distribution='time required for task two' />" +
      "    <transition to='automated state' />" +
      "  </task-node>" +

      "  <state name='automated state' time-distribution='time required for automated state'>" +
      "    <resource-needed pool='big machine' amount='2' />" +
      "    <transition to='end' />" +
      "  </state>" +

      "  <end-state name='end'/>" +

      "</process-definition>" ;

    // make a new model with the given process
    DefaultJbpmSimulationModel model = new DefaultJbpmSimulationModel(processXml);
    // connect Experiment and Model
    model.connectToExperiment(exp);

    // now set the time this simulation should stop at 
//    exp.stop(new SimTime(7500));
    exp.stop(new SimTime(10));
    
    //exp.setShowProgressBar(false);
    
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
  
  /**
   * static list to remember executed actions.
   * Not the nice way, but I don't get a better idea at the moment
   */
  public static List executedActions;
  
  public static class TestAction implements ActionHandler {
    private boolean shouldBeExecuted;
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      if (!shouldBeExecuted)
        throw new RuntimeException("action '"+executionContext.getAction().getName()+"' should not be executed in a simulation run.");
      executedActions.add(executionContext.getAction());
    }    
  }
  
  public static class SimTestAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      executedActions.add(executionContext.getAction());
    }    
  }  
  
  /**
   * tests the action configuration for the simulation. Valid values:
   * <action name="xy" class="x.y" simulation=execute|skip />
   * <action name="xy" class="x.y" simulation-class="sim.x' />
   * <simulation-action name="xy" class="sim.x" />  
   */
  public void testAction() {
    String processXml =     
      "<process-definition name='test'>" +

      "  <start-state name='start'>" +
      "    <transition to='node one' />" +
      "  </start-state>" +
      
      "  <node name='node one'>" +      
      "   <transition to='node two'>" +
      
      "    <action name ='action 1' class='org.jbpm.sim.LanguageDefinitionTest$TestAction'>" +
      "      <shouldBeExecuted>false</shouldBeExecuted>" +
      "    </action>" +
      "    <action name ='action 2' class='org.jbpm.sim.LanguageDefinitionTest$TestAction' simulation='skip'>" +
      "      <shouldBeExecuted>false</shouldBeExecuted>" +
      "    </action>" +
      "    <action name ='action 3' class='org.jbpm.sim.LanguageDefinitionTest$TestAction' simulation='execute'>" +
      "      <shouldBeExecuted>true</shouldBeExecuted>" +
      "    </action>" +
      
      "   </transition>" +
      "  </node>" +
      
      "  <node name='node two'>" +
      "   <transition to='end'>" +
      
      "    <action name ='sim-action 1' class='org.jbpm.sim.LanguageDefinitionTest$TestAction' simulation-class='org.jbpm.sim.LanguageDefinitionTest$SimTestAction'>" +
      "      <shouldBeExecuted>false</shouldBeExecuted>" +
      "    </action>" +
      "    <simulation-action name ='sim-action 2' class='org.jbpm.sim.LanguageDefinitionTest$SimTestAction'>" +
      "    </simulation-action>" +

      "   </transition>" +
      "  </node>" +

      "  <end-state name='end'/>" +

      "</process-definition>" ;
    
    SimulationJpdlXmlReader reader = new SimulationJpdlXmlReader(processXml);
    ProcessDefinition pd = reader.readProcessDefinition();
    
    // remember executed actions
    executedActions = new ArrayList();
    
    ProcessInstance processInstance = pd.createProcessInstance();
    processInstance.signal();
    
    // now check, if the right actions have been executed
    assertTrue(processInstance.hasEnded());
    assertEquals(3, executedActions.size());
    
    assertTrue(executedActions.contains( pd.getAction("action 3") ));
    assertTrue(executedActions.contains( pd.getAction("sim-action 1") ));
    assertTrue(executedActions.contains( pd.getAction("sim-action 2") ));
  }  
  
  public void testScript() {
    String processXml =     
      "<process-definition name='test'>" +

      "  <start-state name='start'>" +
      "    <transition to='node one' />" +
      "  </start-state>" +
      
      "  <node name='node one'>" +      
      "   <transition to='node two'>" +
      
      "    <script name ='not executed in sim 1'>" +
      "       <expression>executed.add(\"script 1\");</expression>" +
      "    </script>" +
      "    <script name ='not executed in sim 2' simulation='skip'>" +
      "       <expression>executed.add(\"script 2\");</expression>" +
      "    </script>" +
      "    <script name ='executed in sim 2' simulation='execute'>" +
      "       <expression>executed.add(\"script 3\");</expression>" +
      "    </script>" +
      
      "   </transition>" +
      "  </node>" +
      
      "  <node name='node two'>" +
      "   <transition to='end'>" +

      "    <script name ='simulation script 1'>" +
      "       <expression>executed.add(\"script 4\");</expression>" +
      "       <simulation-expression>executed.add(\"sim-script 1\");</simulation-expression>" +
      "    </script>" +
      "    <simulation-script name ='simulation script 2'>" +
      "       <expression>executed.add(\"sim-script 2\");</expression>" +
      "    </simulation-script>" +

      "   </transition>" +
      "  </node>" +

      "  <end-state name='end'/>" +

      "</process-definition>" ;
    
    SimulationJpdlXmlReader reader = new SimulationJpdlXmlReader(processXml);
    ProcessDefinition pd = reader.readProcessDefinition();
    
    ProcessInstance processInstance = pd.createProcessInstance();
    processInstance.getContextInstance().setVariable("executed", new ArrayList());
    processInstance.signal();
    
    // now check, if the right actions have been executed
    ArrayList executedActions = (ArrayList) processInstance.getContextInstance().getVariable("executed");
    assertTrue(processInstance.hasEnded());
    
    assertTrue(executedActions.contains( "script 3") );
    assertTrue(executedActions.contains( "sim-script 2") );
    assertTrue(executedActions.contains( "sim-script 1") );
    assertEquals(3, executedActions.size());
  }    
  
  public void testLeavingTransitionProbabilities() {
    String processXml =     
      "<process-definition name='test'>" +
      "  <distribution name='no work'    sample-type='real' type='constant' value='1' /> " +

      "  <start-state name='start'>" +
      "    <transition to='node one' />" +
      "  </start-state>" +
      
      "  <task-node name='node one' >" +
      "    <task name='task one' time-distribution='no work' />" +
      "    <transition name='t2' to='end'      probability='0.0'/>" +
      "    <transition name='t1' to='node two' probability='1.0'/>" +
      "  </task-node>" +

      "  <state name='node two' time-distribution='no work'>" +
      "    <transition name='t3' to='end' probability='1.0'/>" +
      "    <transition name='t4' to='end' probability='1.0'/>" +
      "    <transition name='t5' to='end' probability='1.0'/>" +
      "  </state>" +

      "  <end-state name='end'/>" +

      "</process-definition>" ;

    Experiment exp = new Experiment(getName(), "target");
    exp.setShowProgressBar(false);

    // make a new model with the given process
    DefaultJbpmSimulationModel model = new DefaultJbpmSimulationModel(processXml);
    // connect Experiment and Model
    model.connectToExperiment(exp);

    // start a process instance, which generates events
    ProcessInstance processInstance = new ProcessInstance(model.getProcessDefinitions()[0]);
    SimulationInstance simulationInstance = (SimulationInstance)processInstance.getInstance(SimulationInstance.class);
    simulationInstance.setExperiment(model);
    processInstance.signal();

    exp.stop(new SimTime(3));
    exp.setShowProgressBar(false);    
    exp.start();
    exp.report();
    exp.finish();
    
    // now check which transitions were taken
    // TODO: implement check which transitions were taken
  }  
  
  public void testDecisionProbability() {
    String processXml =     
      "<process-definition name='test'>" +
      "  <distribution name='no work'    sample-type='real' type='constant' value='1' /> " +

      "  <start-state name='start'>" +
      "    <transition to='unaffected decision' />" +
      "  </start-state>" +      

      "  <decision name='unaffected decision'  expression='valid'>" +
      "    <transition name='invalid' to='end invalid' />" +
      "    <transition name='valid'   to='simulated decision' />" +
      "  </decision>" +
      
      "  <decision name='simulated decision' expression='invalid'>" +
      "    <transition name='first'    to='end one'     probability='0.2'/>" +
      "    <transition name='second'   to='end two'     probability='0.4'/>" +
      "    <transition name='third'    to='end three'   probability='0.4'/>" +
      "    <transition name='invalid'  to='end invalid' probability='0.0'/>" +
      "  </decision>" +

      "  <end-state name='end one'/>" +
      "  <end-state name='end two'/>" +
      "  <end-state name='end three'/>" +
      "  <end-state name='end invalid'/>" +

      "</process-definition>" ;

    Experiment exp = new Experiment(getName(), "target");
    exp.setShowProgressBar(false);

    DefaultJbpmSimulationModel model = new DefaultJbpmSimulationModel(processXml);
    model.connectToExperiment(exp);

    // start a process instance, which generates events
    ProcessInstance processInstance = new ProcessInstance(model.getProcessDefinitions()[0]);
    SimulationInstance simulationInstance = (SimulationInstance)processInstance.getInstance(SimulationInstance.class);
    simulationInstance.setExperiment(model);
    processInstance.signal();

    exp.stop(new SimTime(3));
    exp.setShowProgressBar(false);    
    exp.start();
    exp.report();
    exp.finish();
    
    assertTrue(processInstance.hasEnded());
    assertNotSame("end invalid", processInstance.getRootToken().getNode().getName());
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
