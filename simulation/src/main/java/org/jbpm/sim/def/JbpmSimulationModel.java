package org.jbpm.sim.def;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.EndState;
import org.jbpm.sim.SimulationConstants;
import org.jbpm.sim.entity.ResourceUsingEntity;
import org.jbpm.sim.event.ProcessStartEventGenerator;
import org.jbpm.sim.exception.ExperimentConfigurationException;
import org.jbpm.sim.jpdl.SimulationDefinition;
import org.jbpm.sim.kpi.BusinessFigure;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

import desmoj.core.dist.Distribution;
import desmoj.core.dist.IntDist;
import desmoj.core.dist.RealDist;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.Queue;
import desmoj.core.simulator.SimTime;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;
import desmoj.core.statistic.TimeSeries;

/**
 * The main model, which is responsible to set up the 
 * environment for simulation runs. 
 * 
 * @author bernd.ruecker@camunda.com
 */
public abstract class JbpmSimulationModel extends Model {

  private static Log log = LogFactory.getLog(JbpmSimulationModel.class);

  /**
   * distributions for
   * <ul>
   * <li> ending States
   * <li> ending TaskInstances
   * </ul>
   * every distribution is identified by a name. If you add more than one
   * distribution with the same name of the experiment, a
   * <code>org.jbpm.sim.exception.ExperimentConfigurationException</code> is thrown.
   */
  private Map distributions = new HashMap();

  /**
   * Map of all resource pools (which include all DESMOJ stuff needed for 
   * a resource pool).
   * It's a Map<String, ResourcePool>
   */
  private Map resourcePools = new HashMap(); 

  /**
   * Map of all Tallies recording wait time for entities
   * (which are in our case Tasks, States, ...)
   * 
   * It's a Map<String, Histogram>
   */
  private Map entityWaitTallies = new HashMap(); 
  
  /**
   * the map has one entry for every process, containing the used distribution name
   * if null, no start distribution is used (which means: never schedule start event)
   * 
   * It's a Map<String, String>
   */
  // private Map processStartDistributions = new HashMap();
  
  /**
   * maps for process elements which distribution to use
   * The key is the process element, the value the distribution name
   */
  private Map distributionMap = new HashMap();
  
  /**
   * configurations for choosing the leaving transition
   * 
   * It is a Map<Node, LeavingTransitionProbabilityConfiguration>
   */
  private Map leavingTransitionConfigurations = new HashMap();  
  
  /**
   * Map which contains a list of resource requirements for 
   * process elements (for example Task's or State's)
   * 
   * It is a Map<Object, List>
   */
  private Map resourceRequirements = new HashMap();
  
  /**
   * Map which contains a tallies to report process cycle times on it

   * It is a Map<ProcessDefinition, Tally>
   */
  private Map processCycleTimeTallies = new HashMap();

  /**
   * Map with counter for every possible process end state
   */
  private Map processEndStateCounts = new HashMap();

  /**
   * Map with counter of started instances for every
   * possible process definition
   */
  private Map processStartCounts = new HashMap();

  /**
   * This map remembers which elements were the source
   * for a name used in DESMOJ. Because DESMOJ can only use 
   * Strings, but we may want to know the real source, like a 
   * a ProcessDefinition or whatever, later, maybe during the reporting. 
   */
  private Map nameRegistry = new HashMap();  
  
  private ArrayList endedProcessInstances = new ArrayList();
  
  /**
   * configures if a local history of ended process instances
   * is kept. useful to lokk at them later, if that makes sense
   */
  private boolean rememberEndedProcessInstances = false;
  
  /**
   * map for registered business figure calculators
   */
  private Map businessFigures = new HashMap();

  public JbpmSimulationModel(Model owner, String name) {
    super(owner, name, true, true);
        
    // "install" the UserCode Intercepter (TODO: may not needed, delete then)
    // UserCodeInterceptorConfig.setUserCodeInterceptor(new SimulationUserCodeInterceptor());
    
    // load special configuration for simulation run
    // JbpmConfiguration.getInstance("org/jbpm/sim/simulation.cfg.xml");    
  }

  public JbpmSimulationModel() {
    this(null, "JBoss jBPM simulation model");
  } 
  
  public void connectToExperiment(Experiment exp) {
    super.connectToExperiment(exp);

    // set the jBPM-Clock and register it to be controlled by the real simulation clock
    // TODO: Only do that, if the clock isn't already created, e.g. by JbpmExperiment
    JbpmSimulationClock jbpmClock =
      (JbpmSimulationClock) Configs.getObject("jbpm.date.generator");
    getExperiment().getSimClock().addObserver(jbpmClock);
  }
  
  public void init() {
    initResourcePools();
    initDistributions();
    initDistributionUsages();
    initResourceRequirements();
    initTransitionDistributions();
  }
  
  public void doInitialSchedules() {
    ProcessDefinition[] definitions = getProcessDefinitions();
    for (int i = 0; i < definitions.length; i++) {
      SimTime processStartTime = getProcessStartTime(definitions[i]);
      
      if (processStartTime==null) {
        log.debug("process '" + definitions[i].getName() + "' has no start event distribution configured, it will not be started by the simulation framework");
      }
      else {
        log.debug("process '" + definitions[i].getName() + "' has a start event distribution configured and will be started by the simulation framework. The first start is at model time " + processStartTime);
      
        ProcessStartEventGenerator generator =
          new ProcessStartEventGenerator(this, definitions[i]);
  
        // schedule the first process start
        // TODO: Check WHY it starts at sim time 0.0
        generator.schedule(processStartTime);
      }
    }
  } 
  
  public String description() {
    return "jBPM-Simulation"; 
  }

  /**
   * This is implemented by the concrete implementation of the model
   */
  public abstract ProcessDefinition[] getProcessDefinitions();

  public Distribution getDistribution(String name) {
    return (Distribution) distributions.get(name);
  }
  
  /**
   * @return true, if probabilities for leaving transitions are configured for the given node
   */
  public boolean hasLeavingTransitionProbabilitiesConfigured(Node node) {
    return leavingTransitionConfigurations.containsKey(node);
  }
  
  /**
   * figures out which leaving transition should be used for the
   * given node, using the configured probabilities.
   * 
   * If no probabilities for this node are configured, 
   * the default transition is returned
   * 
   * @return leaving transition to take or the default transition,
   *         if no probabilities are configured
   */
  public Transition getLeavingTransition(Node node) {
    LeavingTransitionProbabilityConfiguration conf = 
      (LeavingTransitionProbabilityConfiguration) leavingTransitionConfigurations.get(node);
    
    if (conf==null) {
      log.debug("No transition probabilities configured for " + node + ", taking default transition");
      return node.getDefaultLeavingTransition();
    }
    else {
      Transition transition = conf.decideOutgoingTransition();
      log.debug("Simulation engine decided for leaving " + transition + " for " + node);
      return transition;
    }
  }
  
  /**
   * Returns the time, needed to work on the task with the given id
   * The time is queried from the configured distribution
   * 
   * @return the simulation time when the task should be finished
   */
  public SimTime getTaskWorkingTime(Task task) {
    return getNextSimTimeWithDistributionMap(task);
  }
  
  public SimTime getStateWorkingTime(Node state) {
    return getNextSimTimeWithDistributionMap(state);
  }
  
  public SimTime getProcessStartTime(ProcessDefinition processDefinition) {
    return getNextSimTimeWithDistributionMap(processDefinition);
  }
  
  protected SimTime getNextSimTimeWithDistributionMap(Object key) {
    String distributionName = (String) distributionMap.get(key);
    if (distributionName==null) {
      log.warn("no distribution configured for element '" + key + "'");
      return null;
    }

    Distribution dist = (Distribution) distributions.get(distributionName);
    if (dist==null)
      throw new ExperimentConfigurationException("Distribution with name '" + distributionName + "' configured as event distribution for element '" + key + "' is not defined.");
    
    SimTime result = getSimTimeFromDistribution(dist);
    log.debug("generated sim time "+result+" with distribution for element '" + key + "'");
    return result;
  } 
  
  private SimTime getSimTimeFromDistribution(Distribution dist) {
    if (IntDist.class.isAssignableFrom(dist.getClass()))
      return new SimTime( getPositiveLong(((IntDist)dist).sample()) );
    else if (RealDist.class.isAssignableFrom(dist.getClass()))
      return new SimTime( getPositiveDouble(((RealDist)dist).sample()) );
    else
      throw new ExperimentConfigurationException("Distribution class " + dist.getClass().getName() + " can not be used to construct time events");
  }
  
  private double getPositiveDouble(double d) {
    if (d<0)
      return (-1) * d;
    return d;
  }

  private long getPositiveLong(long l) {
    if (l<0)
      return (-1) * l;
    return l;
  }
  
  public TimeSeries[] getResourceTimeSeries() {
    TimeSeries[] result = new TimeSeries[ resourcePools.size() ];
    int i=0;
    for (Iterator iterator = resourcePools.values().iterator(); iterator.hasNext();) {
      ResourcePool rp = (ResourcePool) iterator.next();
      result[i] = rp.getAvailableResourceTimeSeries();
      i++;
    }
    return result;
  }
  
  public void addResourcePool(String poolName, int capacity, double costPerTimeUnit) {
    // create pool
    ResourcePool pool = new ResourcePool(this, poolName, capacity, costPerTimeUnit);
    
//    // and build an observer to it
//    Observer plotter = new Observer() {
//      public void update(Observable obs, Object value) {
//        log.debug("YYYYY " + 
//         availableResourceTimeSeries.getDataValues().toArray().length + " | " +
//         availableResourceTimeSeries.getTimeValues().toArray().length);
//         log.debug("XXXXXXX " + obs + ": " + value);
//      }
//    };
//    pool.getAvailableResourceTimeSeries().connectToPlotter(plotter);

    resourcePools.put(//
        poolName, //
        pool);
    resourceUsageChanged(poolName);
  }
  
  public String[] getResourcePoolNames() {
    return (String[]) resourcePools.keySet().toArray(new String[0]);
  }

  public ResourcePool getResourcePool(String poolName) {
    ResourcePool resourcePool =(ResourcePool) resourcePools.get(poolName);
    if (resourcePool == null) {
      throw new RuntimeException("pool " + poolName + " is not defined");
    }
    return resourcePool;
  }
  
  public void resourceUsageChanged(String poolName) {
    TimeSeries ts = getResourcePool(poolName).getAvailableResourceTimeSeries();
    ts.update( // 
        getResourcePool(poolName).getAvailableResources() );
  }

  /**
   * @return the resource pool as Queue
   */
  public Queue getResourcePoolQueue(String poolName) {
    return getResourcePool(poolName).getPool();
  }

  /**
   * @return the resource pool time series
   */
  public TimeSeries getResourcePoolTimeSeries(String poolName) {
    return getResourcePool(poolName).getAvailableResourceTimeSeries();
  }
  
  /**
   * returns a queue for a resource pool (needed if no resource is available)
   * the queues are constructed on the fly.
   * 
   * @return the queue for the resource pool
   */
  public Queue getResourceQueue(String poolName) {
    Queue resourceQueue = getResourcePool(poolName).getResourceQueue();
    if (resourceQueue == null) {
      throw new RuntimeException("pool " + poolName + " is not defined");
    }
    return resourceQueue;
  }

  public void checkWaitingQueue(String poolName) {
    Queue q = getResourceQueue(poolName);
    if (!q.isEmpty()) {
      // okay, somebody is waiting
      ResourceUsingEntity e = (ResourceUsingEntity) q.first();

      // tell this entity the resource is available
      if (e.resourceReleased(poolName))
        // and remove if, if the entity could successfully acquire the resource 
        q.remove(e);
    }
  }
  
  public String formatTaskInstance(TaskInstance taskInstance) {
    return taskInstance.toString();
  }
  
  public ResourceRequirement[] getResourceRequirements(Object processElement) {
    List reqList = (List) resourceRequirements.get(processElement);
    if (reqList==null)
      return new ResourceRequirement[0];
    else
      return (ResourceRequirement[]) reqList.toArray(new ResourceRequirement[0]);
  }

  public Tally getResourceWaitTimeTally(String poolName) {
    return getResourcePool(poolName).getWaitTimeTally();
  }

  public Tally getResourceWorkTimeTally(String poolName) {
    return getResourcePool(poolName).getWorkTimeTally();
  }

  public Tally getEntityWaitTimeTally(Object obj) {  
    if (!entityWaitTallies.containsKey(obj)) {
      log.debug("Lazy initializing Histogram for Entity '" + obj + "'");
      // @TODO: Think about an automatically adjusting Histogram (lower & upper end)
//      Histogram h = new Histogram( this, // owning model
//          "Waiting before " + name, // name
//          0, // lower end
//          500, // upper end
//          10, // cells
//          true, // show in report
//          false);
      Tally result = new Tally(this, // owning model
          buildName(obj, SimulationConstants.NAME_PREFIX_WAITING_BEFORE_STATE, SimulationConstants.NAME_SUFFIX_WAITING_BEFORE_STATE), // name
          true, // show in report
          false); // show in trace
      result.reset();
      entityWaitTallies.put(//
          obj, //
          result); // show in trace
      return result;
    }
    return (Tally) entityWaitTallies.get(obj);
  }

  private void initResourcePools() {
    HashMap requiredPools = new HashMap();
    HashMap resourceCosts = new HashMap();
    
    for (int i = 0; i < getProcessDefinitions().length; i++) {
      SimulationDefinition simulationDefinition = 
        (SimulationDefinition) getProcessDefinitions()[i].getDefinition(SimulationDefinition.class);
      
      for (Iterator iterator = simulationDefinition.getResourcePoolDefinitions().keySet().iterator(); iterator.hasNext();) {
        String poolName = (String) iterator.next();
        
        Object[] def = (Object[]) simulationDefinition.getResourcePoolDefinitions().get(poolName);
        Integer poolSize = (Integer) def[0];
        Double costs = (Double) def[1];
        
        if (requiredPools.containsKey(poolName)) {
          Integer otherPoolSize = (Integer) requiredPools.get(poolName);
          if (poolSize.intValue() > otherPoolSize.intValue() ) {
            requiredPools.put(poolName, poolSize);
            resourceCosts.put(poolName, costs);
            log.warn("resource pool '" + poolName + "' redefined in process '"+getProcessDefinitions()[i].getName()+"' with the bigger poolsize " + poolSize + ", was " + otherPoolSize + " before" );
          }
          else if (poolSize.intValue() < otherPoolSize.intValue() ) {
            log.warn("resource pool '" + poolName + "' redefined in process '"+getProcessDefinitions()[i].getName()+"' with the smaler poolsize " + poolSize + " which is ignored. Poolsize still is " + otherPoolSize);
          }
        }
        else {
          requiredPools.put(poolName, poolSize);
          resourceCosts.put(poolName, costs);
        }
      }
    }

    // this is only allowed AFTER the connection is made!
    for (Iterator iterator = requiredPools.keySet().iterator(); iterator.hasNext();) {
      String poolName = (String) iterator.next();
      Integer capacity = (Integer) requiredPools.get(poolName);
      Double costs = (Double) resourceCosts.get(poolName);
      addResourcePool(poolName, capacity.intValue(), costs.doubleValue());
    }
  }
  
  private void initDistributions() {
    for (int i = 0; i < getProcessDefinitions().length; i++) {
      SimulationDefinition simulationDefinition = 
        (SimulationDefinition) getProcessDefinitions()[i].getDefinition(SimulationDefinition.class);
      
      for (Iterator iterator = simulationDefinition.getDistributions().iterator(); iterator.hasNext();) {
        DistributionDefinition distDef = (DistributionDefinition) iterator.next();       
        
        // TODO: improve to check, if the definition is the same (then this distribution can be just ignored)
        if (distributions.containsKey(distDef.getName()))
          throw new RuntimeException("duplicate definition of distribution '" + distDef.getName() + "'");
        
        distributions.put(distDef.getName(), distDef.createDistribution(this));
      }
    }
  }
    
  private void initDistributionUsages() {
    for (int i = 0; i < getProcessDefinitions().length; i++) {
      SimulationDefinition simulationDefinition = 
        (SimulationDefinition) getProcessDefinitions()[i].getDefinition(SimulationDefinition.class);
      distributionMap.putAll( 
          simulationDefinition.getDistributionMap());
    }
  }

  private void initTransitionDistributions() {
    for (int i = 0; i < getProcessDefinitions().length; i++) {
      SimulationDefinition simulationDefinition = 
        (SimulationDefinition) getProcessDefinitions()[i].getDefinition(SimulationDefinition.class);
      
      for (Iterator iterator = simulationDefinition.getTransitionProbabilities().keySet().iterator(); iterator.hasNext();) {
        Transition trans = (Transition) iterator.next();
        double probability = ((Double)simulationDefinition.getTransitionProbabilities().get(trans)).doubleValue();
        
        // now check for every node, if we have already started a configuration
        // if not, create it. The configuration creates correct ranges for every 
        // transition with configured probability
        if (leavingTransitionConfigurations.get(trans.getFrom())==null) {
          leavingTransitionConfigurations.put( //
              trans.getFrom(), 
              new LeavingTransitionProbabilityConfiguration(trans.getFrom(), trans, probability));          
        }
        else {
          LeavingTransitionProbabilityConfiguration conf = 
            (LeavingTransitionProbabilityConfiguration) leavingTransitionConfigurations.get(trans.getFrom());
          conf.addTransition(trans, probability);                    
        }
      }
    }
    
    // now we registered all nodes and transitions, so we now the probability sum
    // so we can create all distributioons now
    for (Iterator iterator = leavingTransitionConfigurations.values().iterator(); iterator.hasNext();) {
      LeavingTransitionProbabilityConfiguration conf = (LeavingTransitionProbabilityConfiguration) iterator.next();
      conf.createDistribution(this);
    }
  }

  private void initResourceRequirements() {
    for (int i = 0; i < getProcessDefinitions().length; i++) {
      SimulationDefinition simulationDefinition = 
        (SimulationDefinition) getProcessDefinitions()[i].getDefinition(SimulationDefinition.class);
      resourceRequirements.putAll( 
          simulationDefinition.getResourceRequirements());
    }
  }

  public void reportProcessInstanceCycleTime(ProcessDefinition pd, double duration) {
    Tally tally = (Tally) processCycleTimeTallies.get(pd);
    if (tally==null) {
      tally = new Tally( this, //
          buildName(pd, SimulationConstants.NAME_PREFIX_PROCESS_CYCLE_TIME, SimulationConstants.NAME_SUFFIX_PROCESS_CYCLE_TIME), // name
          true, // show in report
          false); // show in trace
      tally.reset();
      processCycleTimeTallies.put(pd, tally);
    }
    tally.update(duration);
  }

  public void reportProcessEndState(EndState node) {
    Count count = (Count) processEndStateCounts.get(node);
    if (count==null) {
      count = new Count( this, //
          buildName(node, SimulationConstants.NAME_PREFIX_PROCESS_END_STATE + node.getProcessDefinition().getName() + " | ", SimulationConstants.NAME_SUFFIX_PROCESS_END_STATE), // name
          true, // show in report
          false); // show in trace
      count.reset();
      processEndStateCounts.put(node, count);
    }
    count.update();
  }
  
  public void reportProcessStart(ProcessDefinition processDefinition) {
    Count count = (Count) processStartCounts.get(processDefinition);
    if (count==null) {
      count = new Count( this, //
          buildName(processDefinition, SimulationConstants.NAME_PREFIX_PROCESS_START + processDefinition.getName() + " | ", SimulationConstants.NAME_SUFFIX_PROCESS_START), // name
          true, // show in report
          false); // show in trace
      count.reset();
      processStartCounts.put(processDefinition, count);
    }
    count.update();    
  }  
  
  public String buildName(Object o, String prefix, String sufix) {
    Object propertyName = getShortNameForObject(o);
    String name = prefix + propertyName + sufix;
    nameRegistry.put(name, o);
    return name;
  }

  private String getShortNameForObject(Object o) {
    if (o==null)
      return null;
    
    Object propertyName = o;
    if (PropertyUtils.isReadable(o, "name")) {
      try {
        propertyName = PropertyUtils.getProperty(o, "name");
      }
      catch (Exception e) {
      }
    }
    if (propertyName==null)
      return null;
    else
      return propertyName.toString();
  }
  
  public String getShortNameFor(String fullName) {
    return getShortNameForObject( getSourceElementForName(fullName) );
  }
  
  public Object getSourceElementForName(String fullName) {
    return nameRegistry.get(fullName);
  }

  /**
   * remember that process instance has run,
   * if configured to remember it
   */
  public void reportFinishedProcessInstance(ProcessInstance processInstance) {
    if (isRememberEndedProcessInstances())
      endedProcessInstances.add(processInstance);
  }

  public boolean isRememberEndedProcessInstances() {
    return rememberEndedProcessInstances;
  }

  public void setRememberEndedProcessInstances(
      boolean rememberEndedProcessInstances) {
    this.rememberEndedProcessInstances = rememberEndedProcessInstances;
  }

  public ArrayList getEndedProcessInstances() {
    return endedProcessInstances;
  }
  
  public void addBusinessFigure(BusinessFigure conf) {
    businessFigures.put(conf.getName(), conf);
  }

  public BusinessFigure getBusinessFigure(String name) {
    return (BusinessFigure) businessFigures.get(name);
  }

  public Collection getBusinessFigures() {
    return businessFigures.values();
  }
  
  public Collection getBusinessFigureTypes() {
    ArrayList result = new ArrayList();
    for (Iterator iterator = getBusinessFigures().iterator(); iterator.hasNext();) {
      BusinessFigure figure = (BusinessFigure) iterator.next();
      if (!result.contains( figure.getType() )) {
        result.add(figure.getType());
      }
    }
    return result;
  }

  public double getBusinessFigureSum(String businessFigureType) {
    double result = 0;
    for (Iterator iterator = getBusinessFigures().iterator(); iterator.hasNext();) {
      BusinessFigure figure = (BusinessFigure) iterator.next();
      if (figure.getType().equals( businessFigureType )) {
        result += figure.getResult();
      }
    }
    return result;
  }

}
