package org.jbpm.sim.report.jasper;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.exe.ExperimentReader;
import org.jbpm.sim.report.ExperimentReport;

/**
 * This report shows the comparison of multiple scenarios.
 * 
 * Currently waiting, if it is possible to add different tasks on 
 * one stacked bar or line chart. See <a href="http://www.jasperforge.org/index.php?option=com_joomlaboard&Itemid=215&func=view&id=32269&catid=8">JasperReports forum entry</a>.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ScenarioComparisionReport extends AbstractBaseJasperReport {

  private ExperimentReport report;

  public ScenarioComparisionReport(ExperimentReport report)  {
    this.report = report;
  }
  
  public Object[] getContent() {
    /**
     * we need at least one "pseudo" row, otherwise the report will be empty
     */
    return new Object[] {""};
  }

  public Map getReportParameters() {
    HashMap params = new HashMap();
    params.put("EXPERIMENT_REPORT", report); 
    return params;
  }

  public String getReportPath() {
    return "/org/jbpm/sim/report/jasper/ScenarioComparison.jasper";
  }

  public Map getSubreportPaths() {
    HashMap subreports = new HashMap();
    
    subreports.put("SUBREPORT_WaitingTimeBeforeTaskComparison", 
        "/org/jbpm/sim/report/jasper/WaitingTimeBeforeTaskComparison.jasper");
    subreports.put("SUBREPORT_WaitingTimeForResourceComparison", 
        "/org/jbpm/sim/report/jasper/WaitingTimeForResourceComparison.jasper");
    subreports.put("SUBREPORT_ResourceUtilizationComparison", 
        "/org/jbpm/sim/report/jasper/ResourceUtilizationComparison.jasper");
    subreports.put("SUBREPORT_ScenarioComparisonTable", 
        "/org/jbpm/sim/report/jasper/ScenarioComparisonTable.jasper");
    subreports.put("SUBREPORT_ScenarioComparisonProcessCycleTimesTable", 
        "/org/jbpm/sim/report/jasper/ScenarioComparisonProcessCycleTimesTable.jasper");
    
    return subreports;
  }
  
  /**
   * run a small simulation and show results in Report
   * @param args
   */
  public static void main(String[] args) {
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
      "<experiment name='StaffingExperiment' run-time='100'>" +  
      "  <scenario name='01 clerk'>" +  
      "    <resource-pool name='clerk' pool-size='1' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='02 clerks'>" +  
      "    <resource-pool name='clerk' pool-size='2' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='03 clerks'>" +  
      "    <resource-pool name='clerk' pool-size='3' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='05 clerks'>" +  
      "    <resource-pool name='clerk' pool-size='5' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "  <scenario name='10 clerks'>" +  
      "    <resource-pool name='clerk' pool-size='10' />" +      
      "    <sim-process name='test' />" +
      "  </scenario>" +
      "</experiment>";   
    
    ExperimentReader reader = new ExperimentReader(experimentConfiguration);
    reader.addProcessDefinition( "test", processXml );    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setWriteDesmojHtmlOutput(true);
    experiment.run();
    
    ScenarioComparisionReport report = new ScenarioComparisionReport(experiment.getReport());
    report.show();
  }

}
