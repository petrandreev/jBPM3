package org.jbpm.sim.def;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.sim.datasource.ProcessDataFilter;
import org.jbpm.sim.datasource.ProcessDataSource;
import org.jbpm.sim.event.StatisticsResetEvent;
import org.jbpm.sim.exception.ExperimentConfigurationException;
import org.jbpm.sim.jpdl.SimulationDefinition;
import org.jbpm.sim.kpi.BusinessFigure;
import org.jbpm.sim.report.InMemoryOutput;
import org.jbpm.sim.report.ScenarioReport;

import desmoj.core.report.Reporter;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.SimTime;

/**
 * The scenario is one special simulation run, which relates to one
 * DESMO-J experiment (don't get confused, in the jbpm simulation an experiment
 * consists of one or more simulation scenarios!).
 * 
 * So can contain special configuration, for example for resource pools
 * or distributions. This can reflect different business scenarios, staffing
 * strategies or so on.
 * 
 * All configurations will <b>overwrite</b> possible simulation configurations
 * made in the process definition. 
 * 
 * An example XML for the scenario configuration:
 * 
 * <scenario name='NormalStaffing'>  
 *   <distribution name='start new process instances of test' sample-type='real' type='constant' value='20' /> 
 *   <resource-pool name='tester'      pool-size='2' />      
 *   <sim-process name='test'>
 *     <task-overwrite task-name='task one' time-distribution='time required for task one' /> 
 *     <process-overwrite start-distribution='start new process instances of test' /> 
 *   </sim-process> 
 * </scenario>
 * 
 * @author bernd.ruecker@camunda.com
 */
public class JbpmSimulationScenario {
  
  /**
   * name of scenario
   */
  private String name;
  
  /**
   * if set to false, the scenario is not executed in a simulation experiment
   */
  private boolean execute = true;
  
  /**
   * list of processed configured to be available
   * in this scenario
   */
  private List processDefinitions = new ArrayList();
  
  private List businessFigures = new ArrayList();
  
  private JbpmSimulationModel model = null;

  private ScenarioReport scenarioReport;

  public JbpmSimulationScenario() {
    this(null);
  }

  public JbpmSimulationScenario(String name) {
    if (name==null)
      this.name="DefaultScenario";
    else
      this.name = name;
  }
  
  public void changeName(String name) {
    this.name = name;
  }

  public void addProcessDefinition(ProcessDefinition processDefinition) {
    processDefinitions.add(processDefinition);
  }

  public void addDistribution(DistributionDefinition distDef) {
    for (Iterator iterator = processDefinitions.iterator(); iterator.hasNext();) {
      ProcessDefinition pd = (ProcessDefinition) iterator.next();
      SimulationDefinition def = (SimulationDefinition) pd.getDefinition(SimulationDefinition.class);
      
      def.overwriteDistribution(distDef);
    }
  }

  public void addResourcePool(String poolName, Integer poolSize, Double costPerTimeUnit) {
    for (Iterator iterator = processDefinitions.iterator(); iterator.hasNext();) {
      ProcessDefinition pd = (ProcessDefinition) iterator.next();
      SimulationDefinition def = (SimulationDefinition) pd.getDefinition(SimulationDefinition.class);
      
      def.overwriteResourcePool(poolName, poolSize, costPerTimeUnit);
    }
  }
  
  /**
   * copied from <code>desmoj.core.simulator.TimeConverter</code>, because there it is 
   * not public, but we need to have the date as string (unfortunately)
   */
  static final String DATE_PATTERN = "dd.MM.yyyy HH:mm:ss:SSS";

  public void runSimulation(JbpmSimulationExperiment jbpmExperiment, boolean writeHtmlReport, boolean rememberEndedProcessInstances) {
    runSimulation(jbpmExperiment, writeHtmlReport, rememberEndedProcessInstances, 0);
  }
  
  public void runSimulation(JbpmSimulationExperiment jbpmExperiment, boolean writeHtmlReport, boolean rememberEndedProcessInstances, long seed) {
    String reportOutputType = null;
    if (writeHtmlReport)
      reportOutputType = Experiment.DEFAULT_REPORT_OUTPUT_TYPE;
    
    Experiment desmojExperiment = new Experiment(
        jbpmExperiment.getName() + "." + getName(),
        jbpmExperiment.getOutputPathName(),
     // TODO: shouldn't be converted forth and back, use the REAL date
        new SimpleDateFormat(DATE_PATTERN).format(jbpmExperiment.getRealStartDate()), 
        jbpmExperiment.getTimeUnit(),
        reportOutputType, 
        null, //no trace instead of Experiment.DEFAULT_TRACE_OUTPUT_TYPE,
        // specify an error file, otherwise we get a NullPointerException when an error is logged:
        Experiment.DEFAULT_ERROR_OUTPUT_TYPE,  
        null); //no debug instead of Experiment.DEFAULT_DEBUG_OUTPUT_TYPE);
    
    if (seed!=0) {
      desmojExperiment.setSeedGenerator(seed);
    }
    
    // add our own reporter, to get the results later in memory
    InMemoryOutput report = new InMemoryOutput();
    desmojExperiment.addReceiver(report, Reporter.class);
    
    // construct the simulation model & connect it to the experiment
    DefaultJbpmSimulationModel model = new DefaultJbpmSimulationModel(
        (ProcessDefinition[])processDefinitions.toArray(new ProcessDefinition[0]));
    model.setRememberEndedProcessInstances(rememberEndedProcessInstances);
    model.connectToExperiment(desmojExperiment);

    // add statistic reset event if configured
    if (jbpmExperiment.getResetTime()>0) {
      StatisticsResetEvent evt = new StatisticsResetEvent(model);
      evt.schedule(new SimTime(jbpmExperiment.getResetTime()));
    }
    
    // add business figure configurations
    for (Iterator iterator = businessFigures.iterator(); iterator.hasNext();) {
      BusinessFigure figure = (BusinessFigure) iterator.next();
      // add a copy, because the object contains results afterwords
      // which should not occur in the original configuration
      model.addBusinessFigure(figure.copy());
    }
    
    // now set the time this simulation should stop at 
    desmojExperiment.stop(new SimTime(jbpmExperiment.getSimulationRunTime()));
    
    desmojExperiment.setShowProgressBar(false);
    desmojExperiment.start();

    // save report
    desmojExperiment.report();
    scenarioReport = report.getReport();

    // stop all threads still alive and close all output files
    desmojExperiment.finish();   
    
    if (rememberEndedProcessInstances)
      this.model = model;
    else
      this.model = null;
  }
  
  /**
   * persists the process definitions created for this scenario
   * and the process instances and all dependent stuff
   */
  public void persist(JbpmContext ctx) {
    for (Iterator iterator = processDefinitions.iterator(); iterator.hasNext();) {
      ProcessDefinition pd = (ProcessDefinition) iterator.next();
      ctx.deployProcessDefinition(pd);
    }
    
    for (Iterator iterator = model.getEndedProcessInstances().iterator(); iterator.hasNext();) {
      ProcessInstance pi = (ProcessInstance) iterator.next();
      ctx.save(pi);      
    }
  }


  public String getName() {
    return name;
  }

  public ScenarioReport getScenarioReport() {
    return scenarioReport;
  }

  public boolean isExecute() {
    return execute;
  }

  public void setExecute(boolean execute) {
    this.execute = execute;
  }

  public void addBusinessFigure(BusinessFigure figure) {
    businessFigures.add(figure);
  }

  public void addDataSource(String name, String className) {
    ProcessDataSource src = null;
    try {
      src = (ProcessDataSource) Class.forName(className).newInstance();
    }
    catch (Throwable ex) {
      throw new ExperimentConfigurationException("Couldn't initialize data source '" + name + "' of type '" + className + "'", ex);
    }
    addDataSource(name, src);
  }

  public void addDataSource(String name, ProcessDataSource src) {
    for (Iterator iterator = processDefinitions.iterator(); iterator.hasNext();) {
      ProcessDefinition pd = (ProcessDefinition) iterator.next();
      SimulationDefinition def = (SimulationDefinition) pd.getDefinition(SimulationDefinition.class);
      
      def.addDataSource(name, src);
    }    
  }
  
  public void addDataFilter(String name, String className) {
    ProcessDataFilter filter = null;
    try {
      filter = (ProcessDataFilter) Class.forName(className).newInstance();
    }
    catch (Throwable ex) {
      throw new ExperimentConfigurationException("Couldn't initialize data filter '" + name + "' of type '" + className + "'", ex);
    }   
    addDataFilter(name, filter);
  }

  public void addDataFilter(String name, ProcessDataFilter filter) {
    for (Iterator iterator = processDefinitions.iterator(); iterator.hasNext();) {
      ProcessDefinition pd = (ProcessDefinition) iterator.next();
      SimulationDefinition def = (SimulationDefinition) pd.getDefinition(SimulationDefinition.class);

      def.addDataFilter(name, filter);
    }        
  }

  /**
   * @return list of process definitions. Basically helpful for unit tests
   */
  public List getProcessDefinitions() {
    return processDefinitions;
  }
  
  /**
   * @return list of ended process instances. Basically helpful for unit tests
   */  
  public List getEndedProcessInstances() {
    return model.getEndedProcessInstances();
  }

}
