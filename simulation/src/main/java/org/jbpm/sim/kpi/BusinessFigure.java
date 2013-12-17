package org.jbpm.sim.kpi;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.exception.ExperimentConfigurationException;

/**
 * Configuration of business figures in processes. Either configured
 * with a handler, which can calculate the business figure
 * or an expression.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class BusinessFigure {

  private String name;
  
  private String type;
  
  private Class implementationClass;
  
  private String expression;
  
  private double result = 0d;

  public BusinessFigure(String name, String type, String implementationClass, String expression) {
    this.name = name;
    this.type = type;
    if (implementationClass!=null) {
      try {
        this.implementationClass = Class.forName(implementationClass);
      }
      catch (ClassNotFoundException ex) {
        throw new ExperimentConfigurationException("business figure handler class '" + implementationClass + "' could not be loaded", ex);
      }
    }
    else
      this.expression = expression;
  }

  public BusinessFigure(String name, String type, Class implementationClass) {
    this.name = name;
    this.type = type;
    this.implementationClass = implementationClass;
  }
  
  public BusinessFigure(String name, String type, String expression) {
    this.name = name;
    this.type = type;
    this.expression = expression;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public Class getImplementationClass() {
    return implementationClass;
  }

  public String getExpression() {
    return expression;
  }

  public Number calculateAndAdd(ExecutionContext executionContext) {
    Number result = null;
    if (implementationClass!=null) {
      try {
        BusinessFigureCalculator calc = (BusinessFigureCalculator) implementationClass.newInstance();
        result = calc.calculate(executionContext);
      }
      catch (Exception ex) {
        throw new ExperimentConfigurationException("Couldn't initialize business figure handler of type " + implementationClass, ex);
      }
    }
    else {
      // TODO: implement
      throw new RuntimeException("expressions in business figure calculation not yet implemented");
    }
    
    this.result += result.doubleValue();
    return result;
  }

  public double getResult() {
    return result;
  }  
  
  public Object clone() {
    return copy();
  }
  
  public BusinessFigure copy() {
    if (implementationClass!=null) {
      return new BusinessFigure(name, type, implementationClass);      
    }
    else {
      return new BusinessFigure(name, type, expression);
    }
  }
    
}
