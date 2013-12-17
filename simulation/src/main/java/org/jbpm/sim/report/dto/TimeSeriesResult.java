package org.jbpm.sim.report.dto;

import java.util.Collection;

import desmoj.core.statistic.TimeSeries;

public class TimeSeriesResult extends BaseResult { // org.jbpm.sim.report.dto.TimeSeriesResult

  private static final long serialVersionUID = 6608924829017323537L;

  public TimedValue[] values;
  
  public TimeSeriesResult(String name, String scenario, double[] timeValue, double[] dataValue) {
    super(name, scenario);
    
    this.values = new TimedValue[timeValue.length];
    for (int i = 0; i < timeValue.length; i++) {
      // dataValue should have the same length as timeValue, otherwise
      // we have a serious problem with the simulation framework
      this.values[i] = new TimedValue(timeValue[i], dataValue[i]);
    }
  }

  public TimeSeriesResult(String name, String scenario, TimeSeries ts) {
    this(name, scenario, ts.getTimeValues(), ts.getDataValues());
  }
  
  public TimeSeriesResult(String name, String scenario, Collection timeValue, Collection dataValue) {
    super(name, scenario);
    
    // I miss my autoboxing here :-(
    if (timeValue!=null && dataValue!=null) {
      Double[] temp1 = (Double[]) timeValue.toArray(new Double[0]);
      Double[] temp2 = (Double[]) dataValue.toArray(new Double[0]);
            
      this.values = new TimedValue[temp1.length];
      for (int i = 0; i < temp1.length; i++) {
        this.values[i] = new TimedValue(temp1[i].doubleValue(), temp2[i].doubleValue());
      }
    }
    else
      this.values = new TimedValue[0];
  }

  public TimedValue[] getValues() {
    return values;
  }

}
