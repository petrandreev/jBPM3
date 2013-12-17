package org.jbpm.sim;

import java.util.List;


import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.sim.datasource.ProcessDataFilter;
import org.jbpm.sim.datasource.ProcessDataSource;
import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.def.JbpmSimulationScenario;
import org.jbpm.sim.exe.ExperimentReader;

public class VariableSourceAndFilterTest extends AbstractSimTestCase {

  private static final String testProcessXml = "<process-definition name='test' start-distribution='startDist'>"
      + "  <start-state name='start'>"
      + "    <transition to='change order' />"
      + "  </start-state>"
      + "  <task-node name='change order'>"
      + "  <task name='change order' swimlane='clerk' />"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end'/>"
      + "</process-definition>";

  private static final String experimentConfiguration = "<experiment name='MySimulationExperiment'"
      + "          run-time='100'"
      + "          real-start-time='01.01.1970 01:00:00:002'"
      + "          time-unit='minute'>"
      + "  <scenario name ='test'>"
      + "    <distribution name='startDist' sample-type='real' type='constant' value='20' /> "
      + "    <distribution name='taskDist'  sample-type='real' type='constant' value='20' /> "
      + "    <resource-pool name='clerk' pool-size='1'/> "
      + "    <data-source name='orders' "
      + "       handler='org.jbpm.sim.VariableSourceAndFilterTest$TestProcessVariableSource' /> "
      + "    <data-filter name='orders' "
      + "       handler='org.jbpm.sim.VariableSourceAndFilterTest$TestProcessVariableFilter' /> "
      + "    <sim-process name='test'>"
      + "       <process-overwrite start-distribution='startDist'> "
      + "         <use-data-source name='orders' /> "
      + "       </process-overwrite> "
      + "       <task-overwrite task-name='change order' time-distribution='taskDist'>"
      + "         <use-data-filter name='orders' /> "
      + "       </task-overwrite> "
      + "    </sim-process>"
      + "  </scenario>"
      + "  <output path='target' />" // currently just used as parameter for DESMO-J      
      + "</experiment>";

  static boolean dataSourceIsCalled = false;
  static boolean dataFilterIsCalled = false;

  public static class TestProcessVariableSource implements ProcessDataSource {

    public void addNextData(ExecutionContext ctx) {
      dataSourceIsCalled = true;
      ctx.getContextInstance().createVariable("test", "Hello Bernd");
    }

    public boolean hasNext() {
      return true;
    }

    public void reset() {
    }
  }

  public static class TestProcessVariableFilter implements ProcessDataFilter {

    public void changeProcessData(ExecutionContext ctx) {
      dataFilterIsCalled = true;
      String test = (String) ctx.getContextInstance().getVariable("test");
      test += ", how are you?";
      ctx.getContextInstance().setVariable("test", test);
    }

    public void reset() {
    }
  }

  public void testSourceAndFilter() {
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    // inject process definition
    reader.addProcessDefinition("test", testProcessXml);

    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setRememberEndedProcessInstances(true);
    experiment.run();

    assertTrue(dataSourceIsCalled);
    assertTrue(dataFilterIsCalled);
    JbpmSimulationScenario scenario = experiment.getScenario("test");
    ProcessInstance pi = (ProcessInstance) scenario.getEndedProcessInstances().iterator().next();

    assertEquals("Hello Bernd, how are you?", pi.getContextInstance().getVariable("test"));

    // simulation runs till the end
    assertEquals(100, scenario.getScenarioReport().getSimulationRunTime(), 0.001);
  }

  private static final String experimentConfiguration2 = "<experiment name='MySimulationExperiment'"
      + "          run-time='100'"
      + "          real-start-time='01.01.1970 01:00:00:002'"
      + "          time-unit='minute'>"
      + "  <scenario name ='test'>"
      + "    <distribution name='startDist' sample-type='real' type='constant' value='20' /> "
      + "    <distribution name='taskDist'  sample-type='real' type='constant' value='20' /> "
      + "    <resource-pool name='clerk' pool-size='1'/> "
      + "    <data-source name='orders' "
      + "       handler='org.jbpm.sim.VariableSourceAndFilterTest$TestProcessVariableSourceExhausting' /> "
      + "    <sim-process name='test'>"
      + "       <process-overwrite start-distribution='startDist'> "
      + "         <use-data-source name='orders' /> "
      + "       </process-overwrite> "
      + "       <task-overwrite task-name='change order' time-distribution='taskDist' />"
      + "    </sim-process>"
      + "  </scenario>"
      + "  <output path='target' />" // currently just used as parameter for DESMO-J      
      + "</experiment>";

  static int calledCount = 0;

  public static class TestProcessVariableSourceExhausting implements ProcessDataSource {

    public void addNextData(ExecutionContext ctx) {
      calledCount++;
      ctx.getContextInstance().createVariable("test", new Integer(calledCount));
    }

    public boolean hasNext() {
      return (calledCount < 3);
    }

    public void reset() {
    }
  }

  /**
   * use a data source which provides data only three times, the simulation should be ended after
   * the third query, at this time two processes are ended
   */
  public void testSimulationStopIfSourceIsExhausted() {
    ExperimentReader reader = new ExperimentReader(experimentConfiguration2);
    // inject process definition
    reader.addProcessDefinition("test", testProcessXml);

    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setRememberEndedProcessInstances(true);
    experiment.run();

    JbpmSimulationScenario scenario = experiment.getScenario("test");
    List processes = scenario.getEndedProcessInstances();

    assertEquals(3, calledCount);
    assertEquals(2, processes.size());

    assertEquals(new Integer(1), ((ProcessInstance) processes.get(0)).getContextInstance()
        .getVariable("test"));
    assertEquals(new Integer(2), ((ProcessInstance) processes.get(1)).getContextInstance()
        .getVariable("test"));

    // after two processes the simulation should end, this is 60 time units
    assertEquals(60, scenario.getScenarioReport().getSimulationRunTime(), 0.001);
  }

}
