package org.jbpm.sim.tutorial;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.datasource.ProcessDataFilter;

/**
 * Easy example for a filter implementation, it just changes
 * the estimated resale value to 75 % of the original value
 * 
 * @author bernd.ruecker@camunda.com
 */
public class TutorialDataFilter implements ProcessDataFilter {

  public void changeProcessData(ExecutionContext ctx) {
    ReturnOrder ro = (ReturnOrder)ctx.getContextInstance().getVariable("order");
    ro.setEstimatedResaleValue( ro.getEstimatedResaleValue() * 0.75 );
    ctx.setVariable("order", ro);
  }

  public void reset() {    
  }
}
