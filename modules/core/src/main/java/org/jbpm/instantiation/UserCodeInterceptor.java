package org.jbpm.instantiation;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;

public interface UserCodeInterceptor {
  
  void executeAction(Action action, ExecutionContext executionContext) throws Exception;
  void executeAssignment(AssignmentHandler assignmentHandler, Assignable assignable, ExecutionContext executionContext) throws Exception;
  void executeTaskControllerInitialization(TaskControllerHandler taskControllerHandler, TaskInstance taskInstance, ContextInstance contextInstance, Token token);
  void executeTaskControllerSubmission(TaskControllerHandler taskControllerHandler, TaskInstance taskInstance, ContextInstance contextInstance, Token token);

}
