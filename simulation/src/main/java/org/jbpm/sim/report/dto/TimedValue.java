package org.jbpm.sim.report.dto;

import java.io.Serializable;

public class TimedValue implements Serializable { // org.jbpm.sim.report.dto.TimedValue

  private static final long serialVersionUID = 7092856556996284567L;

  private double time;

  private double value;

  public TimedValue(double time, double value) {
    this.time = time;
    this.value = value;
  }

  public double getTime() {
    return time;
  }

  public double getValue() {
    return value;
  }
  
}
