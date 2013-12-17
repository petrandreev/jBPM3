package org.jbpm.sim.exception;

/**
 * This Exception is thrown, if something with the simulation experiment
 * configuration is invalid.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ExperimentConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 2843165686280916208L;

  public ExperimentConfigurationException() {
    super();
  }

  public ExperimentConfigurationException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  public ExperimentConfigurationException(String arg0) {
    super(arg0);
  }

  public ExperimentConfigurationException(Throwable arg0) {
    super(arg0);
  }

}
