package org.jbpm.sim.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jbpm.sim.def.JbpmSimulationModel;
import org.jbpm.sim.def.ResourceRequirement;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.Queue;
import desmoj.core.simulator.SimTime;

public abstract class ResourceUsingEntity extends Entity {

  private Map acquiredResourceEntities = new HashMap();
  
  private Set missingResources = new HashSet();
  
  private SimTime startWait;
  
  private SimTime endWait;

  private SimTime startWork;
  
  private SimTime endWork;  
  
  private JbpmSimulationModel model;
  
  public ResourceUsingEntity(Model owner, String name, boolean showInTrace) {
    super(owner, name, showInTrace);
    
    model = ((JbpmSimulationModel)owner);
  }
  
  protected JbpmSimulationModel getJbpmModel() {
    return model;
  }

  /**
   * @return the correct type name for this entity (for example Task #47), 
   *         not the name of this instance (for example TaskInstance #347)
   */
  public abstract Object getEntityObject();
  
  protected abstract ResourceRequirement[] getRequiredResources();

  /**
   * acquire all resources needed by this entity.
   * 
   * @return true, if all resources were available, false otherwise
   */
  public boolean acquireResourcesAndStart() {    
    startWait = currentTime();
    ResourceRequirement[] resources = getRequiredResources();
  
    for (int i = 0; i < resources.length; i++) {
      /* TODO: improve this section, can be more than one resource
         think about what to do if not enough resources are free
         should other resources be blocked already? 
         I think not, because it is unrealistic to block human resources for example.
         But this is done if we block it first and don't get the second resource for example...
         Hmm....
         
         Also think about saving waiting times for every resource
       */ 
  
      if (!consumeResources(resources[i].getResourcePoolName(), resources[i].getAmount())) {
        missingResources.add( resources[i].getResourcePoolName() );
      }
    }
    if (missingResources.size()==0) {
      start();
      return true;
    }
    else
      // else: This TaskInstance is queued for the resource "automatically", 
      // if the resource becomes available, it gets "notified"
      // this is implemented in the releaseResources of the experiment    
      return false;
  }

  /**
   * The entity is informed, that a resource from the given pool gets available again
   * So it can be checked if required resources can be consumed now.
   * 
   * If so, it is also checked if ALL resources are acquired, then the Entity can start working
   * (whatever it does)
   */
  public boolean resourceReleased(String poolName) {   
    if (!missingResources.contains(poolName))
      throw new RuntimeException("I am informed of a released resource, but I don't wait for it! Entity: " + getName() + "; poolName: '" + poolName + "'");
    
    ResourceRequirement[] resources = getRequiredResources();
  
    for (int i = 0; i < resources.length; i++) {
      /* TODO: improve this section, can be more than one resource
         think about what to do if not enough resources are free
         should other resources be blocked already? 
         I think not, because it is unrealistic to block human resources for example.
         But this is done if we block it first and don't get the second resource for example...
         Hmm....
       */ 

      if (resources[i].getResourcePoolName().equals(poolName)) {
        if (consumeResources(poolName, resources[i].getAmount())) {
          missingResources.remove( poolName );
        }
        break;
      }
    }
    if (missingResources.size()==0) {
      start();
      return true;
    }
    else
      return false;
  }

  /**
   * consume n resources from the given pool if available. Otherwise
   * the Entity is queued up for this resource and false is returned to indicate 
   * the missing resource 
   * 
   * @param poolName
   * @param n
   * @return true, if consuming was successful or false, if not enough resources are free
   */
  private boolean consumeResources(String poolName, int n) {
    Queue resourcePool = getJbpmModel().getResourcePool(poolName).getPool();
    
    if (n>resourcePool.getQueueLimit())
      throw new RuntimeException("can never handle this request for " + n + " resources of pool " + poolName + ", maximum of available resources is " + resourcePool.getQueueLimit());
    
    if (resourcePool.isEmpty() || resourcePool.length()<n) { 
      // no resource available at the moment
      // queue up, we need an entity class for the queue, so create it on the fly
      Queue q = getJbpmModel().getResourceQueue(poolName);
      q.insert(this);
      return false;
    }
    else {
      ResourceEntity[] result = new ResourceEntity[n];
      // remove resources from pool
      for (int i = 0; i < n; i++) {
        result[i] = (ResourceEntity) resourcePool.first();
        resourcePool.remove(result[i]);    
      }
      getJbpmModel().resourceUsageChanged(poolName);
      acquiredResourceEntities.put(poolName, result);
      return true;
    }    
  }

  /**
   * release all resources acquired by this entity
   * typically this is done after finishing this "task" (or whatever it is)
   */
  public void releaseResources() {    
    // release all resources
    for (Iterator iterator = acquiredResourceEntities.keySet().iterator(); iterator.hasNext();) {
      String poolName = (String) iterator.next();
      ResourceEntity[] resourceEntities = (ResourceEntity[]) acquiredResourceEntities.get(poolName);
      for (int i = 0; i < resourceEntities.length; i++) {
        getJbpmModel().getResourcePool(poolName).getPool().insert(resourceEntities[i]);
      }      
  
      // and check the queues where somebody may wait for one of the released resources
      getJbpmModel().checkWaitingQueue(poolName);
      getJbpmModel().resourceUsageChanged(poolName);
    }
    
  }

  public void start() {
    endWait = currentTime();
    
    getJbpmModel().getEntityWaitTimeTally(getEntityObject()).update(getWaitTime());
    
    startWork = currentTime();
    doStart();
  }
  
  /**
   * start the "work", whatever it is (for example start working on a TaskInstance)
   */
  protected abstract void doStart();
  
  public void end() {
    endWork = currentTime();
    
    // TOOD: think about if it makes sense to have a workTimeHistogram
    
    doEnd();
  }

  /**
   * start the "work", whatever it is (for example start working on a TaskInstance)
   */
  protected abstract void doEnd();

  public double getWaitTime() {
    if (startWait != null && endWait != null) 
      return endWait.getTimeValue() - startWait.getTimeValue();
    else
      return Double.NaN;
  }

  public double getWorkTime() {
    if (startWork != null && endWork != null) 
      return endWork.getTimeValue() - startWork.getTimeValue();
    else
      return Double.NaN;
  }
}