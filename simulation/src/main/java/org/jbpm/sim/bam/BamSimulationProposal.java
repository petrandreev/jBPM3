package org.jbpm.sim.bam;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jbpm.sim.def.JbpmSimulationScenario;

/**
 * contains statistics about the history of a process (like processing times of tasks, outgoing probabilities of decisions, ....)
 * 
 * can be used to generate a XML scenario configuration or directly a <code>JbpmSimulationScenario</code> object.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class BamSimulationProposal
{
  private static final Log log = LogFactory.getLog(BamSimulationProposal.class);

  private String processName;

  private int processVersion;

  private String[] swimlanes;

  /**
   * state proposal elements (ElementStatistics as value)
   */
  private ArrayList stateProposals = new ArrayList();

  private ArrayList decisionProposals = new ArrayList();

  private ArrayList taskProposals = new ArrayList();

  private ElementStatistics processStatistics;

  public BamSimulationProposal()
  {
  }

  public BamSimulationProposal(String processName, int processVersion)
  {
    this.processName = processName;
    this.processVersion = processVersion;
  }

  /**
   * TODO: implement
   */
  public JbpmSimulationScenario getScenario()
  {
    return null;
  }

  /**
   * create a new scenario with this process as only process
   */
  public Element createScenarioConfigurationXml()
  {
    Element result = org.dom4j.DocumentFactory.getInstance().createElement("scenario");
    result.addAttribute("name", "status_quo");

    addProcessToScenarioXml(result);

    return result;
  }

  /**
   * adds this process to the already given scenario (as XML)
   * 
   * @param scenario
   */
  public void addProcessToScenarioXml(Element scenario)
  {
    Element simProcess = scenario.addElement("sim-process");
    // set default path for process
    simProcess.addAttribute("path", "/" + getProcessName() + "/processdefinition.xml");

    // set start distribution
    String startEventDistName = addErlangDistributionElement(scenario, getProcessStatistics());
    simProcess.addElement("process-overwrite").addAttribute("start-distribution", startEventDistName);

    // retrieve already existing swimlanes of the scenario
    HashSet alreadyAddedSwimlanes = new HashSet();
    Iterator iter = scenario.elementIterator("resource-pool");
    while (iter.hasNext())
    {
      Element rp = (Element)iter.next();
      alreadyAddedSwimlanes.add(rp.attributeValue("name"));
    }

    // add resource pools with default size for non existing swimlanes
    for (int i = 0; i < swimlanes.length; i++)
    {
      if (!alreadyAddedSwimlanes.contains(swimlanes[i]))
      {
        Element rp = scenario.addElement("resource-pool");
        rp.addAttribute("name", swimlanes[i]);
        rp.addAttribute("pool-size", "1");
        rp.addAttribute("costs-per-time-unit", "1");
        alreadyAddedSwimlanes.add(swimlanes[i]);
      }
    }

    // loop over states
    iter = stateProposals.iterator();
    while (iter.hasNext())
    {
      ElementStatistics statistics = (ElementStatistics)iter.next();

      // add distributions with default name
      String distName = addNormalDistributionElement(scenario, statistics);

      // add state-overwrite
      Element stateElement = simProcess.addElement("state-overwrite");
      stateElement.addAttribute("state-name", statistics.getName());
      stateElement.addAttribute("time-distribution", distName);

      addLeavingTransitionElements(statistics, stateElement);
    }

    // loop over decisions
    iter = decisionProposals.iterator();
    while (iter.hasNext())
    {
      ElementStatistics statistics = (ElementStatistics)iter.next();

      // add decision-overwrite
      Element stateElement = simProcess.addElement("decision-overwrite");
      stateElement.addAttribute("decision-name", statistics.getName());

      addLeavingTransitionElements(statistics, stateElement);
    }

    // loop over tasks
    iter = taskProposals.iterator();
    while (iter.hasNext())
    {
      ElementStatistics statistics = (ElementStatistics)iter.next();

      // add distributions with default name
      String distName = addNormalDistributionElement(scenario, statistics);

      // add task-overwrite
      Element stateElement = simProcess.addElement("task-overwrite");
      stateElement.addAttribute("task-name", statistics.getName());
      stateElement.addAttribute("time-distribution", distName);

      addLeavingTransitionElements(statistics, stateElement);
    }
  }

  private void addLeavingTransitionElements(ElementStatistics statistics, Element stateElement)
  {
    // loop over leaving transitions
    TransitionProbability[] transitionProbabilities = statistics.getLeavingTransitionProbabilities();
    for (int i = 0; i < transitionProbabilities.length; i++)
    {
      // add probability
      Element transitionElement = stateElement.addElement("transition");
      transitionElement.addAttribute("name", transitionProbabilities[i].getTransitionName());
      transitionElement.addAttribute("probability", String.valueOf(transitionProbabilities[i].getCount()));
    }
  }

  private String addErlangDistributionElement(Element scenario, ElementStatistics stat)
  {
    return addDistributionElement(scenario, stat, "erlang", "real");
  }

  private String addNormalDistributionElement(Element scenario, ElementStatistics stat)
  {
    return addDistributionElement(scenario, stat, "normal", "real");
  }

  private String addDistributionElement(Element scenario, ElementStatistics stat, String type, String sampleType)
  {
    String name = getProcessName() + "." + stat.getName();
    Element dist = scenario.addElement("distribution");
    dist.addAttribute("name", name);
    dist.addAttribute("sample-type", sampleType);
    dist.addAttribute("type", type);
    dist.addAttribute("mean", String.valueOf(stat.getDurationAverage()));
    dist.addAttribute("standardDeviation", String.valueOf(stat.getDurationStddev()));
    return name;
  }

  public void addStateProposal(ElementStatistics stat)
  {
    stateProposals.add(stat);
  }

  public ElementStatistics[] getStateProposals()
  {
    return (ElementStatistics[])stateProposals.toArray(new ElementStatistics[0]);
  }

  public void addDecisionProposal(ElementStatistics statistics)
  {
    decisionProposals.add(statistics);
  }

  public ElementStatistics[] getDecisionProposals()
  {
    return (ElementStatistics[])decisionProposals.toArray(new ElementStatistics[0]);
  }

  public void addTaskProposal(ElementStatistics statistics)
  {
    taskProposals.add(statistics);
  }

  public ElementStatistics[] getTaskProposals()
  {
    return (ElementStatistics[])taskProposals.toArray(new ElementStatistics[0]);
  }

  public static void print(Element scenarioXml)
  {
    log.debug(getXmlAsString(scenarioXml));
  }

  public static String getXmlAsString(Element scenarioXml)
  {
    StringWriter stringWriter = new StringWriter();
    try
    {
      OutputFormat format = OutputFormat.createPrettyPrint();
      XMLWriter xmlWriter = new XMLWriter(stringWriter, format);
      xmlWriter.write(scenarioXml);
    }
    catch (IOException ex)
    {
      throw new RuntimeException("couldn't write XML to string", ex);
    }
    return stringWriter.toString();
  }

  /**
   * print out result on log.debug
   * 
   * ONLY FOR TESTING PURPOSES WHEN PLAYING AROUND...
   * 
   * @param result
   */
  public static void print(BamSimulationProposal result)
  {
    log.debug("\n\n------ PROCESS " + result.getProcessName() + " / Version: " + result.getProcessVersion() + " ---------");
    log.debug("  start event sample count    = " + result.getProcessStatistics().getSampleCount());
    log.debug("  start event interval min    = " + result.getProcessStatistics().getDurationMin());
    log.debug("  start event interval max    = " + result.getProcessStatistics().getDurationMax());
    log.debug("  start event interval avg    = " + result.getProcessStatistics().getDurationAverage());
    log.debug("  start event interval stddev = " + result.getProcessStatistics().getDurationStddev());

    log.debug("\n------ STATEs ---------");

    for (int i = 0; i < result.getStateProposals().length; i++)
    {
      ElementStatistics stat = result.getStateProposals()[i];
      log.debug(stat.getName() + ": ");
      log.debug("  sample count    = " + stat.getSampleCount());
      log.debug("  duration min    = " + stat.getDurationMin());
      log.debug("  duration max    = " + stat.getDurationMax());
      log.debug("  duration avg    = " + stat.getDurationAverage());
      log.debug("  duration stddev = " + stat.getDurationStddev());

      for (int j = 0; j < stat.getLeavingTransitionProbabilities().length; j++)
      {
        log.debug("  -> " + stat.getLeavingTransitionProbabilities()[j].getTransitionName() + ": " + stat.getLeavingTransitionProbabilities()[j].getCount());
      }
    }

    log.debug("\n------ DECISIONs ---------");
    for (int i = 0; i < result.getDecisionProposals().length; i++)
    {
      ElementStatistics stat = result.getDecisionProposals()[i];
      log.debug(stat.getName() + ": ");

      log.debug("  sample count    = " + stat.getSampleCount());
      log.debug("  duration min    = " + stat.getDurationMin());
      log.debug("  duration max    = " + stat.getDurationMax());
      log.debug("  duration avg    = " + stat.getDurationAverage());
      log.debug("  duration stddev = " + stat.getDurationStddev());

      for (int j = 0; j < stat.getLeavingTransitionProbabilities().length; j++)
      {
        log.debug("  -> " + stat.getLeavingTransitionProbabilities()[j].getTransitionName() + ": " + stat.getLeavingTransitionProbabilities()[j].getCount());
      }
    }

    log.debug("\n------ TASKs ---------");

    for (int i = 0; i < result.getTaskProposals().length; i++)
    {
      ElementStatistics stat = result.getTaskProposals()[i];
      log.debug(stat.getName() + ": ");
      log.debug("  sample count    = " + stat.getSampleCount());
      log.debug("  duration min    = " + stat.getDurationMin());
      log.debug("  duration max    = " + stat.getDurationMax());
      log.debug("  duration avg    = " + stat.getDurationAverage());
      log.debug("  duration stddev = " + stat.getDurationStddev());

      for (int j = 0; j < stat.getLeavingTransitionProbabilities().length; j++)
      {
        log.debug("  -> " + stat.getLeavingTransitionProbabilities()[j].getTransitionName() + ": " + stat.getLeavingTransitionProbabilities()[j].getCount());
      }
    }
  }

  public ElementStatistics getProcessStatistics()
  {
    return processStatistics;
  }

  public void setProcessStatistics(ElementStatistics processStatistics)
  {
    this.processStatistics = processStatistics;
  }

  public int getProcessVersion()
  {
    return processVersion;
  }

  public void setProcessVersion(int processVersion)
  {
    this.processVersion = processVersion;
  }

  public String getProcessName()
  {
    return processName;
  }

  public void setProcessName(String processName)
  {
    this.processName = processName;
  }

  public String[] getSwimlanes()
  {
    return swimlanes;
  }

  public void setSwimlanes(String[] swimlanes)
  {
    this.swimlanes = swimlanes;
  }
}
