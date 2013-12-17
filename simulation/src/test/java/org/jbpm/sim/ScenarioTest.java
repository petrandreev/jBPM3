package org.jbpm.sim;

import java.util.Calendar;

import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.def.JbpmSimulationScenario;
import org.jbpm.sim.exe.ExperimentReader;
import org.jbpm.sim.report.ScenarioReport;
import org.jbpm.sim.report.dto.ValueStatisticResult;

public class ScenarioTest extends AbstractSimTestCase {
  
  private static final String testProcessXml =     
    "<process-definition name='test' start-distribution='start new process instances of test'>" +

    "  <distribution name='start new process instances of test' sample-type='real' type='constant' value='20' /> " +
    "  <distribution name='time required for task one'          sample-type='real' type='normal'   mean='25' standardDeviation='10' /> " +      
    "  <distribution name='time required for task two'          sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      
    "  <distribution name='time required for automated state'   sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      

    "  <resource-pool name='tester'      pool-size='2' />" +      
    "  <resource-pool name='big machine' pool-size='3' />" +      

    // TODO: think about shifts for resource pools and implement
//    "  <resource-pool name='big machine'>" + 
//    "    <shift from='xx' till='xx' pool-size='3' />" + 
//    "  </resource-pool>" + 
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

  public void testExperimentReader() {
    String experimentConfiguration = 
      "<experiment name='MySimulationExperiment'" + 
      "          run-time='100'" + 
      "          real-start-time='04.10.1982 14:30:10:500'" +  
      "          time-unit='minute'>" + 
      "  <scenario name='NormalStaffing'>" +  
      
      // This specification overwrites any distribution specification in the processdefinition.xml
      "    <distribution name='start new process instances of test' sample-type='real' type='constant' value='20' /> " +
      "    <distribution name='time required for task one'          sample-type='real' type='normal'   mean='25' standardDeviation='10' /> " +      
      "    <distribution name='time required for task two'          sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      
      "    <distribution name='time required for automated state'   sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      

      // This specification overwrites any resource-pool specification in the processdefinition.xml
      "    <resource-pool name='tester'      pool-size='2' />" +      
      "    <resource-pool name='big machine' pool-size='3' />" +      

      "    <sim-process name='test'>" +
      // TODO: Think about overwriting special stuff of process simulation and implement
//      "      <task-overwrite task-name='task one' time-distribution='time required for task one' />" + 
//      "      <process-overwrite start-distribution='start new process instances of test' />" + 
      "    </sim-process>" + 

      "  </scenario>" +

      // maybe more <scenario ... />'s

      // TODO: Think more about output options
      "  <output path='target' />" + // currently just used as parameter for DESMO-J

      "</experiment>";
    
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    reader.addProcessDefinition("test", testProcessXml );
    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    
    assertNotNull(experiment);
    assertEquals("MySimulationExperiment", experiment.getName());     
    assertEquals(100.0, experiment.getSimulationRunTime(), 0.001);
    Calendar calendar = Calendar.getInstance();
    calendar.set(1982, Calendar.OCTOBER, 4, 14, 30, 10);
    calendar.set(Calendar.MILLISECOND, 500);
    assertEquals(calendar.getTime(), experiment.getRealStartDate());
    assertEquals(JbpmSimulationExperiment.MINUTES, experiment.getTimeUnit());
    assertEquals("target", experiment.getOutputPathName());
    
    assertEquals(1, experiment.getScenarios().size());
    JbpmSimulationScenario scenario = experiment.getScenario("NormalStaffing");
    assertNotNull(scenario);

    assertEquals("NormalStaffing", scenario.getName());    
  }
  
  public void testExperimentRun() {
    String experimentConfiguration = 
      "<experiment name='MySimulationExperiment'" + 
      "          run-time='100'" + 
      "          real-start-time='30.03.1980 00:00:00:000'" +  
      "          time-unit='minute'>" + 
      "  <scenario name='NormalStaffing'>" +  
      
      // This specification overwrites any distribution specification in the processdefinition.xml
      "    <distribution name='start new process instances of test' sample-type='real' type='constant' value='20' /> " +
      "    <distribution name='time required for task one'          sample-type='real' type='normal'   mean='25' standardDeviation='10' /> " +      
      "    <distribution name='time required for task two'          sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      
      "    <distribution name='time required for automated state'   sample-type='real' type='normal'   mean='6'  standardDeviation='1'  /> " +      

      // This specification overwrites any resource-pool specification in the processdefinition.xml
      "    <resource-pool name='tester'      pool-size='2' />" +      
      "    <resource-pool name='big machine' pool-size='3' />" +      

      "    <sim-process name='test'>" +
      // TODO: Think about overwriting special stuff of process simulation and implement
//      "      <task-overwrite task-name='task one' time-distribution='time required for task one' />" + 
//      "      <process-overwrite start-distribution='start new process instances of test' />" + 
      "    </sim-process>" + 

      "  </scenario>" +

      // maybe more <scenario ... />'s

      // TODO: Think more about output options
      "  <output path='target' />" + // currently just used as parameter for DESMO-J

      "</experiment>";      
    
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    reader.addProcessDefinition("test", testProcessXml );
    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.run();
    
    // TODO: check results and write asserts
  }  
  
  public void testTwoScenariosExperimentRun() {
    String experimentConfiguration = 
      "<experiment name='MySimulationExperiment'>" +  
      "  <scenario name='Staffing1'>" +  
      "    <resource-pool name='tester' pool-size='1' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='Staffing2'>" +  
      "    <resource-pool name='tester' pool-size='5' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <output path='target' />" + // currently just used as parameter for DESMO-J
      "</experiment>";      
    
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    reader.addProcessDefinition( "test", testProcessXml );
    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.run();
    
    ScenarioReport report1 = experiment.getSimulationReportForScenario("Staffing1");
    assertNotNull(report1);
    assertEquals("Staffing1", report1.getScenarioName());
    assertEquals(1440.0, report1.getSimulationRunTime(), 0.001);
    assertEquals(3, report1.getStateWaitingTimes().size());
    assertEquals(1, report1.getCycleTimesValueStatistics().size());
    assertEquals(2, report1.getResourcePoolWaitingTimes().size());
    {
      ValueStatisticResult vsr = report1.getStateWaitingTimes("task one");
      assertEquals("task one", vsr.getName());
//      assertEquals(71, vsr.getNumberOfObservations());
    }
    
    ScenarioReport report2 = experiment.getSimulationReportForScenario("Staffing2");
    assertNotNull(report2);
    assertEquals("Staffing2", report2.getScenarioName());
    assertEquals(1440.0, report2.getSimulationRunTime(), 0.001);
    assertEquals(3, report2.getStateWaitingTimes().size());
    assertEquals(1, report1.getCycleTimesValueStatistics().size());
    assertEquals(2, report2.getResourcePoolWaitingTimes().size());
  }    
  
  /**
   * This test case shows how to identify the appropriate staffing strategy
   */
  public void testIdentifyResourceCount() {
    String processXml =     
      "<process-definition name='test' start-distribution='start dist'>" +

      "  <distribution name='start dist' sample-type='real' type='normal' mean='10' standardDeviation='5'/> " +
      "  <distribution name='task dist'  sample-type='real' type='normal' mean='20' standardDeviation='5'/> " +

      "  <swimlane name='clerk' pool-size='10' />" +

      "  <start-state name='start'>" +
      "    <transition to='task one' />" +
      "  </start-state>" +
      
      "  <task-node name='task one'>" +
      "    <task swimlane='clerk' time-distribution='task dist' />" +
      "    <transition to='end' />" +
      "  </task-node>" +
      
      "  <end-state name='end'/>" +

      "</process-definition>" ;    
    
    String experimentConfiguration = 
      "<experiment name='StaffingExperiment'>" +  
      "  <scenario name='Only one poor guy'>" +  
      "    <resource-pool name='clerk' pool-size='1' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='Two people'>" +  
      "    <resource-pool name='clerk' pool-size='2' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='Three people'>" +  
      "    <resource-pool name='clerk' pool-size='3' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='Five people'>" +  
      "    <resource-pool name='clerk' pool-size='5' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='A real crowd'>" +  
      "    <resource-pool name='clerk' pool-size='10' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <output path='target' />" + // currently just used as parameter for DESMO-J
      "</experiment>";   
    
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    reader.addProcessDefinition( "test", processXml );    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setWriteDesmojHtmlOutput(true);
    experiment.run();
    
    /*
     * Here are the results:
     * 
     * ***Waiting time before Task***
     * People   Mean    StandardDev    Min     Max
     * One      373             230      0     764
     * Two       70              42      0     137
     * Three      1.4             3.4    0      19
     * Five       0               0      0       1.7   
     * Ten        0               0      0       0
     * 
     * 
     * ***Cycle Times***
     * People   Mean    StandardDev    Min    Max
     * One      388             227     34    776
     * Two       90              41     14    168
     * Three     22               8      0     44
     * Five      20               7      1     39
     * Ten       20               7      1     39
     * 
     * 
     * ***Queue***
     * People   Length Avg   Waiting Avg.
     * One              40           378
     * Two               7            72
     * Three             0.14          6
     * Five              0             1.8
     * Ten               0             0
     * 
     * So: What goals do we have to compare scenarios:
     * 1.) How much resources would be "good"?
     * 2.) If we pump in X orders now, when are we finished?
     */
    
    // now lets check the results
    ScenarioReport report1 = experiment.getSimulationReportForScenario("Only one poor guy");
    assertNotNull(report1);
    assertEquals("Only one poor guy", report1.getScenarioName());
    assertEquals(1440.0, report1.getSimulationRunTime(), 0.0001);
    
    assertEquals(373.40751, report1.getStateWaitingTimes("task one").getMean(), 0.0001);
    assertEquals(388.50746, report1.getCycleTimesValueStatistics("test").getMean(), 0.0001);
    assertEquals( 40.34285, report1.getResourcePoolWaitingTimes("clerk").getAverageLength(), 0.0001);
    assertEquals(378.98076, report1.getResourcePoolWaitingTimes("clerk").getAverageWaitTime(), 0.0001);
    
    ScenarioReport report2 = experiment.getSimulationReportForScenario("Two people");
    assertNotNull(report2);
    assertEquals("Two people", report2.getScenarioName());
    assertEquals(1440.0, report2.getSimulationRunTime(), 0.0001);    
    assertEquals(70.16351, report2.getStateWaitingTimes("task one").getMean(), 0.0001);
    assertEquals(90.61314, report2.getCycleTimesValueStatistics("test").getMean(), 0.0001);
    assertEquals( 7.21773, report2.getResourcePoolWaitingTimes("clerk").getAverageLength(), 0.0001);
    assertEquals(72.78156, report2.getResourcePoolWaitingTimes("clerk").getAverageWaitTime(), 0.0001);
    
    ScenarioReport report3 = experiment.getSimulationReportForScenario("Three people");
    assertNotNull(report3);
    assertEquals("Three people", report3.getScenarioName());
    assertEquals(1440.0, report3.getSimulationRunTime(), 0.0001);    
    assertEquals( 1.39201, report3.getStateWaitingTimes("task one").getMean(), 0.0001);
    assertEquals(22.63514, report3.getCycleTimesValueStatistics("test").getMean(), 0.0001);
    assertEquals( 0.14597, report3.getResourcePoolWaitingTimes("clerk").getAverageLength(), 0.0001);
    assertEquals( 6.00554, report3.getResourcePoolWaitingTimes("clerk").getAverageWaitTime(), 0.0001);

    ScenarioReport report4 = experiment.getSimulationReportForScenario("Five people");
    assertNotNull(report4);
    assertEquals("Five people", report4.getScenarioName());
    assertEquals(1440.0, report4.getSimulationRunTime(), 0.0001);    
    assertEquals( 0.01188, report4.getStateWaitingTimes("task one").getMean(), 0.0001);
    assertEquals(20.87838, report4.getCycleTimesValueStatistics("test").getMean(), 0.0001);
    assertEquals( 0.00125, report4.getResourcePoolWaitingTimes("clerk").getAverageLength(), 0.0001);
    assertEquals( 1.79324, report4.getResourcePoolWaitingTimes("clerk").getAverageWaitTime(), 0.0001);

    ScenarioReport report5 = experiment.getSimulationReportForScenario("A real crowd");
    assertNotNull(report5);
    assertEquals("A real crowd", report5.getScenarioName());
    assertEquals(1440.0, report5.getSimulationRunTime(), 0.0001);    
    assertEquals( 0.0    , report5.getStateWaitingTimes("task one").getMean(), 0.0001);
    assertEquals(20.89189, report5.getCycleTimesValueStatistics("test").getMean(), 0.0001);
    assertEquals( 0.0    , report5.getResourcePoolWaitingTimes("clerk").getAverageLength(), 0.0001);
    assertEquals( 0.0    , report5.getResourcePoolWaitingTimes("clerk").getAverageWaitTime(), 0.0001);
  }
}
