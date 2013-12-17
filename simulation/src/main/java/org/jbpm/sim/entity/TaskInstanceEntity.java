package org.jbpm.sim.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.sim.def.ResourceRequirement;
import org.jbpm.sim.event.WorkCompletionEvent;
import org.jbpm.taskmgmt.exe.TaskInstance;

import desmoj.core.simulator.Model;

public class TaskInstanceEntity extends ResourceUsingEntity {

  private static Log log = LogFactory.getLog(TaskInstanceEntity.class);

  private TaskInstance taskInstance;

  public TaskInstanceEntity(Model owner, TaskInstance taskInstance) {
    super(owner, "TaskInstance " + taskInstance, true);
    this.taskInstance = taskInstance;
  }

  public Object getEntityObject() {
    return taskInstance.getTask();
  }
  
  public TaskInstance getTaskInstance() {
    return taskInstance;
  }
  
  protected void doStart() {
    startTaskAndScheduleCompletion();    
  }  
  
  public void startTaskAndScheduleCompletion() {
    taskInstance.start();

    // plan task completion
    log.info("ask the simulation clock to complete task "+taskInstance.getName()+" whenever it feels like it");
    // Therefore we need an event
    WorkCompletionEvent evt = new WorkCompletionEvent(getModel());
    // and schedule it to fire on this entity
    evt.schedule(this, getJbpmModel().getTaskWorkingTime( taskInstance.getTask() ));
  }

  protected ResourceRequirement[] getRequiredResources() {
    return getJbpmModel().getResourceRequirements(taskInstance.getTask());
    // TODO: not completely correct, could be also one or more resource-pools
    // and the amount should be not fixed
//    return new ResourceRequirement[] {
//        new ResourceRequirement(taskInstance.getTask().getSwimlane().getName(), 1)
//    };
  }

  public void doEnd() {
    // end the taskInstance, ask the simulation framework for the outgoing transition    
    taskInstance.end(
        getJbpmModel().getLeavingTransition(taskInstance.getToken().getNode()));
  }
}
