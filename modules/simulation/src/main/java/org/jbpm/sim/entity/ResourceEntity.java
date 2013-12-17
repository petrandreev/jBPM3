package org.jbpm.sim.entity;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class ResourceEntity extends Entity {

  public ResourceEntity(Model owner, String name) {
    super(owner, name, true);
  }

}
