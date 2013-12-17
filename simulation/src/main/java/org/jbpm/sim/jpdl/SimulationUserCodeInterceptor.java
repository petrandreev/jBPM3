package org.jbpm.sim.jpdl;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.UserCodeInterceptor;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;

// TODO this serves mostly as an example of how the UserCodeInterceptor could be used.
// It remains to be seen weather this is the right technique for the job.
public class SimulationUserCodeInterceptor implements UserCodeInterceptor {

  public void executeAction(Action action, ExecutionContext executionContext) throws Exception {
    action.execute(executionContext);
  }

  public void executeAssignment(AssignmentHandler assignmentHandler, Assignable assignable, ExecutionContext executionContext) throws Exception {
    // during simulation, execution of assignments is skipped
  }

  public void executeTaskControllerInitialization(TaskControllerHandler taskControllerHandler, TaskInstance taskInstance, ContextInstance contextInstance, Token token) {
    taskControllerHandler.initializeTaskVariables(taskInstance, contextInstance, token);
  }

  public void executeTaskControllerSubmission(TaskControllerHandler taskControllerHandler, TaskInstance taskInstance, ContextInstance contextInstance, Token token) {
    taskControllerHandler.submitTaskVariables(taskInstance, contextInstance, token);
  }

}
