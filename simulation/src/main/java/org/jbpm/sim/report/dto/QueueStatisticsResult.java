package org.jbpm.sim.report.dto;


import desmoj.core.simulator.Queue;
import desmoj.core.simulator.QueueBased;

/**
 * POJO to remember values from objects of type
 * <code>desmoj.core.simulator.Queue</code> or <code>desmoj.core.simulator.QueueBases</code>
 * (without refused and strategy).
 * 
 * @author bernd.ruecker@camunda.com
 */
public class QueueStatisticsResult extends BaseResult { // org.jbpm.sim.report.dto.QueueStatisticsResult

  private static final long serialVersionUID = 8790401605841407111L;

  private String strategy;
  private long observations;
  private int queueLimit;
  private int length;
  private int minLength;
  private int maxLength;
  private double averageLength;
  private long zeroWaits;
  private double maxWaitTime;
  private double averageWaitTime;
  private long refused;
  private double stdDevLength;

  public QueueStatisticsResult(String name, String scenario, String strategy, long observations,
      int queueLimit, int length, int minLength, int maxLength,
      double averageLength, long zeroWaits, double maxWaitTime,
      double averageWaitTime, long refused, double stdDevLength) {
    super(name, scenario);

    this.strategy = strategy;
    this.observations = observations;
    this.queueLimit = queueLimit;
    this.length = length;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.averageLength = averageLength;
    this.zeroWaits = zeroWaits;
    this.maxWaitTime = maxWaitTime;
    this.averageWaitTime = averageWaitTime;
    this.refused = refused;
    this.stdDevLength = stdDevLength;
  } 

  public QueueStatisticsResult(String name, String scenarioName, QueueBased queue) {
    super(name, scenarioName);
    
    this.refused = -1;
    if (Queue.class.isAssignableFrom(queue.getClass())) {
      this.strategy = ((Queue)queue).getQueueStrategy();
      this.refused = ((Queue)queue).getRefused();
    }

    this.observations = queue.getObservations();
    this.queueLimit = queue.getQueueLimit();
    this.length = queue.length();
    this.minLength = queue.minLength();
    this.maxLength = queue.maxLength(); // if (q.getQueueLimit() == Integer.MAX_VALUE) --> "unlimit.";
    this.averageLength = queue.averageLength();
    this.zeroWaits = queue.zeroWaits();
    this.maxWaitTime = queue.maxWaitTime().getTimeValue();
    this.averageWaitTime = queue.averageWaitTime().getTimeValue();
    this.stdDevLength =  queue.stdDevLength();
  }

  public String getStrategy() {
    return strategy;
  }

  public long getObservations() {
    return observations;
  }

  public int getQueueLimit() {
    return queueLimit;
  }

  public int getLength() {
    return length;
  }

  public int getMinLength() {
    return minLength;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public double getAverageLength() {
    return averageLength;
  }

  public long getZeroWaits() {
    return zeroWaits;
  }

  public double getMaxWaitTime() {
    return maxWaitTime;
  }

  public double getAverageWaitTime() {
    return averageWaitTime;
  }

  public long getRefused() {
    return refused;
  }

  public double getStdDevLength() {
    return stdDevLength;
  }

}
