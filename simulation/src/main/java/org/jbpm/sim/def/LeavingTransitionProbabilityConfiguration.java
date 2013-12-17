package org.jbpm.sim.def;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.sim.exception.ExperimentConfigurationException;

import desmoj.core.dist.Distribution;
import desmoj.core.dist.RealDist;
import desmoj.core.dist.RealDistUniform;
import desmoj.core.simulator.Model;

/**
 * This class cares about the probabilities of outgoing transitions
 * of a <code>org.jbpm.graph.def.Node</code>
 *  
 * @author bernd.ruecker@camunda.com
 */
public class LeavingTransitionProbabilityConfiguration {

  private Node node;
  
  private RealDist distribution;
  
  private double probabilitySum = 0.0;
  
  /**
   * This maps contains all transition and their range of samples 
   */
  private Map transitionProbabilityRange = new HashMap();

  public LeavingTransitionProbabilityConfiguration(Node node) {
    this.node = node;
  }
  
  public LeavingTransitionProbabilityConfiguration(Node node, Transition trans, double probability) {
    this(node);
    addTransition(trans, probability);
  }
  
  public void addTransition(Transition trans, double probability) {
    transitionProbabilityRange.put(trans, new double[] { probabilitySum, probabilitySum+probability} );
    probabilitySum += probability;
  }
  
  /**
   * create the distribution. Call this method AFTER all transitions are added, otherwise
   * we get wrong probability sums
   * 
   * we use an uniform distribution, because every value is equal probable now
   * so the probability values configured on each transition refers to a "range" of samples.
   * If the value is in these range, the transition is chosen.
   * Every node gets a own distribution, to make them statistically independent
   * 
   * @return the distribution used
   */
  public Distribution createDistribution(Model owner) {
    distribution = new RealDistUniform(owner, // model
        "Selecting outgoing transition of " + node, 
        0.0, // lower border
        probabilitySum, // upper border
        true, // showInReport
        false);
    
    return distribution;
  }

  /**
   * now let the dices decide, which transition to take
   * @return the chosen transition
   */
  public Transition decideOutgoingTransition() {
    double sample = distribution.sample();
    
    for (Iterator iterator = transitionProbabilityRange.keySet().iterator(); iterator.hasNext();) {
      Transition trans = (Transition) iterator.next();
      double[] range = (double[]) transitionProbabilityRange.get(trans);
      
      if (sample>=range[0] && sample < range[1])
        return trans;
    }
    
    throw new ExperimentConfigurationException("Couldn't find a transition for the sample " + sample + ". Wired, this should never happen!");
  }

  public Node getNode() {
    return node;
  }
  
}
