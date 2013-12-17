package org.jbpm.sim.bam;

/**
 * Container for probability of a transition (with name)
 * 
 * @author bernd.ruecker@camunda.com
 */
public class TransitionProbability {

  private String transitionName;
  
  private long count;

  public TransitionProbability(String transitionName, long count) {
    this.transitionName = transitionName;
    this.count = count;
  }

  public String getTransitionName() {
    return transitionName;
  }

  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }
}
