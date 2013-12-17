package org.jbpm.sim.def;

import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.sim.SimulationConstants;
import org.jbpm.sim.entity.ResourceEntity;

import desmoj.core.simulator.Queue;
import desmoj.core.simulator.QueueBased;
import desmoj.core.simulator.SimTime;
import desmoj.core.statistic.Tally;
import desmoj.core.statistic.TimeSeries;

public class ResourcePool {
  
  private static Log log = LogFactory.getLog(ResourcePool.class);
  
  private String poolName;
  
  /**
   * cost of this resource per time unit
   */
  private double costPerTimeUnit;
  
  /**
   * resource-pools, which are simple queues in the DESMOJ vocabulary
   * It's a Map<String, Queue>
   */
  private Queue pool;
  
  /**
   * A map for recording numbers of "used" resources per resource pool
   * 
   *  it's a Map<String, TimeSeries>
   */
  private TimeSeries availableResourceTimeSeries;

  /**
   * waiting queues, used to queue up for some time if no resource is available
   */
  private Queue resourceQueue;
  
  /**
   * The Histogram records all waiting times for this resource
   */
  private Tally waitTimeTally;
  
  /**
   * The Histogram records all working times for this resource
   * TODO: I think this is senseless, but lets see
   */
  private Tally workTimeHistogram;
  
  public ResourcePool(JbpmSimulationModel owner, String poolName, int capacity, double costPerTimeUnit) {
    log.debug("add new resource pool '" + poolName + "' with capacity " + capacity + " and costs " + costPerTimeUnit);
    this.poolName = poolName;
    this.costPerTimeUnit = costPerTimeUnit;
    
    pool = new Queue(owner, // model which owns the queue
        owner.buildName(this, SimulationConstants.NAME_PREFIX_RESOURCE_POOL, SimulationConstants.NAME_SUFFIX_RESOURCE_POOL), // name
        QueueBased.FIFO, //
        capacity, // 0=unlimited capacity
        true, // show in report
        true); // show in trace
    
    // fill queue with ready to use resources
    for (int i = 0; i < capacity; i++) {
      pool.insert( new ResourceEntity(owner, "Resource from " + poolName) );
    }    
    
    // add the queue for waiting for a resource    
    resourceQueue = new Queue(owner, // model which owns the queue
        owner.buildName(this, SimulationConstants.NAME_PREFIX_RESOURCE_QUEUE, SimulationConstants.NAME_SUFFIX_RESOURCE_QUEUE), // name
        QueueBased.FIFO, //
        0, // unlimited capacity
        true, // show in report
        true); // show in trace
    
    // add the time series for recording number of used resources
    availableResourceTimeSeries = new TimeSeries(owner, 
        owner.buildName(this, SimulationConstants.NAME_PREFIX_RESOURCE_USAGE_TIMESERIES, SimulationConstants.NAME_SUFFIX_RESOURCE_USAGE_TIMESERIES), // name 
        new SimTime(0.0), // start 
        new SimTime(Double.MAX_VALUE), // stop
        false); 
    availableResourceTimeSeries.connectToPlotter(new Observer() {
      public void update(Observable observable, Object o) {
        // the TimeSeries does nothing if no observer is connected, so just connect a dummy
      }
    });
    availableResourceTimeSeries.reset();
    
//    waitTimeHistogram = new Histogram( owner, //
//        "Waiting times for " + poolName, // name
//        0, // lower end
//        50, // upper end
//        25, // cells
//        true, // show in report
//        false); // show in trace
    waitTimeTally = new Tally( owner, //
        owner.buildName(this, SimulationConstants.NAME_PREFIX_WAITING_FOR_RESOURCE, SimulationConstants.NAME_SUFFIX_WAITING_FOR_RESOURCE), // name
        false, // show in report, TODO: activate in report as soon it is used correctly
        false); // show in trace
    waitTimeTally.reset();
  }

  public Queue getPool() {
    return pool;
  }

  public TimeSeries getAvailableResourceTimeSeries() {
    return availableResourceTimeSeries;
  }

  public Queue getResourceQueue() {
    return resourceQueue;
  }

  public Tally getWaitTimeTally() {
    return waitTimeTally;
  }

  public Tally getWorkTimeTally() {
    return workTimeHistogram;
  }

  public String getName() {
    return getPoolName();
  }
  
  public String getPoolName() {
    return poolName;
  }
  
  public int getCapacity() {
    return pool.getQueueLimit();
  }
  
  public int getAvailableResources() {
    return pool.length();
  }
  
  public String toString() {
    return "ResourcePool(" + poolName + ")";
  }

  public double getCostPerTimeUnit() {
    return costPerTimeUnit;
  }

  public void setCostPerTimeUnit(double costPerTimeUnit) {
    this.costPerTimeUnit = costPerTimeUnit;
  }
}
