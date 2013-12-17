package org.jbpm.sim.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.Token;
import org.jbpm.sim.def.ResourceRequirement;
import org.jbpm.sim.event.WorkCompletionEvent;

import desmoj.core.simulator.Model;

public class TokenEntity extends ResourceUsingEntity {

  private static Log log = LogFactory.getLog(TokenEntity.class);

  private Token token;

  public Token getToken() {
    return token;
  }

  public TokenEntity(Model owner, Token token) {
    super(owner, "Token " + token + " in state " + token.getNode(), true);
    this.token = token;
  }

  public Object getEntityObject() {
    return token.getNode();
  }  
  
  protected void doStart() {
    scheduleCompletion();    
  }  
  
  public void scheduleCompletion() {
    // plan task completion
    log.info("ask the simulation clock to signal token "+token+" whenever it feels like it");
    // Therefore we need an event
    WorkCompletionEvent evt = new WorkCompletionEvent(getModel());
    // and schedule it to fire on this entity
    evt.schedule(this, getJbpmModel().getStateWorkingTime( token.getNode() ));
  }

  protected ResourceRequirement[] getRequiredResources() {
    return getJbpmModel().getResourceRequirements( token.getNode() );
  }

  public void doEnd() {
    // signal the token, ask the simulation framework for the outgoing transition    
    token.signal(
        getJbpmModel().getLeavingTransition(token.getNode()));        
  }
}
