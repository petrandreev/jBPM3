package org.jbpm.sim.report;

import java.util.Collection;
import java.util.Iterator;

import org.jbpm.sim.SimulationConstants;
import org.jbpm.sim.def.JbpmSimulationModel;
import org.jbpm.sim.report.dto.CountResult;
import org.jbpm.sim.report.dto.QueueStatisticsResult;
import org.jbpm.sim.report.dto.TimeSeriesResult;
import org.jbpm.sim.report.dto.UtilizationStatisticsResult;
import org.jbpm.sim.report.dto.ValueStatisticResult;

import desmoj.core.dist.Distribution;
import desmoj.core.report.Message;
import desmoj.core.report.MessageReceiver;
import desmoj.core.report.Reporter;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.QueueBased;
import desmoj.core.simulator.Reportable;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.TimeSeries;
import desmoj.core.statistic.ValueStatistics;

/**
 * collects output from DESMO-J and saves them in memory.
 * The statistical figure are copied to a bunch of POJO's
 * (ValueObjects), so the garbage collector can clean up
 * the simulation stuff
 * 
 * @author bernd.ruecker@camunda.com
 */
public class InMemoryOutput implements MessageReceiver {
 
  private ScenarioReport report = null;

  public InMemoryOutput() {
    report = new ScenarioReport();
  }
  
  /**
   * the experiment name should be EXPERIMENT.SCENARIO in the
   * jbpm simulation enviroment. Isolate the scenario here, to be less
   * verbose in reports.
   */
  private String getScenarioName(Experiment experiment)  {
    if (experiment.getName()!=null && experiment.getName().indexOf(".")>-1)
      return experiment.getName().substring( experiment.getName().indexOf(".") + 1 );
    else
      return experiment.getName();
  }

  public void receive(Reporter r) {
    if (r == null || r.getReportable()==null)
      return;

    /**
     * We have to handle different Reportables here.
     * Unfortunately it is not possible in DESMO-J to plug in own Reporters 
     * for the different Reportables, so we have to get the source
     * from the Default Reporter here, and look which object
     * it observers.
     */
    Reportable source = r.getReportable();
//    log.debug( "SOURCE: " + (source!=null ? source + "[" + source.getClass().getName() + "]" : "NULL"));
    String scenarioName = getScenarioName(r.getModel().getExperiment());

    // if this is not possible, we do not run in a jbpm simulation
    // ignore that problem
    JbpmSimulationModel jbpmModel = (JbpmSimulationModel) source.getModel();
    String name = jbpmModel.getShortNameFor(source.getName());

    if (source instanceof Model) {
      Model model = (Model)source;
      
      report.setScenarioName( scenarioName );
      /*
       * For Simulation run time don't use:
       *    model.getExperiment().getStopTime().getTimeValue() );
       * because it is the configured end time, if the simulation is stopped 
       * earlier, the right run time is in the current model time
       */
      report.setSimulationRunTime( model.currentTime().getTimeValue() );
      report.setResetTime( model.resetAt().getTimeValue() );
      
      // add resource pool time series (there is no own reporter for it
      // available
      String[] resourcePools = jbpmModel.getResourcePoolNames();
      for (int i = 0; i < resourcePools.length; i++) {
        TimeSeries ts = jbpmModel.getResourcePoolTimeSeries( resourcePools[i] );
        String tsName = jbpmModel.getShortNameFor(ts.getName());
        report.addResourcePoolTimeSeries(
            new TimeSeriesResult(tsName, scenarioName, ts));
      }
      
      // add other business figures to report
      Collection businessFigureTypes = jbpmModel.getBusinessFigureTypes();
      for (Iterator iterator = businessFigureTypes.iterator(); iterator
          .hasNext();) {
        String type = (String) iterator.next();
        double sum = jbpmModel.getBusinessFigureSum(type);
        report.addBusinessFigure(type, sum);
      }
    }
    else if (source instanceof ValueStatistics) {
      ValueStatistics vs = (ValueStatistics)source;

      if (vs.getName().startsWith(SimulationConstants.NAME_PREFIX_WAITING_BEFORE_STATE) && vs.getName().endsWith(SimulationConstants.NAME_SUFFIX_WAITING_FOR_RESOURCE))
        report.addStateWaitStatistics(new ValueStatisticResult(name, scenarioName, vs));
      else if (vs.getName().startsWith(SimulationConstants.NAME_PREFIX_PROCESS_CYCLE_TIME) && vs.getName().endsWith(SimulationConstants.NAME_SUFFIX_PROCESS_CYCLE_TIME))
        report.addProcessCycleTimeStatistics(new ValueStatisticResult(name, scenarioName, vs));
      else
        report.addMiscValueStatistics(new ValueStatisticResult(name, scenarioName, vs));
    }
    else if (source instanceof QueueBased) {
      QueueBased queue = (QueueBased)source;            

      if (queue.getName().startsWith(SimulationConstants.NAME_PREFIX_RESOURCE_QUEUE) && queue.getName().endsWith(SimulationConstants.NAME_SUFFIX_RESOURCE_QUEUE))
        report.addResourcePoolWaitingTimes(new QueueStatisticsResult(name, scenarioName, queue));
      else if (queue.getName().startsWith(SimulationConstants.NAME_PREFIX_RESOURCE_POOL) && queue.getName().endsWith(SimulationConstants.NAME_SUFFIX_RESOURCE_POOL))
        report.addResourcePoolUtilization(new UtilizationStatisticsResult(name, scenarioName, queue, jbpmModel.getResourcePool(name).getCostPerTimeUnit()));
      else
        report.addMiscQueueStatistics(new QueueStatisticsResult(name, scenarioName, queue));
    }
    else if (source instanceof Count) {
      Count count = (Count)source;
      
      // we have counts for process starts and ends
      if (count.getName().startsWith(SimulationConstants.NAME_PREFIX_PROCESS_END_STATE) && count.getName().endsWith(SimulationConstants.NAME_SUFFIX_PROCESS_END_STATE))
        report.addProcessEndCount(new CountResult(name, scenarioName, count));
      else if (count.getName().startsWith(SimulationConstants.NAME_PREFIX_PROCESS_START) && count.getName().endsWith(SimulationConstants.NAME_SUFFIX_PROCESS_START))
        report.addProcessStartCount(new CountResult(name, scenarioName, count));      
    }
    else if (source instanceof Distribution) {
      //    Distribution dist = (Distribution)source;
      
      // TODO: Think about what to do here... 
      // Maybe also interesting to query the original DistributionDefinition again here 
      
      //    dist.getInitialSeed();
      //    dist.getName();
      //    dist.getObservations();
      //    dist.getNumSamples();
      //    dist.getClass().getSimpleName();
      
      // to get more informations we need to dig deeper here
    }
  } 

  /**
   * method to be called when a Message is received. this class does not
   * handle Messages so it will simply return
   */
  public void receive(Message m) {
    return;
  }

  public ScenarioReport getReport() {
    return report;
  }

}
