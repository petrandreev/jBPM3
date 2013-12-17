package org.jbpm.sim.report;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;

import org.jbpm.sim.def.DistributionDefinition;
import org.jbpm.sim.report.dto.BaseResult;
import org.jbpm.sim.report.dto.CountResult;
import org.jbpm.sim.report.dto.QueueStatisticsResult;
import org.jbpm.sim.report.dto.TimeSeriesResult;
import org.jbpm.sim.report.dto.UtilizationStatisticsResult;
import org.jbpm.sim.report.dto.ValueStatisticResult;

/**
 * This class holds simulation run details (as simple POJOs) for a special scenario
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ScenarioReport {

  private String scenarioName;

  private double simulationRunTime;

  private double resetTime;

  /**
   * all ValueStatisticResult objects for waiting time before states / tasks.
   */
  private Map stateWaitingTimes = new HashMap();

  /**
   * all ValueStatisticResult objects for process cycle times
   */
  private Map cycleTimesValueStatistics = new HashMap();

  /**
   * other ValueStatisticResult objects
   */
  private Map miscValueStatistics = new HashMap();

  private Map resourcePoolWaitingTimes = new HashMap();
  private Map resourcePoolUtilizations = new HashMap();
  private Map miscQueueStatistics = new HashMap();

  private Map resourcePoolTimeSeries = new HashMap();

  private Map distributionDefinitions = new HashMap();

  private Map businessFigureValues = new HashMap();

  private Map processEndCounts = new HashMap();
  private Map processStartCounts = new HashMap();

  public void addStateWaitStatistics(ValueStatisticResult vsr) {
    stateWaitingTimes.put(vsr.getName(), vsr);
  }

  public void addProcessCycleTimeStatistics(ValueStatisticResult vsr) {
    // the name is the process name (maybe we have different processes
    // in the scenario
    cycleTimesValueStatistics.put(vsr.getName(), vsr);
  }

  public void addMiscValueStatistics(ValueStatisticResult vsr) {
    miscValueStatistics.put(vsr.getName(), vsr);
  }

  public void addResourcePoolWaitingTimes(QueueStatisticsResult qsr) {
    resourcePoolWaitingTimes.put(qsr.getName(), qsr);
  }

  public void addMiscQueueStatistics(QueueStatisticsResult qsr) {
    miscQueueStatistics.put(qsr.getName(), qsr);
  }

  public void addResourcePoolUtilization(BaseResult usr) {
    resourcePoolUtilizations.put(usr.getName(), usr);
  }

  public void addResourcePoolTimeSeries(TimeSeriesResult timeSeriesReport) {
    resourcePoolTimeSeries.put(timeSeriesReport.getName(), timeSeriesReport);
  }

  public void addDistributionDefinition(DistributionDefinition o) {
    distributionDefinitions.put(o.getName(), o);
  }

  public void addProcessEndCount(CountResult countResult) {
    processEndCounts.put(countResult.getName(), countResult);
  }

  public void addProcessStartCount(CountResult countResult) {
    processStartCounts.put(countResult.getName(), countResult);
  }

  public void setScenarioName(String name) {
    scenarioName = name;
  }

  /**
   * creates an XML document out of the observations
   */
  public Document createDocument() {
    //Document document = DocumentHelper.createDocument();
    //Element reportRoot = document.addElement( "root" );   

    // TODO: Implement or maybe skip :-)
    throw new RuntimeException("create xml document of simulation statistics not yet implemented");

    // return document;
  }

  public String getScenarioName() {
    return scenarioName;
  }

  public void addBusinessFigure(String type, double sum) {
    businessFigureValues.put(type, new Double(sum));
  }

  public void calculateResourceCosts(double unutilizedTimeCostFactor) {
    for (Iterator iterator = getResourcePoolUtilizations().iterator(); iterator.hasNext();) {
      UtilizationStatisticsResult util = (UtilizationStatisticsResult) iterator.next();
      util.calculateCosts(unutilizedTimeCostFactor, simulationRunTime);
    }
  }

  public Collection getBusinessFigureTypes() {
    return businessFigureValues.keySet();
  }

  public double getBusinessFigureValue(String type) {
    return ((Double) businessFigureValues.get(type)).doubleValue();
  }

  /**
   * Convenience method after removing typo
   * 
   * @deprecated call {@link #getCosts()} instead
   */
  public double getCost() {
    return getCosts();
  }

  /**
   * @return costs (resource costs and business figures of type costs)
   */
  public double getCosts() {
    double result = getResourceCosts();
    for (Iterator iterator = getBusinessFigureTypes().iterator(); iterator.hasNext();) {
      String type = (String) iterator.next();
      if ("costs".equals(type)) {
        result += getBusinessFigureValue(type);
      }
    }
    return result;
  }

  /**
   * @return resource costs
   */
  public double getResourceCosts() {
    double result = 0;
    // calculate costs from resources with getResourcePoolUtilizations()
    for (Iterator iterator = getResourcePoolUtilizations().iterator(); iterator.hasNext();) {
      UtilizationStatisticsResult util = (UtilizationStatisticsResult) iterator.next();
      result += util.getCosts();
    }

    // TODO: add other costs
    // calculate additional "business" costs
    // maybe this should be calculated earlier and just be written as results here 

    return result;
  }

  public QueueStatisticsResult getWorstAverageResourceWaitingTime() {
    QueueStatisticsResult result = null;
    for (Iterator iterator = getResourcePoolWaitingTimes().iterator(); iterator.hasNext();) {
      QueueStatisticsResult stat = (QueueStatisticsResult) iterator.next();
      if (result == null || stat.getAverageWaitTime() > result.getAverageWaitTime()) {
        result = stat;
      }
    }
    return result;
  }

  public double getAverageResourceUtilization() {
    double utilization = 0;
    int observations = 0;
    for (Iterator iterator = getResourcePoolUtilizations().iterator(); iterator.hasNext();) {
      UtilizationStatisticsResult util = (UtilizationStatisticsResult) iterator.next();
      utilization += util.getAverageUtilization() * util.getPoolSize();
      observations += util.getPoolSize();
    }
    return utilization / observations;
  }

  public int getResourceAmount() {
    int result = 0;
    for (Iterator iterator = getResourcePoolUtilizations().iterator(); iterator.hasNext();) {
      UtilizationStatisticsResult util = (UtilizationStatisticsResult) iterator.next();
      result += util.getPoolSize();
    }
    return result;
  }

  public DistributionDefinition getDistributionDefinition(String name) {
    return (DistributionDefinition) distributionDefinitions.get(name);
  }

  public double getSimulationRunTime() {
    return simulationRunTime;
  }

  public void setSimulationRunTime(double simulationRunTime) {
    this.simulationRunTime = simulationRunTime;
  }

  public Collection getStateWaitingTimes() {
    return stateWaitingTimes.values();
  }

  public ValueStatisticResult getStateWaitingTimes(String name) {
    return (ValueStatisticResult) stateWaitingTimes.get(name);
  }

  public Collection getCycleTimesValueStatistics() {
    return cycleTimesValueStatistics.values();
  }

  public ValueStatisticResult getCycleTimesValueStatistics(String name) {
    return (ValueStatisticResult) cycleTimesValueStatistics.get(name);
  }

  public Collection getMiscValueStatistics() {
    return miscValueStatistics.values();
  }

  public ValueStatisticResult getMiscValueStatistics(String name) {
    return (ValueStatisticResult) miscValueStatistics.get(name);
  }

  public Collection getDistributionDefinitions() {
    return distributionDefinitions.values();
  }

  public Collection getResourcePoolWaitingTimes() {
    return resourcePoolWaitingTimes.values();
  }

  public QueueStatisticsResult getResourcePoolWaitingTimes(String name) {
    return (QueueStatisticsResult) resourcePoolWaitingTimes.get(name);
  }

  public Collection getResourcePoolUtilizations() {
    return resourcePoolUtilizations.values();
  }

  public UtilizationStatisticsResult getResourcePoolUtilization(String name) {
    return (UtilizationStatisticsResult) resourcePoolUtilizations.get(name);
  }

  public Collection getMiscQueueStatistics() {
    return miscQueueStatistics.values();
  }

  public QueueStatisticsResult getMiscQueueStatistics(String name) {
    return (QueueStatisticsResult) miscQueueStatistics.get(name);
  }

  public Collection getResourcePoolTimeSeries() {
    return resourcePoolTimeSeries.values();
  }

  public TimeSeriesResult getResourcePoolTimeSeries(String name) {
    return (TimeSeriesResult) resourcePoolTimeSeries.get(name);
  }

  public TimeSeriesResult getResourcePoolTimeSeriesResult(String poolName) {
    TimeSeriesResult result = (TimeSeriesResult) resourcePoolTimeSeries.get(poolName);
    if (result == null)
      return new TimeSeriesResult(poolName, scenarioName, new double[0], new double[0]);
    return result;
  }

  public Map getProcessEndCounts() {
    return processEndCounts;
  }

  public CountResult getProcessEndCount(String name) {
    return (CountResult) processEndCounts.get(name);
  }

  public Map getProcessStartCounts() {
    return processStartCounts;
  }

  public CountResult getProcessStartCount(String name) {
    return (CountResult) processStartCounts.get(name);
  }

  public double getResetTime() {
    return resetTime;
  }

  public void setResetTime(double resetTime) {
    this.resetTime = resetTime;
  }
}
