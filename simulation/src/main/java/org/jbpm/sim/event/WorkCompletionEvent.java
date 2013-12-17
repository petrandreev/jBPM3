package org.jbpm.sim.event;

import org.jbpm.sim.entity.ResourceUsingEntity;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

/**
 * This <code>desmoj.core.simulator.Event</code> triggers the completion of some "work",
 * which can be TaskInstances, States, ...
 * 
 * @author bernd.ruecker@camunda.com
 */
public class WorkCompletionEvent extends Event {

  public WorkCompletionEvent(Model owner) {
    super(owner, "WorkCompletionEvent", true);
  }

  public void eventRoutine(Entity who) {
    ResourceUsingEntity entity = (ResourceUsingEntity) who;    
    // just a hint: maybe release resources triggers some other processes!
    // so there can be other processes running "in between" this two instructions
    // but it is still the same model time, so no problem
    entity.releaseResources();    
    entity.end();  
  }
}
