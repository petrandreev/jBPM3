package org.jbpm.sim.report.dto;

import desmoj.core.statistic.ValueStatistics;

/**
 * POJO to remember values from objects of type
 * <code>desmoj.core.statistic.ValueStatistics</code>.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ValueStatisticResult extends BaseResult {  // org.jbpm.sim.report.ValueStatisticResult

  private static final long serialVersionUID = 1818666790550287367L;

  private double mean;
  private double standardDerivation;
  private double maximum;
  private double minimum;
  private long numberOfObservations;

  public ValueStatisticResult(String name, String scenario, double mean, double stdDev, double maximum, double minimum, long observations) {
    super(name, scenario);
    this.mean = mean;
    this.standardDerivation = stdDev;
    this.maximum = maximum;
    this.minimum = minimum;
    this.numberOfObservations = observations;    
  }
//  Better use Comparators, we don't yet now on which numbers to concentrate,
//  this depends on the application
//  public int compareTo(Object o) {
//    double otherMean = ((ValueStatisticResult)o).getMean();
//    
//    if (mean < otherMean)
//      return -1;
//    else if (mean > otherMean)
//      return 1;
//    else
//      return 0;
//  }  

  public ValueStatisticResult(String name, String scenarioName, ValueStatistics vs) {
    this(name,
        scenarioName, 
        vs.getMean(),
        vs.getStdDev(),
        vs.getMaximum(),
        vs.getMinimum(),
        vs.getObservations());
  }

  public double getMean() {
    return mean;
  }

  public double getStandardDerivation() {
    return standardDerivation;
  }

  public double getMaximum() {
    return maximum;
  }

  public double getMinimum() {
    return minimum;
  }

  public long getNumberOfObservations() {
    return numberOfObservations;
  }
}
