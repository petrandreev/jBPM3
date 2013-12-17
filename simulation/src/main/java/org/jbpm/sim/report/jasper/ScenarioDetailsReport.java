package org.jbpm.sim.report.jasper;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.exe.ExperimentReader;
import org.jbpm.sim.report.ScenarioReport;

/**
 * Report to show details of the simulation results of one scenario
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ScenarioDetailsReport extends AbstractBaseJasperReport {

  private ScenarioReport report;

  public ScenarioDetailsReport(ScenarioReport report)  {
    this.report = report;
  }
  
  public Object[] getContent() {
    /**
     * we need one object, otherwise the report is empty
     */
    return new Object[] {""};
  }

  public Map getReportParameters() {
    HashMap params = new HashMap();
    params.put("SCENARIO_REPORT", report); 
    return params;
  }

  public String getReportPath() {
    return "/org/jbpm/sim/report/jasper/ScenarioDetails.jasper";
  }

  public Map getSubreportPaths() {
    HashMap subreports = new HashMap();
    
    subreports.put("SUBREPORT_WaitingTimeBeforeTask", 
        "/org/jbpm/sim/report/jasper/WaitingTimeBeforeTask.jasper");
    subreports.put("SUBREPORT_WaitingTimeForResource", 
      "/org/jbpm/sim/report/jasper/WaitingTimeForResource.jasper");
    subreports.put("SUBREPORT_ResourceUsageTime", 
      "/org/jbpm/sim/report/jasper/ResourceUsageTime.jasper");
    subreports.put("SUBREPORT_ResourcePools", 
      "/org/jbpm/sim/report/jasper/ResourcePools.jasper");
//    
//  SUBREPORT_ResourcePools
    
    return subreports;
  }
  
  public static void main(String[] args) {
    String processXml =     
      "<process-definition name='test' start-distribution='start dist'>" +

      "  <distribution name='start dist' sample-type='real' type='normal' mean='10' standardDeviation='5'/> " +
      "  <distribution name='task dist'  sample-type='real' type='normal' mean='20' standardDeviation='5'/> " +
      "  <distribution name='task dist 2'  sample-type='real' type='normal' mean='5' standardDeviation='25'/> " +
      "  <distribution name='task dist 3'  sample-type='real' type='normal' mean='15' standardDeviation='5'/> " +

      "  <swimlane name='clerk' pool-size='10' />" +
      "  <swimlane name='manager' />" +

      "  <start-state name='start'>" +
      "    <transition to='task one' />" +
      "  </start-state>" +
      
      "  <task-node name='task one'>" +
      "    <task swimlane='clerk' time-distribution='task dist' />" +
      "    <transition to='task two' />" +
      "  </task-node>" +

      "  <task-node name='task two'>" +
      "    <task swimlane='manager' time-distribution='task dist 2' />" +
      "    <transition to='task three' />" +
      "  </task-node>" +

      "  <task-node name='task three'>" +
      "    <task swimlane='clerk' time-distribution='task dist 3' />" +
      "    <transition to='end' />" +
      "  </task-node>" +

      "  <end-state name='end'/>" +

      "</process-definition>" ;    
    
    String experimentConfiguration = 
      "<experiment name='StaffingExperiment' run-time='1000'>" +  
      "  <scenario name='Three people'>" +  
      "    <resource-pool name='clerk'   pool-size='2' />" +      
      "    <resource-pool name='manager' pool-size='1' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "</experiment>";   
    
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    reader.addProcessDefinition( "test", processXml );    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setWriteDesmojHtmlOutput(true);
    experiment.run();
    
    ScenarioDetailsReport report = new ScenarioDetailsReport(experiment.getSimulationReportForScenario("Three people"));
    report.show();
  }

}
