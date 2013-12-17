/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.graph.exe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Identifiable;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.log.ProcessInstanceCreateLog;
import org.jbpm.graph.log.ProcessInstanceEndLog;
import org.jbpm.job.CleanUpProcessJob;
import org.jbpm.job.SignalTokenJob;
import org.jbpm.logging.exe.LoggingInstance;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;
import org.jbpm.msg.MessageService;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;
import org.jbpm.util.Clock;

/**
 * is one execution of a {@link org.jbpm.graph.def.ProcessDefinition}. To create a new process
 * execution of a process definition, just use the {@link #ProcessInstance(ProcessDefinition)}.
 */
public class ProcessInstance implements Identifiable, Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected String key;
  protected Date start;
  protected Date end;
  protected ProcessDefinition processDefinition;
  protected Token rootToken;
  protected Token superProcessToken;
  protected boolean isSuspended;
  protected Map instances;
  protected Map transientInstances;
  protected List runtimeActions;
  /** not persisted */
  protected List cascadeProcessInstances;

  // constructors /////////////////////////////////////////////////////////////

  public ProcessInstance() {
  }

  /**
   * creates a new process instance for the given process definition, puts the root-token (=main
   * path of execution) in the start state and executes the initial node. In case the initial
   * node is a start-state, it will behave as a wait state. For each of the optional module
   * definitions contained in the {@link ProcessDefinition}, the corresponding module instance
   * will be created.
   * 
   * @throws JbpmException if processDefinition is null.
   */
  public ProcessInstance(ProcessDefinition processDefinition) {
    this(processDefinition, null, null);
  }

  /**
   * creates a new process instance for the given process definition, puts the root-token (=main
   * path of execution) in the start state and executes the initial node. In case the initial
   * node is a start-state, it will behave as a wait state. For each of the optional module
   * definitions contained in the {@link ProcessDefinition}, the corresponding module instance
   * will be created.
   * 
   * @param variables will be inserted into the context variables after the context submodule
   * has been created and before the process-start event is fired, which is also before the
   * execution of the initial node.
   * @throws JbpmException if processDefinition is null.
   */
  public ProcessInstance(ProcessDefinition processDefinition, Map variables) {
    this(processDefinition, variables, null);
  }

  /**
   * creates a new process instance for the given process definition, puts the root-token (=main
   * path of execution) in the start state and executes the initial node. In case the initial
   * node is a start-state, it will behave as a wait state. For each of the optional module
   * definitions contained in the {@link ProcessDefinition}, the corresponding module instance
   * will be created.
   * 
   * @param variables will be inserted into the context variables after the context submodule
   * has been created and before the process-start event is fired, which is also before the
   * execution of the initial node.
   * @throws JbpmException if processDefinition is null.
   */
  public ProcessInstance(ProcessDefinition processDefinition, Map variables, String key) {
    if (processDefinition == null) {
      throw new IllegalArgumentException("process definition is null");
    }
    // initialize the members
    this.processDefinition = processDefinition;
    this.rootToken = new Token(this);
    this.key = key;

    // create the optional definitions
    addInitialModuleDefinitions(processDefinition);

    // if this is created in the context of a persistent operation
    Services.assignId(this);

    // add the creation log
    rootToken.addLog(new ProcessInstanceCreateLog());

    // set the variables
    addInitialContextVariables(variables);

    Node initialNode = rootToken.getNode();
    fireStartEvent(initialNode);
  }

  public void addInitialContextVariables(Map variables) {
    if (variables != null) {
      ContextInstance contextInstance = getContextInstance();
      if (contextInstance != null) contextInstance.addVariables(variables);
    }
  }

  public void addInitialModuleDefinitions(ProcessDefinition processDefinition) {
    Map definitions = processDefinition.getDefinitions();
    // if the state-definition has optional definitions
    if (definitions != null) {
      // loop over each optional definition
      for (Iterator i = definitions.values().iterator(); i.hasNext();) {
        ModuleDefinition definition = (ModuleDefinition) i.next();
        // and create the corresponding optional instance
        ModuleInstance instance = definition.createInstance();
        if (instance != null) addInstance(instance);
      }
    }
  }

  public void fireStartEvent(Node initialNode) {
    start = Clock.getCurrentTime();

    // fire the process start event
    if (initialNode != null) {
      ExecutionContext executionContext = new ExecutionContext(rootToken);
      processDefinition.fireEvent(Event.EVENTTYPE_PROCESS_START, executionContext);

      // execute the start node
      initialNode.execute(executionContext);
    }
  }

  // optional module instances ////////////////////////////////////////////////

  /**
   * adds the given optional module instance (bidirectional).
   */
  public ModuleInstance addInstance(ModuleInstance moduleInstance) {
    if (moduleInstance == null) {
      throw new IllegalArgumentException("module instance is null");
    }

    if (instances == null) instances = new HashMap();
    instances.put(moduleInstance.getClass().getName(), moduleInstance);
    moduleInstance.setProcessInstance(this);
    return moduleInstance;
  }

  /**
   * removes the given optional module instance (bidirectional).
   */
  public ModuleInstance removeInstance(ModuleInstance moduleInstance) {
    if (moduleInstance == null) {
      throw new IllegalArgumentException("module instance is null");
    }

    if (instances != null && instances.remove(moduleInstance.getClass().getName()) != null) {
      moduleInstance.setProcessInstance(null);
      return moduleInstance;
    }
    return null;
  }

  /**
   * looks up an optional module instance by its class.
   */
  public ModuleInstance getInstance(Class moduleClass) {
    String className = moduleClass.getName();
    if (instances != null) {
      ModuleInstance moduleInstance = (ModuleInstance) instances.get(className);
      if (moduleInstance != null) return moduleInstance;
    }

    // client requested a module instance that is not in the persistent map;
    // assume the client wants a transient instance
    if (transientInstances == null) transientInstances = new HashMap();
    ModuleInstance moduleInstance = (ModuleInstance) transientInstances.get(className);
    if (moduleInstance == null) {
      try {
        moduleInstance = (ModuleInstance) moduleClass.newInstance();
        moduleInstance.setProcessInstance(this);
      }
      catch (InstantiationException e) {
        throw new JbpmException("failed to instantiate " + moduleClass, e);
      }
      catch (IllegalAccessException e) {
        throw new JbpmException(getClass() + " has no access to " + moduleClass, e);
      }
      transientInstances.put(className, moduleInstance);
    }

    return moduleInstance;
  }

  /**
   * process instance extension for process variableInstances.
   */
  public ContextInstance getContextInstance() {
    return (ContextInstance) getInstance(ContextInstance.class);
  }

  /**
   * process instance extension for managing the tasks and actors.
   */
  public TaskMgmtInstance getTaskMgmtInstance() {
    return (TaskMgmtInstance) getInstance(TaskMgmtInstance.class);
  }

  /**
   * process instance extension for logging. Probably you don't need to access the logging
   * instance directly. Mostly, {@link Token#addLog(ProcessLog)} is sufficient and more
   * convenient.
   */
  public LoggingInstance getLoggingInstance() {
    return (LoggingInstance) getInstance(LoggingInstance.class);
  }

  // operations ///////////////////////////////////////////////////////////////

  /**
   * instructs the main path of execution to continue by taking the default transition on the
   * current node.
   * 
   * @throws IllegalStateException if the token is not active.
   */
  public void signal() {
    if (hasEnded()) throw new IllegalStateException("process instance has ended");
    rootToken.signal();
  }

  /**
   * instructs the main path of execution to continue by taking the specified transition on the
   * current node.
   * 
   * @throws IllegalStateException if the token is not active.
   */
  public void signal(String transitionName) {
    if (hasEnded()) throw new IllegalStateException("process instance has ended");
    rootToken.signal(transitionName);
  }

  /**
   * instructs the main path of execution to continue by taking the specified transition on the
   * current node.
   * 
   * @throws IllegalStateException if the token is not active.
   */
  public void signal(Transition transition) {
    if (hasEnded()) throw new IllegalStateException("process instance has ended");
    rootToken.signal(transition);
  }

  /**
   * ends (=cancels) this process instance and all the tokens in it.
   */
  public void end() {
    // if already ended, do nothing
    if (end != null) return;

    // record the end time
    // the end time also indicates that this process instance has ended
    end = Clock.getCurrentTime();

    // end the main path of execution
    rootToken.end();

    // fire the process-end event
    processDefinition.fireEvent(Event.EVENTTYPE_PROCESS_END, new ExecutionContext(rootToken));

    // add the process instance end log
    rootToken.addLog(new ProcessInstanceEndLog());

    // Fetch this higher, rather than doing the work twice.
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();

    // is this a sub-process?
    if (superProcessToken != null && !superProcessToken.hasEnded()) {
      // is message service available?
      MessageService messageService;
      if (jbpmContext != null && Configs.getBoolean("jbpm.sub.process.async")
        && (messageService = jbpmContext.getServices().getMessageService()) != null) {
        // signal super-process token asynchronously to avoid stale state exceptions
        // due to concurrent signals to the super-process
        // https://jira.jboss.org/browse/JBPM-2948
        SignalTokenJob job = new SignalTokenJob(superProcessToken);
        job.setDueDate(new Date());
        job.setExclusive(true);
        messageService.send(job);
      }
      else {
        addCascadeProcessInstance(superProcessToken.getProcessInstance());
        // message service unavailable, signal super-process token synchronously
        ExecutionContext executionContext = new ExecutionContext(superProcessToken);
        executionContext.setSubProcessInstance(this);
        superProcessToken.signal(executionContext);
      }
    }

    // cancel jobs associated to this process instance

    // is there an active context?
    if (jbpmContext != null) {
      Services services = jbpmContext.getServices();
      PersistenceService persistenceService = services.getPersistenceService();
      // is persistence service available? if so, are there jobs to delete?
      if (persistenceService != null
        && persistenceService.getJobSession().countDeletableJobsForProcessInstance(this) > 0) {
        // is message service available?
        MessageService messageService = services.getMessageService();
        if (messageService != null) {
          // cancel jobs asynchronously to avoid stale state exceptions due to job acquisition
          // https://jira.jboss.org/browse/JBPM-1709
          CleanUpProcessJob job = new CleanUpProcessJob(rootToken);
          job.setDueDate(new Date());
          messageService.send(job);
        }
        else {
          // is scheduler service available?
          SchedulerService schedulerService = services.getSchedulerService();
          if (schedulerService != null) {
            // give scheduler a chance to cancel timers
            schedulerService.deleteTimersByProcessInstance(this);
          }
          else {
            // just delete jobs straight from the database
            persistenceService.getJobSession().deleteJobsForProcessInstance(this);
          }
        }
      }
    }
  }

  /**
   * suspends this execution. This will make sure that tasks, timers and messages related to
   * this process instance will not show up in database queries.
   * 
   * @see #resume()
   */
  public void suspend() {
    isSuspended = true;
    rootToken.suspend();
  }

  /**
   * resumes a suspended execution. All timers that have been suspended might fire if the
   * duedate has been passed. If an admin resumes a process instance, the option should be
   * offered to update, remove and create the timers and messages related to this process
   * instance.
   * 
   * @see #suspend()
   */
  public void resume() {
    isSuspended = false;
    rootToken.resume();
  }

  // runtime actions //////////////////////////////////////////////////////////

  /**
   * adds an action to be executed upon a process event in the future.
   */
  public RuntimeAction addRuntimeAction(RuntimeAction runtimeAction) {
    if (runtimeAction == null) {
      throw new IllegalArgumentException("runtime action is null");
    }
    if (runtimeActions == null) runtimeActions = new ArrayList();
    runtimeActions.add(runtimeAction);
    runtimeAction.processInstance = this;
    return runtimeAction;
  }

  /**
   * removes a runtime action.
   */
  public RuntimeAction removeRuntimeAction(RuntimeAction runtimeAction) {
    if (runtimeAction == null) {
      throw new IllegalArgumentException("runtime action is null");
    }
    if (runtimeActions != null && runtimeActions.remove(runtimeAction)) {
      runtimeAction.processInstance = null;
      return runtimeAction;
    }
    return null;
  }

  /**
   * is the list of all runtime actions.
   */
  public List getRuntimeActions() {
    return runtimeActions;
  }

  // various information retrieval methods ////////////////////////////////////

  /**
   * tells if this process instance is still active or not.
   */
  public boolean hasEnded() {
    return end != null;
  }

  /**
   * calculates if this process instance has still options to continue.
   */
  public boolean isTerminatedImplicitly() {
    return !hasEnded() ? rootToken.isTerminatedImplicitly() : true;
  }

  /**
   * looks up the token in the tree, specified by the slash-separated token path.
   * 
   * @param tokenPath is a slash-separated name that specifies a token in the tree.
   * @return the specified token or null if the token is not found.
   */
  public Token findToken(String tokenPath) {
    return rootToken != null ? rootToken.findToken(tokenPath) : null;
  }

  /**
   * collects all instances for this process instance.
   */
  public List findAllTokens() {
    List tokens = new ArrayList();
    tokens.add(rootToken);
    rootToken.collectChildrenRecursively(tokens);
    return tokens;
  }

  void addCascadeProcessInstance(ProcessInstance cascadeProcessInstance) {
    if (cascadeProcessInstances == null) cascadeProcessInstances = new ArrayList();
    cascadeProcessInstances.add(cascadeProcessInstance);
  }

  public Collection removeCascadeProcessInstances() {
    Collection removed = cascadeProcessInstances;
    cascadeProcessInstances = null;
    return removed;
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ProcessInstance)) return false;

    ProcessInstance other = (ProcessInstance) o;
    if (id != 0 && id == other.getId()) return true;

    return key != null && key.equals(other.getKey())
      && processDefinition.equals(other.getProcessDefinition());
  }

   /**
   * Computes the hash code for this process instance. Process instances without an id
   * (not persisted to db) will return their {@linkplain System#identityHashCode(Object) identity
   * hash code}.
   */
  public int hashCode() {
    if (id != 0) 
        return (int) (id ^ (id >>> 32));
    else
        return System.identityHashCode(this);
  }
  
  public String toString() {
    return "ProcessInstance"
      + (key != null ? '(' + key + ')' : id != 0 ? "(" + id + ')'
        : '@' + Integer.toHexString(hashCode()));
  }

  // getters and setters //////////////////////////////////////////////////////

  public long getId() {
    return id;
  }

  public Token getRootToken() {
    return rootToken;
  }

  public Date getStart() {
    return start;
  }

  public Date getEnd() {
    return end;
  }

  public Map getInstances() {
    return instances;
  }

  public ProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  public Token getSuperProcessToken() {
    return superProcessToken;
  }

  public void setSuperProcessToken(Token superProcessToken) {
    this.superProcessToken = superProcessToken;
  }

  public boolean isSuspended() {
    return isSuspended;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public void setEnd(Date end) {
    this.end = end;
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    this.processDefinition = processDefinition;
  }

  public void setRootToken(Token rootToken) {
    this.rootToken = rootToken;
  }

  public void setStart(Date start) {
    this.start = start;
  }

  /** a unique business key */
  public String getKey() {
    return key;
  }

  /** set the unique business key */
  public void setKey(String key) {
    this.key = key;
  }

}
