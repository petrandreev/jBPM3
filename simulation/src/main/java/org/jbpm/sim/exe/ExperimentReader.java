package org.jbpm.sim.exe;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.jpdl.xml.JpdlParser;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.jpdl.xml.ProblemListener;
import org.jbpm.sim.datasource.UseDataFilterAction;
import org.jbpm.sim.datasource.UseDataSourceAction;
import org.jbpm.sim.def.DistributionDefinition;
import org.jbpm.sim.def.JbpmSimulationExperiment;
import org.jbpm.sim.def.JbpmSimulationScenario;
import org.jbpm.sim.exception.ExperimentConfigurationException;
import org.jbpm.sim.jpdl.SimulationDefinition;
import org.jbpm.sim.jpdl.SimulationJpdlXmlReader;
import org.jbpm.sim.kpi.BusinessFigure;
import org.jbpm.sim.kpi.BusinessFigureAction;
import org.jbpm.taskmgmt.def.Task;
import org.xml.sax.InputSource;

/**
 * The ExperimentReader is responsible for creating a 
 * <code>org.jbpm.sim.exe.JbpmSimulationExperiment</code> out of 
 * the experiment XML.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ExperimentReader implements ProblemListener {
  
  private static final long serialVersionUID = 1L;
  private static Log log = LogFactory.getLog(ExperimentReader.class);

  private InputSource inputSource = null;
  protected Document experimentDoc;
  
  private JbpmSimulationExperiment experiment;
  
  /**
   * internal registry of process definitions used in a experiment.
   * 
   * Especially useful if you want to use process definitions which 
   * can not be discovered by the reader itself (for example in unit tests) 
   */
  private Map processDefinitionRepository = new HashMap();
  
  public ExperimentReader(String experimentXml) {
    this(new InputSource(new StringReader(experimentXml)));
  }

  public ExperimentReader(InputSource inputSource) {
    this.inputSource = inputSource;
  }

  protected Document getXmlDocument() {
    if(experimentDoc==null) {
      try {
        experimentDoc = JpdlParser.parse(inputSource, this);
      }
      catch (Exception e) {
        throw new ExperimentConfigurationException("Couldn't read experiment XML", e);
      }
    }
    return experimentDoc;
  }

  public void addProblem(Problem problem) {
    log.warn("problem occured while parsing XML: " + problem);
    // TODO: implement addProblem (can be called from JpdlParser)    
  }
  
  /**
   * add a process definition to this reader, which can be referenced from a sim-process
   * in a experiment. This is useful in test cases, where you don't store the
   * process in a XML file, but as a String. 
   */
  public void addProcessDefinition(String processName, String processXml) {
    /*
     * we can not parse the ProcessDefinition here, because
     * if we have multiple scenarios, they save information about
     * resource pools or distributions in the SImulationDefinition
     * module of the processDefinition object. So only save the XML,
     * so we can create as many objects as we want from it
     */
    processDefinitionRepository.put(processName, processXml);
  }
  
  public JbpmSimulationExperiment readExperiment()  {
    Element root = getXmlDocument().getRootElement();

    String name = root.attributeValue("name");
    experiment = new JbpmSimulationExperiment(name);
    
    // read basic attributes
    String runTimeString = root.attributeValue("run-time");
    String resetTime = root.attributeValue("reset-time");
    String realStartTimeString = root.attributeValue("real-start-time");
    String timeUnitString = root.attributeValue("time-unit");
    String currencyString = root.attributeValue("currency");
    String unutilizedTimeCostFactorString = root.attributeValue("unutilized-time-cost-factor");
    
    if (currencyString!=null)
      experiment.setCurrency( currencyString );
    if (unutilizedTimeCostFactorString!=null)
      experiment.setUnutilizedTimeCostFactor(Double.parseDouble(unutilizedTimeCostFactorString));
      
    if (runTimeString!=null)
      experiment.setSimulationRunTime( Double.parseDouble(runTimeString) );
    if (resetTime!=null)
      experiment.setResetTime( Double.parseDouble(resetTime) );
    if (realStartTimeString!=null)
      experiment.setRealStartDate( realStartTimeString );
    
    if (timeUnitString!=null) {
      timeUnitString = timeUnitString.toLowerCase(); 
      if ("millisecond".equals(timeUnitString))
        experiment.setTimeUnit( JbpmSimulationExperiment.MILLISECONDS );
      else if ("second".equals(timeUnitString))
        experiment.setTimeUnit( JbpmSimulationExperiment.SECONDS );
      else if ("minute".equals(timeUnitString))
        experiment.setTimeUnit( JbpmSimulationExperiment.MINUTES );
      else if ("hour".equals(timeUnitString))
        experiment.setTimeUnit( JbpmSimulationExperiment.HOURS );
    }    
    
    // read output parameters
    Element outputElement = root.element("output");
    if (outputElement!=null) {
      String outputPathName = outputElement.attributeValue("path");
      experiment.setOutputPathName(outputPathName);
    }
    
    // read scenarios
    Iterator scenarioIterator = root.elementIterator("scenario");
    while (scenarioIterator.hasNext()) {
      Element scenarioElement = (Element) scenarioIterator.next();
      String baseScenario = scenarioElement.attributeValue("base-scenario");
      if (baseScenario!=null) {
        Element baseScenarioElement = null;
        Iterator iter2 = root.elementIterator("scenario");
        while (iter2.hasNext()) {
          Element e = (Element) iter2.next();
          if (baseScenario.equals( e.attributeValue("name") )) {
            baseScenarioElement = e;
            break;
          }
        }
        if (baseScenarioElement==null)
          throw new ExperimentConfigurationException("base scenario with name '" + baseScenario + "' does not exist");
        addScenario(
            readScenario(scenarioElement, baseScenarioElement));
      }
      else
        addScenario(
            readScenario(scenarioElement, null));
    }
    
    JbpmSimulationExperiment result = experiment;
    experiment = null;
    return result;
  }
  
  protected void addScenario(JbpmSimulationScenario scenario) {
    experiment.addScenario(scenario);
  }
  
  public static DistributionDefinition readDistribution(Element distributionElement) {
    String name = distributionElement.attributeValue("name");
    String sampleType = distributionElement.attributeValue("sample-type");
    String type = distributionElement.attributeValue("type");

    String valueText = distributionElement.attributeValue("value");
    String meanText = distributionElement.attributeValue("mean");
    String standardDeviationText = distributionElement.attributeValue("standardDeviation");
    String minText = distributionElement.attributeValue("min");
    String maxText = distributionElement.attributeValue("max");
    String nonNegativeText = distributionElement.attributeValue("nonNegative");

    boolean nonNegative = ("true".equals(nonNegativeText) || "yes".equals(nonNegativeText));

    return new DistributionDefinition( //
        name, type, sampleType, valueText, meanText, standardDeviationText, minText, maxText, nonNegative);
  }

  protected JbpmSimulationScenario readScenario(Element scenarioElement, Element baseScenarioElement) {
    String name = scenarioElement.attributeValue("name");
    JbpmSimulationScenario scenario = new JbpmSimulationScenario(name);

    String execute = scenarioElement.attributeValue("execute");
    if ("false".equalsIgnoreCase(execute)|| "no".equalsIgnoreCase(execute))
        scenario.setExecute(false);
    
    beforeScenarioRead(scenario, scenarioElement, baseScenarioElement);
    
    // TODO: Maybe a scenario just refers to a somehow "global" defined scenario
    // For the moment, only inline definition of the scenarios is supported

    if (baseScenarioElement!=null)
      readScenarioContent(baseScenarioElement, scenario);    
    
    // now read scenario, this overwrites data from base scenario
    readScenarioContent(scenarioElement, scenario);    

    afterScenarioRead(scenario, scenarioElement, baseScenarioElement);
    
    return scenario;
  }

  protected void afterScenarioRead(JbpmSimulationScenario scenario,
      Element scenarioElement, Element baseScenarioElement) {    
    // Empty, could be used to extend ExperimentReader    
  }

  protected void beforeScenarioRead(JbpmSimulationScenario scenario,
      Element scenarioElement, Element baseScenarioElement) {
    // Empty, could be used to extend ExperimentReader    
  }

  private void readScenarioContent(Element scenarioElement,
      JbpmSimulationScenario scenario) {
    // read processes
    Iterator processIterator = scenarioElement.elementIterator("sim-process");       
    while (processIterator.hasNext()) {
      Element processElement = (Element) processIterator.next();      
      readSimProcess(scenario, processElement);
    }
    
    // read distributions and maybe overwrite process definition configurations
    Iterator distributionIterator = scenarioElement.elementIterator("distribution");
    while (distributionIterator.hasNext()) {
      Element distributionElement = (Element) distributionIterator.next();
      DistributionDefinition distDef = ExperimentReader.readDistribution(distributionElement);      
      scenario.addDistribution(distDef);
    }    
    
    // read resource pools and maybe overwrite process definition configurations
    /*
     * read information of resource pools
     */
    Iterator poolElementIter = scenarioElement.elementIterator("resource-pool");
    while (poolElementIter.hasNext()) {
      Element resourcePoolElement = (Element) poolElementIter.next();

      String poolName = resourcePoolElement.attributeValue("name");
      String poolSizeText = resourcePoolElement.attributeValue("pool-size");
      Integer poolSize = new Integer(poolSizeText);
      scenario.addResourcePool(poolName, poolSize, readCostPerTimeUnit(resourcePoolElement));
    }
    
    // read business figures
    Iterator businessFigureIterator = scenarioElement.elementIterator("business-figure");
    while (businessFigureIterator.hasNext()) {
      Element figureElement = (Element) businessFigureIterator.next();
      BusinessFigure figure = new BusinessFigure(
          figureElement.attributeValue("name"),
          figureElement.attributeValue("type"),
          figureElement.attributeValue("handler"),
          figureElement.attributeValue("expression"));
      scenario.addBusinessFigure(figure);
    }     
    
    // read data source and filters
    Iterator dataSourceIterator = scenarioElement.elementIterator("data-source");
    while (dataSourceIterator.hasNext()) {
      Element dataSource = (Element) dataSourceIterator.next();
      scenario.addDataSource( //
          dataSource.attributeValue("name"), // name
          dataSource.attributeValue("handler")); // handler class name
    }     

    // read data source and filters
    Iterator dataFilterIterator = scenarioElement.elementIterator("data-filter");
    while (dataFilterIterator.hasNext()) {
      Element dataFilter = (Element) dataFilterIterator.next();
      scenario.addDataFilter( //
          dataFilter.attributeValue("name"), // name
          dataFilter.attributeValue("handler")); // handler class name
    }     
}
  
  protected Double readCostPerTimeUnit(Element resourcePoolElement) {
    String costPerTimeUnitText = resourcePoolElement.attributeValue("costs-per-time-unit");
    if (costPerTimeUnitText!=null)
      return Double.valueOf(costPerTimeUnitText);
    else
      return new Double(0);
  }  

  private void readSimProcess(JbpmSimulationScenario scenario, Element processElement) {
    String processName = processElement.attributeValue("name");
    String processPath = processElement.attributeValue("path");
    ProcessDefinition pd = null;
    
    // if in the local repository take it from there
    if (processName!=null && processDefinitionRepository.containsKey(processName)) {
      String processXml = (String) processDefinitionRepository.get(processName);
      SimulationJpdlXmlReader processReader = new SimulationJpdlXmlReader(processXml);
      pd = processReader.readProcessDefinition();
    }
    // if not the XML file should be read
    else if (processPath!=null) {
      InputSource source = new InputSource(this.getClass().getResourceAsStream(processPath));
      SimulationJpdlXmlReader processReader = new SimulationJpdlXmlReader(source);
      pd = processReader.readProcessDefinition();
    }
    else
      throw new ExperimentConfigurationException("references process (name='"+processName+"', path='"+processPath+"') in scenario '" + scenario.getName() + "' not found.");
    
    // read special overwrite statements
    Iterator processIter = processElement.elementIterator("process-overwrite");
    while(processIter.hasNext()) {
        readProcessOverwrite(pd, (Element)processIter.next());
    }
    Iterator stateIter = processElement.elementIterator("state-overwrite");
    while(stateIter.hasNext()) {
        readStateOverwrite(pd, (Element)stateIter.next());
    }
    Iterator decisionIter = processElement.elementIterator("decision-overwrite");
    while(decisionIter.hasNext()) {
        readDecisionOverwrite(pd, (Element)decisionIter.next());
    }
    Iterator taskIter = processElement.elementIterator("task-overwrite");
    while(taskIter.hasNext()) {
        readTaskOverwrite(pd, (Element)taskIter.next());
    }
    Iterator nodeIter = processElement.elementIterator("node-overwrite");
    while(nodeIter.hasNext()) {
        readNodeOverwrite(pd, (Element)nodeIter.next());
    }

    scenario.addProcessDefinition( pd );
  }

  private void readProcessOverwrite(ProcessDefinition pd, Element e) {
    String newStartDistribution = e.attributeValue("start-distribution");
    if (newStartDistribution!=null)
      ((SimulationDefinition)pd.getDefinition(SimulationDefinition.class)).setStartDistribution(newStartDistribution);

    readUseDataSourceEvent(pd.getStartState(), e, Event.EVENTTYPE_BEFORE_SIGNAL);
  }  

  private void readStateOverwrite(ProcessDefinition pd, Element e) {
    String stateName = e.attributeValue("state-name");
    String newDistribution = e.attributeValue("time-distribution");

    SimulationDefinition sd = ((SimulationDefinition)pd.getDefinition(SimulationDefinition.class));
    Node node = pd.getNode(stateName);
    
    if (newDistribution!=null)
      sd.addStateDistribution(node, newDistribution);
    
    readUseDataSourceEvent(node, e, Event.EVENTTYPE_NODE_ENTER);
    readUseDataFilterEvent(node, e, Event.EVENTTYPE_NODE_ENTER);
    readBusinessFigureEvent(node, e, Event.EVENTTYPE_NODE_ENTER);
    
    Iterator iter = e.elementIterator("transition");
    while (iter.hasNext()) {
      readTransitionOverwrite(sd, node, (Element)iter.next());
    }
  }
  
  private void readNodeOverwrite(ProcessDefinition pd, Element e) {
    String nodeName = e.attributeValue("node-name");
    Node node = pd.getNode(nodeName);

    readUseDataSourceEvent(node, e, Event.EVENTTYPE_NODE_ENTER);
    readUseDataFilterEvent(node, e, Event.EVENTTYPE_NODE_ENTER);
    readBusinessFigureEvent(node, e, Event.EVENTTYPE_NODE_ENTER);    
  }  

  private void readDecisionOverwrite(ProcessDefinition pd, Element e) {
    String decisionName = e.attributeValue("decision-name");

    SimulationDefinition sd = ((SimulationDefinition)pd.getDefinition(SimulationDefinition.class));
    Node node = pd.getNode(decisionName);
    
    Iterator iter = e.elementIterator("transition");
    while (iter.hasNext()) {
      readTransitionOverwrite(sd, node, (Element)iter.next());
    }
  }  

  private void readTaskOverwrite(ProcessDefinition pd, Element e) {
    String taskName = e.attributeValue("task-name");
    String newDistribution = e.attributeValue("time-distribution");
    
    SimulationDefinition sd = ((SimulationDefinition)pd.getDefinition(SimulationDefinition.class));
    Task task = pd.getTaskMgmtDefinition().getTask(taskName);

    if (newDistribution!=null) {
      sd.addTaskDistribution(task, newDistribution);
    }
    
    readUseDataSourceEvent(task.getTaskNode(), e, Event.EVENTTYPE_NODE_ENTER);
    readUseDataFilterEvent(task.getTaskNode(), e, Event.EVENTTYPE_NODE_ENTER);    
    readBusinessFigureEvent(task.getTaskNode(), e, Event.EVENTTYPE_NODE_ENTER);

    Iterator iter = e.elementIterator("transition");
    while (iter.hasNext()) {
      readTransitionOverwrite(sd, task.getTaskNode(), (Element)iter.next());
    }
  }
  
  private void readUseDataSourceEvent(GraphElement graphElement, Element xmlElement, String eventtype) {    
    Element e = xmlElement.element("use-data-source");
    if (e!=null) {
      Event evt = new Event(eventtype);
      UseDataSourceAction action = new UseDataSourceAction();
      action.setName( e.attributeValue("name") );
      evt.addAction( action );

      graphElement.addEvent(evt);
    }
  }

  private void readUseDataFilterEvent(GraphElement graphElement, Element xmlElement, String eventtype) {    
    Element e = xmlElement.element("use-data-filter");
    if (e!=null) {
      Event evt = new Event(eventtype);
      UseDataFilterAction action = new UseDataFilterAction();
      action.setName( e.attributeValue("name") );
      evt.addAction( action );

      graphElement.addEvent(evt);
    }
  }
  
  private void readBusinessFigureEvent(GraphElement graphElement, Element xmlElement, String eventtype) {    
    Element e = xmlElement.element("calculate-business-figure");
    if (e!=null) {
      Event evt = new Event(eventtype);
      BusinessFigureAction action = new BusinessFigureAction();
      action.setName( e.attributeValue("name") );
      evt.addAction( action );

      graphElement.addEvent(evt);
    }
  }  
  
  private void readTransitionOverwrite(SimulationDefinition sd, Node node, Element transitionElement) {
    String name = transitionElement.attributeValue("name");    
    Transition trans = node.getLeavingTransition(name);
    
    String probString = transitionElement.attributeValue( "probability" );
    if (probString!=null) {
      double prob = Double.parseDouble(probString);
      sd.addTransitionProbability(trans, prob);
    }

  }  
  
}
