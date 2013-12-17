package org.jbpm.sim.def;

/**
 * @author bernd.ruecker@camunda.com
 * TODO: rename
 */
public class ResourceRequirement {

  private String resourcePoolName;
  
  private int amount;

  public ResourceRequirement(String resourcePoolName, int requiredAmount) {
    this.resourcePoolName = resourcePoolName;
    this.amount = requiredAmount;
  }

  public String getResourcePoolName() {
    return resourcePoolName;
  }

  public int getAmount() {
    return amount;
  }
}
