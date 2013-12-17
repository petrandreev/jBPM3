package org.jbpm.sim.jpdl;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.dom4j.Element;
import org.xml.sax.InputSource;

import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.NodeCollection;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.node.State;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.jpdl.xml.ProblemListener;
import org.jbpm.sim.action.ProcessEndAction;
import org.jbpm.sim.action.ProcessStartAction;
import org.jbpm.sim.action.StartTaskAndPlanCompletion;
import org.jbpm.sim.def.DistributionDefinition;
import org.jbpm.sim.exe.ExperimentReader;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

/**
 * The SimulationJpdXmlReader is used instead of the original 
 * <code>org.jbpm.jpdl.xml.JpdlXmlReader</code>
 * from jBPM when using jBPM as simulation engine.
 * 
 * This reader instruments the process with additional things needed for simulation
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SimulationJpdlXmlReader extends JpdlXmlReader {

  private static final long serialVersionUID = 1L;
  
  public SimulationJpdlXmlReader(String processXml) {
    this(new InputSource(new StringReader(processXml)));    
  }
  
  public SimulationJpdlXmlReader(InputSource inputSource) {
    this(inputSource, null);
  }

  public SimulationJpdlXmlReader(Reader reader) {
    this(new InputSource(reader));
  }

  public SimulationJpdlXmlReader(InputSource inputSource, ProblemListener problemListener) {
    super(inputSource, problemListener);
  }

  public ProcessDefinition readProcessDefinition() {
    ProcessDefinition result = super.readProcessDefinition();
    instrument();
    return result;
  }
  
  /**
   * @return SimulationDefinition of process definition which is currently build.
   *         it is constructed on the fly, if it is not available
   */
  private SimulationDefinition getSimulationDefinition() {
    SimulationDefinition simulationDefinition = (SimulationDefinition) processDefinition.getDefinition(SimulationDefinition.class);
    if (simulationDefinition==null) {
      simulationDefinition = new SimulationDefinition();
      processDefinition.addDefinition(simulationDefinition);      
    }
    return simulationDefinition;
  }
  
  /**
   *  during instrumentation, you can:
   *  1) extract extra simulation information from the process xml and 
   *     store it in the simulation definition
   *  2) add event listeners to the process elements in processDefinition
   *  3) modify the whole processDefinition as you want
   */
  public void instrument() {    
    Element rootElement = super.document.getRootElement();
    SimulationDefinition simulationDefinition = getSimulationDefinition();
    
    /*
     * read information of resource pools
     */
    Iterator poolElementIter = rootElement.elementIterator("resource-pool");
    while (poolElementIter.hasNext()) {
      Element resourcePoolElement = (Element) poolElementIter.next();

      String poolName = resourcePoolElement.attributeValue("name");
      String poolSizeText = resourcePoolElement.attributeValue("pool-size");
      Integer poolSize = new Integer(poolSizeText);
      Double costPerTimeUnit = readCostPerTimeUnit(resourcePoolElement);
      
      simulationDefinition.addResourcePool(poolName, poolSize, costPerTimeUnit);
    }

    /*
     * swimlanes can serve as resource pools
     */
    Iterator swimlaneElementIter = rootElement.elementIterator("swimlane");
    while (swimlaneElementIter.hasNext()) {
      Element swimlaneElement = (Element) swimlaneElementIter.next();
      if (swimlaneElement.attributeValue("pool-size")!=null) {
        String poolName = swimlaneElement.attributeValue("name");
        String poolSizeText = swimlaneElement.attributeValue("pool-size");
        Integer poolSize = new Integer(poolSizeText);
        Double costPerTimeUnit = readCostPerTimeUnit(swimlaneElement);

        simulationDefinition.addResourcePool(poolName, poolSize, costPerTimeUnit);
      }
    }
    
    /*
     * read information of distributions
     */
    Iterator distributionIterator = rootElement.elementIterator("distribution");
    while (distributionIterator.hasNext()) {
      Element distributionElement = (Element) distributionIterator.next();
      DistributionDefinition distDef = ExperimentReader.readDistribution(distributionElement);      
      simulationDefinition.addDistribution(distDef);
    }
    
    /*
     * Events
     */
    // listen to all task assign events
    Event taskAssignEvent = new Event(Event.EVENTTYPE_TASK_CREATE);
    processDefinition.addEvent(taskAssignEvent);
    taskAssignEvent.addAction(new StartTaskAndPlanCompletion());

    // listen to all process start events to record count
    Event processStartEvent = new Event(Event.EVENTTYPE_BEFORE_SIGNAL);    
    processDefinition.getStartState().addEvent(processStartEvent);
    processStartEvent.addAction(new ProcessStartAction());

    // listen to all process end events to record cycle times of the process
    Event processEndEvent = new Event(Event.EVENTTYPE_PROCESS_END);    
    processDefinition.addEvent(processEndEvent);
    processEndEvent.addAction(new ProcessEndAction());
    
    
    /*
     * distribution usages
     */
    // process start distribution
    simulationDefinition.setStartDistribution( rootElement.attributeValue("start-distribution") );   
  }

  private Double readCostPerTimeUnit(Element resourcePoolElement) {
    String costPerTimeUnitText = resourcePoolElement.attributeValue("costs-per-time-unit");
    if (costPerTimeUnitText!=null)
      return Double.valueOf(costPerTimeUnitText);
    else
      return new Double(0);
  }

  public Task readTask(Element taskElement, TaskMgmtDefinition taskMgmtDefinition, TaskNode taskNode) {
    Task task = super.readTask(taskElement, taskMgmtDefinition, taskNode);
    
    // read distribution for task
    String distributionName = taskElement.attributeValue("time-distribution");
    getSimulationDefinition().addTaskDistribution(task, distributionName);
    
    // ONE resource from the pool named like the swimlane is always needed, if a swimlane is configured
    if (task.getSwimlane()!=null)
      getSimulationDefinition().addResourceRequirement(task, task.getSwimlane().getName(), 1);

    // read additional resource requirements
    readResourceUsages(taskElement, task);
    
    return task;
  }

  public void readNode(Element nodeElement, Node node, NodeCollection nodeCollection) {
    super.readNode(nodeElement, node, nodeCollection);

    // only instrument states
    if (State.class.isAssignableFrom(node.getClass())) {      
      String distributionName = nodeElement.attributeValue("time-distribution");
      getSimulationDefinition().addStateDistribution(node, distributionName);

      readResourceUsages(nodeElement, node);
    }
    // TODO: Later implement a resource requirement for whole TaskNode
    //       valid for all Tasks in it (but consumed only one for all Tasks
    //    else if (TaskNode.class.isAssignableFrom(node.getClass())) {    
    //       readResourceUsages(nodeElement, node);
    //     }
  }

  private void readResourceUsages(Element xmlElement, Object processElement) {
    Iterator iter = xmlElement.elementIterator("resource-needed");
    while (iter.hasNext()) {
      Element resourceElement = (Element) iter.next();
      String poolName = resourceElement.attributeValue("pool");
      String amountText = resourceElement.attributeValue("amount");

      int amount = 1;
      if (amountText!=null)
        amount = Integer.parseInt(amountText);
      
      getSimulationDefinition().addResourceRequirement(processElement, poolName, amount);
    }
  }

  public Transition resolveTransitionDestination(Element transitionElement, Node node) {
    Transition trans = super.resolveTransitionDestination(transitionElement, node);
    
    // read probabilities for outgoing transitions 
    String probString = transitionElement.attributeValue( "probability" );
    if (probString!=null) {
      double prob = Double.parseDouble(probString);
      getSimulationDefinition().addTransitionProbability(trans, prob);
    }
    
    return trans;
  }
  
}
