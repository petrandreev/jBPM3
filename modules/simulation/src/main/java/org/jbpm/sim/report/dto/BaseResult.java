package org.jbpm.sim.report.dto;

import java.io.Serializable;

public class BaseResult implements Serializable {

  private static final long serialVersionUID = 6781023371976276684L;

  private String name;
  private String scenario;

  public BaseResult(String name, String scenario) {
    this.name = name;
    this.scenario = scenario;
  }

  public String getName() {
    return name;
  }

  public String getScenario() {
    return scenario;
  }

}