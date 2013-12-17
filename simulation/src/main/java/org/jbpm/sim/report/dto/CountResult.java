package org.jbpm.sim.report.dto;

import desmoj.core.statistic.Count;

public class CountResult extends BaseResult {
  
  private long count;

  private static final long serialVersionUID = 1L;

  public CountResult(String name, String scenario, Count count) {
    super(name, scenario);
    this.count = count.getValue();
  }

  public CountResult(String name, String scenario, long count) {
    super(name, scenario);
    this.count = count;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

}
