package org.jbpm.sim.tutorial;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.sim.kpi.BusinessFigureCalculator;

/**
 * Example of very easy business figure calculator
 * 
 * @author ruecker
 */
public class AlternativeCostsCalculator implements BusinessFigureCalculator {
  
  public Number calculate(ExecutionContext executionContext) {
    ReturnOrder o = (ReturnOrder)executionContext.getContextInstance().getVariable("returnOrder");
    double resaleValue = o.getEstimatedResaleValue();
    
    /*
     * lets assume the same probability for broken goods as in the
     * "status quo" process, which is
     *   15 % in quick test
     *   74.8 % in extended test
     * --> 79.8 % of returned goods are defect, 10.2 % not
     * 
     * Because we assume we sell the stuff to some other company
     * They do not reduce prices by 79.8 % but by 90 % (because
     * they have the work with checking that stuff now).
     * 
     * So we have "virtual costs" of 90 % of the value of the goods
     */
    return Double.valueOf( resaleValue * 0.90 );
  }

}
