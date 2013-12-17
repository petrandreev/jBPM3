package org.jbpm.sim.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class holds simulation run details
 * (as simple POJOs) for a whole scenario
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ExperimentReport {
  
  private String experimentName; 

  private String currency; 

  /**
   * Map with all scenario results with the scenario name as key.
   * 
   * It is a Map<String, ScenarioReport>
   */
  private Map scenarioReports = new HashMap();

  public ExperimentReport(String experimentName) {
    this.experimentName = experimentName;
    this.currency = "";
  }

  public ExperimentReport(String experimentName, String currency) {
    this.experimentName = experimentName;
    this.currency = currency;
  }
  
  /**
   * returns a list with a ValueStatisticResult for
   * all states / task wait times for all contained 
   * scenarios
   */
  public List getAllStateWaitingTimes() {
    ArrayList result = new ArrayList();
    for (Iterator iterator = scenarioReports.values().iterator(); iterator.hasNext();) {
      ScenarioReport sr = (ScenarioReport) iterator.next();
      result.addAll(
        sr.getStateWaitingTimes());
    }
    return result;
  }

  /**
   * returns a list with a QueueStatisticResult for
   * all resource pools
   */
  public List getAllResourcePoolWaitingTimes() {
    ArrayList result = new ArrayList();
    for (Iterator iterator = scenarioReports.values().iterator(); iterator.hasNext();) {
      ScenarioReport sr = (ScenarioReport) iterator.next();
      result.addAll(
        sr.getResourcePoolWaitingTimes());
    }
    return result;
  }
  
  /**
   * returns a list with a UtilizationStatisticsResult for
   * all resource pools
   */
  public List getAllResourcePoolUtilizations() {
    ArrayList result = new ArrayList();
    for (Iterator iterator = scenarioReports.values().iterator(); iterator.hasNext();) {
      ScenarioReport sr = (ScenarioReport) iterator.next();
      result.addAll(
        sr.getResourcePoolUtilizations());
    }
    return result;
  }  
  
  public void addScenarioReport(ScenarioReport simulationReport) {
    scenarioReports.put(simulationReport.getScenarioName(), simulationReport);    
  }
  
  public ScenarioReport getScenarioReport(String scenarioName) {
    return (ScenarioReport) scenarioReports.get(scenarioName);
  }  

  public String getExperimentName() {
    return experimentName;
  }

  public int getScenarioCount() {
    return scenarioReports.keySet().size();
  }

  public Collection getScenarioReports() {
    return scenarioReports.values();
  }

  public String getCurrency() {
    return currency;
  }
}
