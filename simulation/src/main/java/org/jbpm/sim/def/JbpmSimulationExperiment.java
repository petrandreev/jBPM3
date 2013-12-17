package org.jbpm.sim.def;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jbpm.sim.exception.ExperimentConfigurationException;
import org.jbpm.sim.exe.SimulationProgressListener;
import org.jbpm.sim.report.ExperimentReport;
import org.jbpm.sim.report.ScenarioReport;


import desmoj.core.simulator.Units;

/**
 * This class represents a full blown jbpm simulation experiment,
 * which can include several simulation scenarios, maybe to compare them.
 * 
 * Every scenario inside the JbpmSimulationExperiment is executed in a 
 * DESMO-J experiment. So don't be confused about this two different 
 * definition of the term "experiment" in this area.
 * 
 * The configuration is done with this XML:
 *   <experiment name='MySimulationExperiment' 
 *               run-time='100'
 *               real-start-time='30.03.1980 00:00:00'  
 *               time-unit='minute'> 
 * 
 * @author bernd.ruecker@camunda.com
 */
public class JbpmSimulationExperiment {
  
  public static int MILLISECONDS = Units.MS;
  public static int SECONDS = Units.S;
  public static int MINUTES = Units.MIN;
  public static int HOURS = Units.H;
  
  /**
   * name of experiment
   */
  private String name;
  
  /**
   * Specifies after which simulation time the experiment should be stopped.
   * Hint: If no more events are in the simulation event queue, the simulation
   * may stop <b>earlier</b> than this time.
   * 
   * Default is 1440, which corresponds to one day if the time unit is MINUTES
   */
  private double simulationRunTime = 1440.0;
  
  /**
   * Specifies a point in model time at which all statistical counters
   * are reseted. This relates to what is called the steady state, the initial
   * transient is cut out off all statistics
   */
  private double resetTime = 0;

  /**
   * Modeling time can be transverted into real time, which is used for example
   * in the jbpm logs. Here you can configure the date which equals the SimTime 0.
   * 
   * The default is: "01.01.1970 01:00:00.000" = new Date(0)
   */
  // The default is my birthday: "30.03.1980 00:00:00.0"
  private Date realStartDate = new Date(0);
  
  /**
   * the time unit of the simulation time. Currently supported are the values:
   * milliseconds, seconds, minutes and hours. Please use the constants for that,
   * which are a wrapper around <code>desmoj.core.simulator.Units</code>.
   * 
   * default is minutes
   */
  private int timeUnit = MINUTES;
  
  /**
   * Currency used for calculated costs. At the moment this is just a 
   * text info for simplicity. Only one currency can be used at a time
   */
  private String currency = "EUR";
  
  /**
   * This factor defines, how the unutilized time of resources is taken
   * into account while calculating the costs.
   * 1.0 means, the unutilized time is included in the cost calculation,
   * 0.0 means, the unutilized time is not included,
   * 0,75 means, the unutilized time is included with 75 % of the costs of the
   * "normal" (=utilized) time.
   */
  private double unutilizedTimeCostFactor = 0;
  
  /**
   * All defined scenarios of this experiment in a map (with the 
   * experiment name as key)
   */
  private Map scenarios = new HashMap();
  
  /**
   * where to save the DESMO-J output report to
   * TODO: This is a temporary solution, the report should be
   * generated in memory as a stream instead.
   */
  private String outputPathName = ".";
  
  /**
   * configure if the default desmoj HTML report should be 
   * written to a file in the configured output path
   */
  private boolean writeDesmojHtmlOutput = false;

  /**
   * configures if a local history of ended process instances
   * is kept. useful to persist them later, if that makes sense
   */
  private boolean rememberEndedProcessInstances = false;
  
  /**
   * seed value used for simulation (if different to 0)
   */
  private long seed = 0;
  
  private ExperimentReport report = null;
  
  public JbpmSimulationExperiment(String name) {
    if (name==null)
      name = "JbpmSimulationExperiment";
    this.name = name;
  }

  public void addScenario(JbpmSimulationScenario scenario) {
    scenarios.put(scenario.getName(), scenario);   
  }
  
  /**
   * run the experiment and start simulation of all scenario's.
   * 
   * Please note, that this can take some time and the method
   * is blocking.
   */
  public void run() {
    run(null);
  }
  
  /**
   * run the experiment and start simulation of all scenario's.
   * 
   * Please note, that this can take some time and the method
   * is blocking.
   */
  public void run(SimulationProgressListener listener) {  
    report = new ExperimentReport(getName(), getCurrency());   
    int scenarioNumber = 0;
    int count = scenarios.values().size();
    
    for (Iterator iterator = scenarios.values().iterator(); iterator.hasNext();) {
      JbpmSimulationScenario scenario = (JbpmSimulationScenario) iterator.next();
      if (listener != null)
        listener.newScenario(scenario.getName(), scenarioNumber, count);

      if (scenario.isExecute()) {
        // TODO: use SimulationProgressListener in simulation run
        scenario.runSimulation(this,// 
            getWriteDesmojHtmlOutput(), //
            rememberEndedProcessInstances, //
            seed);
        
        ScenarioReport scenarioReport = scenario.getScenarioReport();
        scenarioReport.calculateResourceCosts( unutilizedTimeCostFactor );
        report.addScenarioReport( scenarioReport );
      }
      scenarioNumber++;
    }
  }
  
  public Collection getScenarios() {
    return scenarios.values();
  }

  public JbpmSimulationScenario getScenario(String name) {
    return (JbpmSimulationScenario) scenarios.get(name);
  }

  /**
   * retrieves the <code>org.jbpm.sim.report.SimulationReport</code> for the specified scenario.
   * 
   * The SimulationResult is null in case the simulation hasn't run yet.
   * 
   * @param scenarioName the name of the scenario
   * @return  the simulation report
   */
  public ScenarioReport getSimulationReportForScenario(String scenarioName) {
    return ((JbpmSimulationScenario) scenarios.get(scenarioName)).getScenarioReport();
  }
  
  public void setSimulationRunTime(double simulationRunTime) {
    this.simulationRunTime = simulationRunTime;
  }

  public void setRealStartDate(String realStartTimeString) {
    try {
      setRealStartDate(
          new SimpleDateFormat(JbpmSimulationScenario.DATE_PATTERN).parse(realStartTimeString));
    }
    catch (ParseException e) {
      throw new ExperimentConfigurationException("real start date '" + realStartTimeString  +
          "' does not match pattern '" + JbpmSimulationScenario.DATE_PATTERN + "'", e);
    }    
  }
  
  public void setRealStartDate(Date realStartDate) {
    this.realStartDate = realStartDate;
  }

  public void setTimeUnit(int timeUnit) {
    this.timeUnit = timeUnit;
  }

  public void setOutputPathName(String outputPathName) {
    if (outputPathName==null)
      this.outputPathName = ".";
    else
      this.outputPathName = outputPathName;
  }

  public String getName() {
    return name;
  }

  public double getSimulationRunTime() {
    return simulationRunTime;
  }

  public Date getRealStartDate() {
    return realStartDate;
  }

  public int getTimeUnit() {
    return timeUnit;
  }

  public String getOutputPathName() {
    return outputPathName;
  }

  public boolean getWriteDesmojHtmlOutput() {
    return writeDesmojHtmlOutput;
  }

  public void setWriteDesmojHtmlOutput(boolean writeDesmojHtmlOutput) {
    this.writeDesmojHtmlOutput = writeDesmojHtmlOutput;
  }

  public ExperimentReport getReport() {
    return report;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public boolean isRememberEndedProcessInstances() {
    return rememberEndedProcessInstances;
  }

  public void setRememberEndedProcessInstances(
      boolean rememberEndedProcessInstances) {
    this.rememberEndedProcessInstances = rememberEndedProcessInstances;
  }

  public double getUnutilizedTimeCostFactor() {
    return unutilizedTimeCostFactor;
  }

  public void setUnutilizedTimeCostFactor(double unutilizedTimeCostFactor) {
    this.unutilizedTimeCostFactor = unutilizedTimeCostFactor;
  }

  public double getResetTime() {
    return resetTime;
  }

  public void setResetTime(double resetTime) {
    this.resetTime = resetTime;
  }

  public long getSeed() {
    return seed;
  }

  public void setSeed(long seed) {
    this.seed = seed;
  }
}
