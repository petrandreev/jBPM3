package org.jbpm.sim.bam;

import java.util.ArrayList;

/**
 * (state or task instance) 
 *
 * @author bernd.ruecker@camunda.com
 */
public class ElementStatistics {
  
  private String name;

  /**
   * probabilities for the outgoing transitions.
   * The transition is the key, the value is a counter
   * for occurrence and can be used to specify the probability
   * later
   */
  private ArrayList leavingTransitionProbabilities = new ArrayList();

  private long sampleCount;

  private double durationAverage;
  private double durationStddev;

  private double durationMin;
  private double durationMax;
  
  public ElementStatistics(String name) {
    this.name = name;
  }
  
  public void addTransitionProbability(TransitionProbability tp) {
    leavingTransitionProbabilities.add(tp);
  }

  public TransitionProbability[] getLeavingTransitionProbabilities() {
    return (TransitionProbability[]) leavingTransitionProbabilities.toArray(new TransitionProbability[0]);
  }
  
  public double getDurationAverage() {
    return durationAverage;
  }
  public void setDurationAverage(double durationAverage) {
    this.durationAverage = durationAverage;
  }
  public double getDurationStddev() {
    return durationStddev;
  }
  public void setDurationStddev(double durationStddev) {
    this.durationStddev = durationStddev;
  }
  public double getDurationMin() {
    return durationMin;
  }
  public void setDurationMin(double durationMin) {
    this.durationMin = durationMin;
  }
  public double getDurationMax() {
    return durationMax;
  }
  public void setDurationMax(double durationMax) {
    this.durationMax = durationMax;
  }

  public long getSampleCount() {
    return sampleCount;
  }

  public void setSampleCount(long sampleCount) {
    this.sampleCount = sampleCount;
  }

  public String getName() {
    return name;
  }

}
