package org.jbpm.sim.tutorial;

import java.util.Calendar;
import java.util.Date;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.datasource.ProcessDataSource;
import org.jbpm.sim.exception.ExperimentConfigurationException;

/**
 * Example variable source for the tutorial. This one is very easy,
 * it just returns 5 different return order types in a infinite loop,
 * hard coded. Not very sophisticated, but easy ;-)
 * 
 * @author bernd.ruecker@camunda.com
 */
public class TutorialDataSource implements ProcessDataSource {

  private int state = 0;
  
  private int STATE_MAX = 5;
  
  public void reset() {
    state = 0;
  }

  private Date getDate(int daysInPast) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, daysInPast);
    return cal.getTime();
  }

  public void addNextData(ExecutionContext ctx) {
    ctx.getContextInstance().createVariable("returnOrder", getProcessVariable());
    next();
  }

  private void next() {
    state++;
    if (state >= STATE_MAX)
      state = 0;
  }

  private Object getProcessVariable() {
    switch (state) {
    case 0:
      return new ReturnOrder(getDate(10), 100.0, 75.0);
    case 1:
      return new ReturnOrder(getDate(10), 100.0, 75.0);
    case 2:
      return new ReturnOrder(getDate(10), 100.0, 75.0);
    case 3:
      return new ReturnOrder(getDate(10), 100.0, 75.0);
    case 4:
      return new ReturnOrder(getDate(10), 100.0, 75.0);
    }
    return null;
  }

  public boolean hasNext() {
    return true;
  }
}
