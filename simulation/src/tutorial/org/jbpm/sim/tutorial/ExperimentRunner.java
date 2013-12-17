package org.jbpm.sim.tutorial;

import java.util.Iterator;

import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.exe.ExperimentReader;
import org.jbpm.sim.report.ExperimentReport;
import org.jbpm.sim.report.ScenarioReport;
import org.jbpm.sim.report.dto.ValueStatisticResult;
import org.jbpm.sim.report.jasper.ScenarioComparisionReport;
import org.jbpm.sim.report.jasper.ScenarioDetailsReport;
import org.xml.sax.InputSource;

/**
 * Main class to run the tutorial, can be used instead of the Eclipse run time
 * configuration, to see how it works or if you don't run Eclipse.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ExperimentRunner {
  
  public static void main(String[] args) {
//    String experimentConf = "/org/jbpm/sim/tutorial/business//simulationExperiment.xml";    
//    String experimentConf = "/org/jbpm/sim/tutorial/business//simulationExperimentChristmas.xml";
    String experimentConf = "/org/jbpm/sim/tutorial/business/simulationExperimentAlternatives.xml";
    
    // the following code is also contained in the
    // ready to use JbpmSimulationExperimentRunner
    
    ExperimentReader reader = new ExperimentReader(   
        new InputSource(ExperimentRunner.class.getResourceAsStream(experimentConf)));
    
    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setWriteDesmojHtmlOutput(true);
    experiment.setRememberEndedProcessInstances(false);
    
    experiment.run(); // can take some time
    
    ExperimentReport report = experiment.getReport();

    ScenarioComparisionReport r = new ScenarioComparisionReport(report);
    r.show();

    //  ScenarioDetailsReport detailsReport1 = new ScenarioDetailsReport(report.getScenarioReport("status_quo_normal_case"));
    //  detailsReport1.show();

    //  ScenarioDetailsReport detailsReport2 = new ScenarioDetailsReport(report.getScenarioReport("christmas_normal_case"));
    //  detailsReport2.show();
    
    printOnConsole(report);    
  }

  private static void printOnConsole(ExperimentReport report) {
    for (Iterator iterator = report.getScenarioReports().iterator(); iterator.hasNext();) {
      ScenarioReport result = (ScenarioReport) iterator.next();
      System.out.println( "----\n" );

      System.out.print( result.getScenarioName() + "\t" );
      System.out.print( result.getWorstAverageResourceWaitingTime().getName() + "\t" );
      System.out.print( result.getAverageResourceUtilization() + "\t" );
      System.out.print( result.getResourceAmount() + "\t" );
      System.out.print( result.getCost() + "\t" );
//      System.out.println( result.getDescription() + "\t" );
      
      System.out.println( "\n" );

      for (Iterator iterator2 = result.getCycleTimesValueStatistics().iterator(); iterator2.hasNext();) {
        ValueStatisticResult vsr = (ValueStatisticResult) iterator2.next();
        System.out.print( "\t\t\t\t\t" );
        System.out.print( vsr.getName() + "\t" );
        System.out.print( vsr.getMean() + "\n" );        
      }
    }
  }
  
}
