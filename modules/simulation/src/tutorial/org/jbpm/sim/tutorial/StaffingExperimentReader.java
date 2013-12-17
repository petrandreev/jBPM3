package org.jbpm.sim.tutorial;

import java.util.HashMap;
import java.util.Iterator;

import org.dom4j.Element;
import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.def.JbpmSimulationScenario;
import org.jbpm.sim.exe.ExperimentReader;
import org.jbpm.sim.report.ExperimentReport;
import org.jbpm.sim.report.jasper.ScenarioComparisionReport;
import org.xml.sax.InputSource;

/**
 * Simple example demonstrating how to overwrite the ExperimentReader
 * to instrument it to gnerate multiple scenarios based on some
 * more or less sophisticated algorithm.
 * 
 *  This example uses a very poor one, which just provides staffing strategies 
 *  with the given pool sizes (this is done in the super class)
 *  pool sizes + 1 and pool sizes + 2 (these are added here)
 * 
 * @author ruecker
 *
 */
public class StaffingExperimentReader extends ExperimentReader {
  
  private long addCount = 2;
  
  /**
   * flag to avoid endless loop
   */
  private boolean afterScenarioReadActive = false;

  public StaffingExperimentReader(InputSource inputSource) {
    super(inputSource);
  }

  public StaffingExperimentReader(String experimentXml) {
    super(experimentXml);
  }

  protected void afterScenarioRead(JbpmSimulationScenario scenario, Element scenarioElement, Element baseScenarioElement) {
    if (afterScenarioReadActive)
      return;
    
    afterScenarioReadActive = true;    
    HashMap pools = new HashMap();    
    HashMap costs = new HashMap();    
    
    Iterator poolElementIter = scenarioElement.elementIterator("resource-pool");
    while (poolElementIter.hasNext()) {
      Element resourcePoolElement = (Element) poolElementIter.next();
      String poolName = resourcePoolElement.attributeValue("name");
      String poolSizeText = resourcePoolElement.attributeValue("pool-size");
      Integer poolSize = new Integer(poolSizeText);
      pools.put(poolName, poolSize);
      costs.put(poolName, readCostPerTimeUnit(resourcePoolElement));
    }

    // add more scenarios with more people 
    // (so the provided people count is the lower limit)

    for (int add = 1; add <= addCount; add++) {
      JbpmSimulationScenario generatedScenario = readScenario(scenarioElement, baseScenarioElement);          
      generatedScenario.changeName(scenario.getName() + "+" + add);
      
      for (Iterator iterator = pools.keySet().iterator(); iterator.hasNext();) {
        String name = (String) iterator.next();
        Integer size = (Integer) pools.get(name);
        size = new Integer(size.intValue() + add);
        generatedScenario.addResourcePool(name, size, (Double)costs.get(name));
      }
      
      addScenario(generatedScenario);
    }    
    afterScenarioReadActive = false;
  }

  protected void beforeScenarioRead(JbpmSimulationScenario scenario,
      Element scenarioElement, Element baseScenarioElement) {
  }
  
  public static void main(String[] args) { 
    String experimentConf = "/org/jbpm/sim/tutorial/business//simulationExperiment.xml";

    StaffingExperimentReader reader = new StaffingExperimentReader(   
        new InputSource(StaffingExperimentReader.class.getResourceAsStream(experimentConf)));

    JbpmSimulationExperiment experiment = reader.readExperiment();
    experiment.setWriteDesmojHtmlOutput(true);
    experiment.setRememberEndedProcessInstances(false);

    experiment.run(); // can take some time

    ExperimentReport report = experiment.getReport();

    ScenarioComparisionReport r = new ScenarioComparisionReport(report);
    r.show();
  }  
}
