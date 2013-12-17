package org.jbpm.sim.report.dto;

import desmoj.core.simulator.QueueBased;

/**
 * POJO to remember Utilization of resource pools
 * 
 * Internally, resource pools are implemented as queues
 * so the queue statistics are mapped to utilization
 * figures
 * 
 * @author bernd.ruecker@camunda.com
 */
public class UtilizationStatisticsResult extends BaseResult { // org.jbpm.sim.report.dto.UtilizationStatisticsResult

  private static final long serialVersionUID = -1158340031538382394L;

  private long observations;
  private int poolSize;
  private int minAvailabe;
  private int maxAvailabe;
  private double averageUnutilizedAmount;
  private double stdDevUnutilizedAmount;
  private double maxUnutilizedTime;
  private double averageUnutilizedTime;
  private long zeroWaits;

  private double costs;
  private double costsPerTimeUnit;

  public UtilizationStatisticsResult(
      String name, String scenario, long observations,
      int poolSize, int minAvailabe, int maxAvailabe,
      double averageUnutilizedAmount, double stdDevUnutilizedAmount, 
      double maxUnutilizedTime, double averageUnutilizedTime, long zeroWaits, 
      double costsPerTimeUnit) {
        super(name, scenario);
        this.observations = observations;
        this.poolSize = poolSize;
        this.minAvailabe = minAvailabe;
        this.maxAvailabe = maxAvailabe;
        this.averageUnutilizedAmount = averageUnutilizedAmount;
        this.stdDevUnutilizedAmount = stdDevUnutilizedAmount;
        this.maxUnutilizedTime = maxUnutilizedTime;
        this.averageUnutilizedTime = averageUnutilizedTime;
        this.zeroWaits = zeroWaits;
        this.costsPerTimeUnit = costsPerTimeUnit;
  } 
  
  
  public UtilizationStatisticsResult(String name, String scenarioName, QueueBased queue, double costsPerTimeUnit) {
    this(
        name,
        scenarioName,
        queue.getObservations(),
        queue.getQueueLimit(),        
        queue.minLength(),
        queue.maxLength(),  
        queue.averageLength(),  // averageUnutilizedAmount     
        queue.stdDevLength(),// stdUnutilizedAmount
        queue.maxWaitTime().getTimeValue(),  // maxUnutilizedTime
        queue.averageWaitTime().getTimeValue(), // averageUnutilizedTime
        queue.zeroWaits(),
        costsPerTimeUnit); 
  }

  public void calculateCosts(double unutilizedCostFactor, double runtime) {
    double utilized = (getPoolSize() - getAverageUnutilizedAmount()) * runtime * costsPerTimeUnit;
    double unutilized = getAverageUnutilizedAmount() * unutilizedCostFactor * runtime * costsPerTimeUnit;
    
    this.costs = utilized + unutilized;
  }
  
  public double getAverageUtilization() {
    return (getPoolSize() - getAverageUnutilizedAmount()) / getPoolSize();   
  }

  public long getObservations() {
    return observations;
  }

  public int getPoolSize() {
    return poolSize;
  }

  public int getMinAvailabe() {
    return minAvailabe;
  }

  public int getMaxAvailabe() {
    return maxAvailabe;
  }
 
  public double getAverageUnutilizedAmount() {
    return averageUnutilizedAmount;
  }


  public double getStdDevUnutilizedAmount() {
    return stdDevUnutilizedAmount;
  }


  public double getMaxUnutilizedTime() {
    return maxUnutilizedTime;
  }


  public double getAverageUnutilizedTime() {
    return averageUnutilizedTime;
  }


  public long getZeroWaits() {
    return zeroWaits;
  }


  public double getCosts() {
    return costs;
  } 

}
