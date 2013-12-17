package org.jbpm.sim.jpdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.sim.datasource.ProcessDataFilter;
import org.jbpm.sim.datasource.ProcessDataSource;
import org.jbpm.sim.def.DistributionDefinition;
import org.jbpm.sim.def.ResourceRequirement;
import org.jbpm.sim.exception.ExperimentConfigurationException;
import org.jbpm.taskmgmt.def.Task;

/**
 * this object is populated during instrumentation (see SimulationTestCase)
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SimulationDefinition extends ModuleDefinition {

  private static final long serialVersionUID = 1L;

  private static Log log = LogFactory.getLog(SimulationDefinition.class);

  // Map<Object (process elements), Object (any simulation configuration information)>
  // Map simulationConfiguration;

  /**
   * Map containing pool definitions. The pool name is the key, the value is an Object array with
   * the first element: size as Integer, and the second element: costsPerTimeUnit as Double
   */
  private Map resourcePoolDefinitions = new HashMap();

  private List distributions = new ArrayList();

  /**
   * maps for process elements which distribution to use The key is the process element, the value
   * the distribution name
   */
  private Map distributionMap = new HashMap();

  /**
   * Map which contains a list of resource requirements for process elements (for example Task's or
   * State's) It is a Map<Object, List>
   */
  private Map resourceRequirements = new HashMap();

  /**
   * Map which contains configured probabilities for Transitions It is a Map<Transition, Double>
   */
  private Map transitionProbabilities = new HashMap();

  private Map dataSources = new HashMap();

  private Map dataFilters = new HashMap();

  public ModuleInstance createInstance() {
    return new SimulationInstance(this);
  }

  /**
   * adds a resource pool. If the pool already exists, the bigger pool size is taken.
   * 
   * @param poolName
   * @param poolSize
   */
  public void addResourcePool(String poolName, Integer poolSize, Double costPerTimeUnit) {
    if (resourcePoolDefinitions.containsKey(poolName)) {
      Integer otherPoolSize = (Integer) ((Object[]) resourcePoolDefinitions.get(poolName))[0];
      if (poolSize.intValue() > otherPoolSize.intValue()) {
        resourcePoolDefinitions.put(poolName, new Object[] { poolSize, costPerTimeUnit });
        log.warn("resource pool '"
            + poolName
            + "' redefined in process '"
            + getProcessDefinition().getName()
            + "' with the bigger poolsize "
            + poolSize
            + ", was "
            + otherPoolSize
            + " before");
      }
      else if (poolSize.intValue() < otherPoolSize.intValue())
        log.warn("resource pool '"
            + poolName
            + "' redefined in process '"
            + getProcessDefinition().getName()
            + "' with the smaler poolsize "
            + poolSize
            + " which is ignored. Poolsize still is "
            + otherPoolSize);
    }
    else
      resourcePoolDefinitions.put(poolName, new Object[] { poolSize, costPerTimeUnit });
  }

  /**
   * adds a resource pool. If the pool already exists it is overwritten
   * 
   * @param poolName
   * @param poolSize
   */
  public void overwriteResourcePool(String poolName, Integer poolSize, Double costPerTimeUnit) {
    resourcePoolDefinitions.put(poolName, new Object[] { poolSize, costPerTimeUnit });
  }

  public void addResourceRequirement(Object processElement, String poolName, int amount) {
    List reqList = (List) resourceRequirements.get(processElement);
    if (reqList == null) {
      reqList = new ArrayList();
      resourceRequirements.put(processElement, reqList);
    }
    reqList.add(new ResourceRequirement(poolName, amount));
  }

  /**
   * adds a distribution.
   * 
   * @param distDef
   */
  public void addDistribution(DistributionDefinition distDef) {
    distributions.add(distDef);
  }

  /**
   * adds a distribution but delets all distribution definitions with the given name
   * 
   * @param distDef
   */
  public void overwriteDistribution(DistributionDefinition distDef) {
    // delete all distributions with that name
    for (Iterator iterator = new ArrayList(distributions).iterator(); iterator.hasNext();) {
      DistributionDefinition dd = (DistributionDefinition) iterator.next();
      if (dd.getName().equals(distDef.getName())) distributions.remove(dd);
    }
    // and add the new one
    distributions.add(distDef);
  }

  public List getDistributions() {
    return distributions;
  }

  private void addToDistributionMap(Object key, String distributionName) {
    distributionMap.put(key, distributionName);
  }

  public void setStartDistribution(String distributionName) {
    addToDistributionMap(processDefinition, distributionName);
  }

  public void addTaskDistribution(Task task, String distributionName) {
    addToDistributionMap(task, distributionName);
  }

  public void addStateDistribution(Node state, String distributionName) {
    addToDistributionMap(state, distributionName);
  }

  public Map getDistributionMap() {
    return distributionMap;
  }

  public Map getResourceRequirements() {
    return resourceRequirements;
  }

  public void addTransitionProbability(Transition trans, double prob) {
    transitionProbabilities.put(trans, new Double(prob));
  }

  public Map getTransitionProbabilities() {
    return transitionProbabilities;
  }

  public Map getResourcePoolDefinitions() {
    return resourcePoolDefinitions;
  }

  public void setResourcePoolDefinitions(Map resourcePoolDefinitions) {
    this.resourcePoolDefinitions = resourcePoolDefinitions;
  }

  public void addDataSource(String name, ProcessDataSource src) {
    dataSources.put(name, src);
  }

  public ProcessDataSource getDataSource(String name) {
    ProcessDataSource result = (ProcessDataSource) dataSources.get(name);
    if (result == null)
      throw new ExperimentConfigurationException("data source '" + name + "' not configured");
    return result;
  }

  public void addDataFilter(String name, ProcessDataFilter filter) {
    dataFilters.put(name, filter);
  }

  public ProcessDataFilter getDataFilter(String name) {
    ProcessDataFilter result = (ProcessDataFilter) dataFilters.get(name);
    if (result == null)
      throw new ExperimentConfigurationException("data filter '" + name + "' not configured");
    return result;
  }
}
