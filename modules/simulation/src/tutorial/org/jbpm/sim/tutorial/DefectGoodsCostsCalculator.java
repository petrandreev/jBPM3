package org.jbpm.sim.tutorial;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.kpi.BusinessFigureCalculator;

/**
 * Example of very easy business figure calculator
 * 
 * @author ruecker
 */
public class DefectGoodsCostsCalculator implements BusinessFigureCalculator {
  
  public Number calculate(ExecutionContext executionContext) {
    ReturnOrder o = (ReturnOrder)executionContext.getContextInstance().getVariable("returnOrder");
    double resaleValue = o.getEstimatedResaleValue();
        
    /*
     * okay, the goods is defect, throw it away, 
     * virtual costs is the value of the goods
     * 
     * (Remark: Maybe not realistic, but ok for this showcase.
     * I reality you may get some money from manufacturer when within warranty)
     */
    return Double.valueOf( resaleValue );
  }

}
