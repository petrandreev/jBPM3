package org.jbpm.sim.def;

import org.jbpm.sim.exception.ExperimentConfigurationException;

import desmoj.core.dist.Distribution;
import desmoj.core.dist.IntDistConstant;
import desmoj.core.dist.IntDistPoisson;
import desmoj.core.dist.IntDistUniform;
import desmoj.core.dist.RealDistConstant;
import desmoj.core.dist.RealDistErlang;
import desmoj.core.dist.RealDistExponential;
import desmoj.core.dist.RealDistNormal;
import desmoj.core.dist.RealDistUniform;
import desmoj.core.simulator.Model;


/**
 * This class serves as an "container" to temporary save distribution configurations.
 * It can also create <code>desmoj.core.dist.Distribution<code>'s from this information.
 * 
 * @author bernd.ruecker@camunda.com
 * TODO: Make this class configurable to allow custom distributions later
 */
public class DistributionDefinition {

  public static boolean showInTrace = true;
  public static boolean showInReport = true;

  private String name;
  private String type;
  private String sampleType;
  private String valueText;
  private String meanText;
  private String standardDeviationText;
  private String minText;
  private String maxText;
  private boolean nonNegative;
  
  public DistributionDefinition(String name, String type, String sampleType,
      String valueText, String meanText, String standardDeviationText,
      String minText, String maxText, boolean nonNegative) {
    this.name = name;
    this.type = type;
    this.sampleType = sampleType;
    this.valueText = valueText;
    this.meanText = meanText;
    this.standardDeviationText = standardDeviationText;
    this.minText = minText;
    this.maxText = maxText;
    this.nonNegative = nonNegative;
  }
 
  public Distribution createDistribution(Model owner) {
    Distribution dist = createDistributionObject(owner);
    dist.setNonNegative(nonNegative);
    return dist;
  }
  
  private Distribution createDistributionObject(Model owner) {
    try {
      // Integer
      if ("int".equals(sampleType)) {
        if ("constant".equals(type)) 
          return new IntDistConstant(owner, name, Long.valueOf(valueText).longValue(), showInTrace, showInReport);
        else if ("poisson".equals(type)) 
          return new IntDistPoisson(owner, name, Double.valueOf(meanText).doubleValue(), showInTrace, showInReport);
        else if ("uniform".equals(type)) 
          return new IntDistUniform(owner, name, Long.valueOf(minText).longValue(), Long.valueOf(maxText).longValue(), showInTrace, showInReport);
      }    
      // Real
      else if ("real".equals(sampleType)) {
        if ("constant".equals(type)) 
          return new RealDistConstant(owner, name, Double.valueOf(valueText).doubleValue(), showInTrace, showInReport);
        else if ("normal".equals(type)) 
          return new RealDistNormal(owner, name, Double.valueOf(meanText).doubleValue(), Double.valueOf(standardDeviationText).doubleValue(), showInTrace, showInReport);      
        else if ("erlang".equals(type)) 
          return new RealDistErlang(owner, name, 1, Double.valueOf(meanText).doubleValue(), showInTrace, showInReport);      
        else if ("uniform".equals(type)) 
          return new RealDistUniform(owner, name, Double.valueOf(minText).doubleValue(), Double.valueOf(maxText).doubleValue(), showInTrace, showInReport);
        else if ("exponential".equals(type)) 
          return new RealDistExponential(owner, name, Double.valueOf(meanText).doubleValue(), showInTrace, showInReport);      
      }
      // Boolean
      else if ("boolean".equals(sampleType)) {
        throw new ExperimentConfigurationException("sample type boolean not yet supported.");      
      }
      else
        throw new ExperimentConfigurationException("sample type '" + sampleType + "' unknown.");
  
      throw new ExperimentConfigurationException("distribution type '" + type + "' for sample type '"+sampleType+"' unknown.");
    }
    catch (NumberFormatException ex) {
      throw new ExperimentConfigurationException("number format exception "+ex.getMessage()+" while creating distribution '" + name + "'", ex);
    }
  }  
  
  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getSampleType() {
    return sampleType;
  }

  public String getValueText() {
    return valueText;
  }

  public String getMeanText() {
    return meanText;
  }

  public String getStandardDeviationText() {
    return standardDeviationText;
  }

  public String getMinText() {
    return minText;
  }

  public String getMaxText() {
    return maxText;
  }

}
