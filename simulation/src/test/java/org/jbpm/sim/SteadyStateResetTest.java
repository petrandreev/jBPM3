package org.jbpm.sim;

import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.exe.ExperimentReader;
import org.jbpm.sim.report.ScenarioReport;

/**
 * This test tests if the steady state reset property of an experiment 
 * works correctly. It's purpose is to reset all statistical counters
 * at one special point in model time, which is seen as the start of the
 * steady state.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SteadyStateResetTest extends AbstractSimTestCase {

  private static final String testProcessXml =     
    "<process-definition name='test'>" +

    "  <start-state name='start'>" +
    "    <transition to='end' />" +
    "  </start-state>" +    
    "  <end-state name='end'/>" +

    "</process-definition>" ;

  private static final String experimentConfiguration = 
    "<experiment name='MySimulationExperiment'" + 
    "          run-time='100'" + 
    "          reset-time='50' " +
    "          real-start-time='01.01.1970 01:00:00:002'" +      
    "          time-unit='minute'>" + 
    "  <scenario name='test'>" +  
    
    "    <distribution name='start' sample-type='real' type='constant' value='10' /> " +
    "    <sim-process name='test'>" +
    "      <process-overwrite start-distribution='start' />" + 
    "    </sim-process>" + 

    "  </scenario>" +
    
    "  <output path='target' />" + // currently just used as parameter for DESMO-J    
    
    "</experiment>";

  public void testExperimentReader() {
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    reader.addProcessDefinition("test", testProcessXml );
    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.run();
    
    ScenarioReport report = experiment.getSimulationReportForScenario("test");
    assertNotNull(report);

    assertEquals(100.0, report.getSimulationRunTime(), 0.0001);    
    assertEquals(50.0, report.getResetTime(), 0.0001);    
    assertEquals(5, report.getCycleTimesValueStatistics("test").getNumberOfObservations());
    
    assertEquals(5, report.getProcessStartCount("test").getCount());
    assertEquals(5, report.getProcessEndCount("end").getCount());
  }
}
