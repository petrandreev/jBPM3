package org.jbpm.sim.report;

/**
 * Statistic utils for calculating confidence intervals or sample sizes to 
 * satisfy given confidence levels.
 * 
 * More information can be found in the statistics chapter of my master thesis.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class StatUtil {

  /**
   * calculating the confidence interval summand, which must be added/subtracted from
   * the sample mean to get the confidence interval (mean +/- confidenceIntervalSummand).
   * 
   * The confidence interval is calculated with the 
   * "Inverse Cumulative Standard Normal Distribution Function" and not the t distribution
   * which requires a sample size of at least 30 to get qualitative results.
   */
  public static double getConfidenceIntervalSummand(double confidenceLevel, double standardDerivation, double sampleSize) {    
    // calculate Z
    double z = AcklamStatUtil.getInvCDF(getInvCdfParam(confidenceLevel), true);
    
    // calculate result
    double result = (z * standardDerivation) / Math.sqrt(sampleSize);    
    return result;
  }
  
  /**
   * computes the required sample size to get the given confidence level
   * and the given confidence interval width (NOTE: The parameter should be the half width,
   * because that is the variable normally added/subtracted from the mean).
   * 
   * The standard derivation of a sample must be provided. 
   * 
   * The confidence interval is calculated with the 
   * "Inverse Cumulative Standard Normal Distribution Function" and not the t distribution
   * which requires a sample size of at least 30 to get qualitative results.
   */
  public static double getRequiredSampleSize(double confidenceLevel, double halfConfidenceIntervalWidth, double standardDerivation) {
    // calculate Z
    double z = AcklamStatUtil.getInvCDF(getInvCdfParam(confidenceLevel), true);

    // calculate result
    double result = Math.pow( ((z * standardDerivation) / halfConfidenceIntervalWidth), 2);    
    return result;
  }
  
  public static double getInvCdfParam(double confidenceLevel) {
    return 1 - ((1d - confidenceLevel) / 2d);
  }
  
}
