package org.jbpm.sim.tutorial.business;

import java.util.Random;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;

/**
 * this is just for illustration how this can be done, just by
 * checking random orders. Maybe another idea would be to check
 * expensive goods or maybe doubtful customers.
 * 
 * This Decision handler is never executed in the simulation tutorial,
 * because I defined probabilities for the outcome there, which makes it 
 * easier to define different scenarios to play with this parameter 
 * 
 * @author bernd.ruecker@camunda.com
 */
public class DecideCheckHandler implements DecisionHandler {

  private static final long serialVersionUID = -509684687456418797L;
  
  /**
   * use fixed seed to make possible tests comparable
   */
  private static Random rnd = new Random(987654321);

  public String decide(ExecutionContext executionContext) throws Exception {
    int random = rnd.nextInt(10);
    if (random>=9)
      return "check";
    else
      return "no check";
  }

}
