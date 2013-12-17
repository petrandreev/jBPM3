package org.jbpm.sim.exe;

import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.report.ExperimentReport;
import org.jbpm.sim.report.ScenarioReport;
import org.jbpm.sim.report.jasper.ScenarioComparisionReport;
import org.jbpm.sim.report.jasper.ScenarioDetailsReport;
import org.xml.sax.InputSource;

/**
 * main class to execute a simulation experiment and 
 * show the default report afterwards.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class JbpmSimulationExperimentRunner {

  /**
   * configures if the report (currently a JasperReports report)
   * is shown in a own window after the simulation run
   */
  private boolean showReport = true;
  
  /**
   * configures if the simulation run should be executed in a own thread
   */
  private boolean async = true;
  
  private boolean createDesmojHtmlOutput;
  
  private boolean rememberEndedProcessInstances;
  
  public JbpmSimulationExperimentRunner() {
  }

  public JbpmSimulationExperimentRunner(boolean showReport, boolean async,
      SimulationProgressListener progressListener) {
    this.showReport = showReport;
    this.async = async;
  }

  /**
   * read the the experiment configuration and runs it.
   * This includes start simulation of all scenario's.
   * 
   * The simulation can take some time, but this method
   * is not blocking if the property async is set to true
   * (which is the default).
   * 
   * If a <code>org.jbpm.sim.exe.SimulationProgressListener</code> is set, it is notifies  
   * of any progress of the simulation
   */  
  public void run(final String experimentConfigurationXml) {
    if (async) {
      new Thread(new Runnable() {
        public void run() {
          doTheJob(experimentConfigurationXml);
        }
      }).start();
    }
    else
      doTheJob(experimentConfigurationXml);
  }
  
  void doTheJob(String experimentConfigurationXmlPath) {
    ExperimentReader reader = new ExperimentReader(   
        new InputSource(this.getClass().getResourceAsStream(experimentConfigurationXmlPath)));
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setWriteDesmojHtmlOutput(createDesmojHtmlOutput);
    experiment.setRememberEndedProcessInstances(rememberEndedProcessInstances);
    
    experiment.run(); // can take some time
    
    ExperimentReport report = experiment.getReport();
    
    if (showReport)
      showReport(report);
  }

  private void showReport(ExperimentReport report) {
    if (report.getScenarioCount()==1) {
      ScenarioReport sr = (ScenarioReport)report.getScenarioReports().iterator().next();
      ScenarioDetailsReport r = new ScenarioDetailsReport(sr);
      r.show();
    }
    else if (report.getScenarioCount()>1) {
      ScenarioComparisionReport r = new ScenarioComparisionReport(report);
      r.show();
    }      
    // else: no results -> show no report
  }
  
  public static void main() {
    String experimentConf = "/xyz/conf.xml";
    boolean showReport = true;
    boolean runAsync = true;

    new org.jbpm.sim.exe.JbpmSimulationExperimentRunner(showReport, runAsync, null).run(experimentConf);
  }

  public void setCreateDesmojHtmlOutput(boolean createDesmojHtmlOutput) {
    this.createDesmojHtmlOutput = createDesmojHtmlOutput;
  }

  public boolean isRememberEndedProcessInstances() {
    return rememberEndedProcessInstances;
  }

  public void setRememberEndedProcessInstances(
      boolean rememberEndedProcessInstances) {
    this.rememberEndedProcessInstances = rememberEndedProcessInstances;
  }
  
}
