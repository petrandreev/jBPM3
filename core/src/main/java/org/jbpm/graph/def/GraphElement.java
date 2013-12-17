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
package org.jbpm.graph.def;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.RuntimeAction;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.log.ActionLog;
import org.jbpm.instantiation.UserCodeInterceptor;
import org.jbpm.instantiation.UserCodeInterceptorConfig;
import org.jbpm.job.ExecuteActionJob;
import org.jbpm.msg.MessageService;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.signal.EventService;
import org.jbpm.util.ClassUtil;

public abstract class GraphElement implements Identifiable, Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  protected String name;
  protected String description;
  protected ProcessDefinition processDefinition;
  protected Map events;
  protected List exceptionHandlers;

  public GraphElement() {
  }

  public GraphElement(String name) {
    setName(name);
  }

  // events ///////////////////////////////////////////////////////////////////

  /**
   * indicative set of event types supported by this graph element. this is currently only used
   * by the process designer to know which event types to show on a given graph element. in
   * process definitions and at runtime, there are no constraints on the event-types.
   */
  public abstract String[] getSupportedEventTypes();

  /**
   * gets the events, keyed by eventType (java.lang.String).
   */
  public Map getEvents() {
    return events;
  }

  public boolean hasEvents() {
    return events != null && !events.isEmpty();
  }

  public Event getEvent(String eventType) {
    return events != null ? (Event) events.get(eventType) : null;
  }

  public boolean hasEvent(String eventType) {
    return events != null ? events.containsKey(eventType) : false;
  }

  public Event addEvent(Event event) {
    if (event == null) {
      throw new IllegalArgumentException("event is null");
    }
    if (event.getEventType() == null) {
      throw new IllegalArgumentException("event type is null");
    }
    if (events == null) {
      events = new HashMap();
    }
    events.put(event.getEventType(), event);
    event.graphElement = this;
    return event;
  }

  public Event removeEvent(Event event) {
    Event removedEvent = null;
    if (event == null) {
      throw new IllegalArgumentException("event is null");
    }
    if (event.getEventType() == null) {
      throw new IllegalArgumentException("event type is null");
    }
    if (events != null) {
      removedEvent = (Event) events.remove(event.getEventType());
      if (removedEvent != null) {
        event.graphElement = null;
      }
    }
    return removedEvent;
  }

  // exception handlers ///////////////////////////////////////////////////////

  /**
   * is the list of exception handlers associated to this graph element.
   */
  public List getExceptionHandlers() {
    return exceptionHandlers;
  }

  public ExceptionHandler addExceptionHandler(ExceptionHandler exceptionHandler) {
    if (exceptionHandler == null) {
      throw new IllegalArgumentException("exception handler is null");
    }
    if (exceptionHandlers == null) {
      exceptionHandlers = new ArrayList();
    }
    exceptionHandlers.add(exceptionHandler);
    exceptionHandler.graphElement = this;
    return exceptionHandler;
  }

  public void removeExceptionHandler(ExceptionHandler exceptionHandler) {
    if (exceptionHandler == null) {
      throw new IllegalArgumentException("exception handler is null");
    }
    if (exceptionHandlers != null && exceptionHandlers.remove(exceptionHandler)) {
      exceptionHandler.graphElement = null;
    }
  }

  public void reorderExceptionHandler(int oldIndex, int newIndex) {
    if (exceptionHandlers != null && Math.min(oldIndex, newIndex) >= 0
      && Math.max(oldIndex, newIndex) < exceptionHandlers.size()) {
      Object o = exceptionHandlers.remove(oldIndex);
      exceptionHandlers.add(newIndex, o);
    }
    else {
      throw new IndexOutOfBoundsException("could not move element from " + oldIndex + " to "
        + newIndex);
    }
  }

  // event handling ///////////////////////////////////////////////////////////

  public void fireEvent(String eventType, ExecutionContext executionContext) {
    Token token = executionContext.getToken();
    if (log.isDebugEnabled()) log.debug(token + " fires event '" + eventType + "' on " + this);

    GraphElement eventSource = executionContext.getEventSource();
    try {
      executionContext.setEventSource(this);

      JbpmContext jbpmContext = executionContext.getJbpmContext();
      if (jbpmContext != null) {
        EventService eventService = (EventService) jbpmContext.getServices()
          .getService(EventService.SERVICE_NAME);
        if (eventService != null) eventService.fireEvent(eventType, this, executionContext);
      }

      fireAndPropagateEvent(eventType, executionContext);
    }
    finally {
      executionContext.setEventSource(eventSource);
    }
  }

  public void fireAndPropagateEvent(String eventType, ExecutionContext executionContext) {
    // check whether the event was fired on this element
    // or propagated from another element
    boolean isPropagated = !equals(executionContext.getEventSource());

    // execute static actions
    Event event = getEvent(eventType);
    if (event != null) {
      // update the context
      executionContext.setEvent(event);
      // execute the static actions specified in the process definition
      executeActions(event.getActions(), executionContext, isPropagated);
    }

    // execute the runtime actions
    List runtimeActions = getRuntimeActionsForEvent(executionContext, eventType);
    executeActions(runtimeActions, executionContext, isPropagated);

    // remove the event from the context
    executionContext.setEvent(null);

    // propagate the event to the parent element
    GraphElement parent = getParent();
    if (parent != null) {
      parent.fireAndPropagateEvent(eventType, executionContext);
    }
  }

  private void executeActions(List actions, ExecutionContext executionContext,
    boolean isPropagated) {
    if (actions == null) return;

    for (Iterator iter = actions.iterator(); iter.hasNext();) {
      Action action = (Action) iter.next();
      if (!action.acceptsPropagatedEvents() && isPropagated) continue;

      if (action.isAsync()) {
        ExecuteActionJob job = createAsyncActionExecutionJob(executionContext.getToken(),
          action);
        MessageService messageService = executionContext.getJbpmContext()
          .getServices()
          .getMessageService();
        messageService.send(job);
      }
      else {
        executeAction(action, executionContext);
      }
    }
  }

  protected ExecuteActionJob createAsyncActionExecutionJob(Token token, Action action) {
    ExecuteActionJob job = new ExecuteActionJob(token);
    job.setAction(action);
    job.setDueDate(new Date());
    job.setExclusive(action.isAsyncExclusive());
    return job;
  }

  public void executeAction(Action action, ExecutionContext executionContext) {
    // create action log
    ActionLog actionLog = new ActionLog(action);
    Token token = executionContext.getToken();
    token.startCompositeLog(actionLog);

    try {
      // if the action is associated to an event, the token needs to be locked.
      // conversely, if the action is the behavior of a node or the token is already locked,
      // the token does not need to be locked
      if (executionContext.getEvent() != null && !token.isLocked()) {
        String lockOwner = action.toString();
        token.lock(lockOwner);
        try {
          executeActionImpl(action, executionContext);
        }
        finally {
          token.unlock(lockOwner);
        }
      }
      else {
        executeActionImpl(action, executionContext);
      }
    }
    catch (Exception exception) {
      // NOTE that Errors are not caught because that might halt the JVM
      // and mask the original Error
      // log the action exception
      actionLog.setException(exception);
      // search for an exception handler or throw to the client
      raiseException(exception, executionContext);
    }
    finally {
      token.endCompositeLog();
    }
  }

  private void executeActionImpl(Action action, ExecutionContext executionContext)
    throws Exception {
    // set context action
    executionContext.setAction(action);
    try {
      UserCodeInterceptor userCodeInterceptor = UserCodeInterceptorConfig.getUserCodeInterceptor();
      if (userCodeInterceptor != null) {
        userCodeInterceptor.executeAction(action, executionContext);
      }
      else {
        action.execute(executionContext);
      }
    }
    finally {
      // reset context action
      executionContext.setAction(null);
    }
  }

  private List getRuntimeActionsForEvent(ExecutionContext executionContext, String eventType) {
    List eventRuntimeActions = null;
    List runtimeActions = executionContext.getProcessInstance().getRuntimeActions();
    if (runtimeActions != null) {
      for (Iterator iter = runtimeActions.iterator(); iter.hasNext();) {
        RuntimeAction runtimeAction = (RuntimeAction) iter.next();
        // if the runtime action is registered on this element and eventType
        if (equals(runtimeAction.getGraphElement())
          && eventType.equals(runtimeAction.getEventType())) {
          // ... add its action to the list of runtime actions
          if (eventRuntimeActions == null) eventRuntimeActions = new ArrayList();
          eventRuntimeActions.add(runtimeAction.getAction());
        }
      }
    }
    return eventRuntimeActions;
  }

  /**
   * looks for an {@linkplain ExceptionHandler exception handler} in this graph element and then
   * recursively up the parent hierarchy. If an exception handler is found, it is applied. If
   * the exception handler does not rethrow, the exception is considered handled. Otherwise the
   * rethrown exception is propagated up the parent hierarchy.
   * 
   * @throws DelegationException if no applicable exception handler is found
   */
  public void raiseException(Throwable exception, ExecutionContext executionContext) {
    if (isAbleToHandleExceptions(executionContext)) {
      ExceptionHandler exceptionHandler = findExceptionHandler(exception);
      // was a matching exception handler found?
      if (exceptionHandler != null) {
        // make exception available to handler
        executionContext.setException(exception);
        try {
          exceptionHandler.handleException(this, executionContext);
          return;
        }
        catch (Exception e) {
          // NOTE that Errors are not caught because that might halt the JVM
          // and mask the original Error
          exception = e;
        }
        finally {
          // clear exception after handling it, allowing execution to handle further exceptions
          // see https://jira.jboss.org/browse/JBPM-2854
          executionContext.setException(null);
        }
      }

      // if this graph element is not parentless
      GraphElement parent = getParent();
      if (parent != null && !equals(parent)) {
        // propagate exception to parent
        parent.raiseException(exception, executionContext);
        return;
      }
    }

    // if exception was not handled, throw delegation exception at client
    throw exception instanceof JbpmException ? (JbpmException) exception
      : new DelegationException("no applicable exception handler found", exception);
  }

  /**
   * Tells whether the given context can handle exception by checking for:
   * <ul>
   * <li>the absence of a previous exception</li>
   * <li>an active transaction, or no transaction present</li>
   * </ul>
   */
  private static boolean isAbleToHandleExceptions(ExecutionContext executionContext) {
    // if executionContext has an exception set, it is already handling an exception
    // in this case, do not offer exception to handlers; throw at client
    // see https://jira.jboss.org/browse/JBPM-1887
    if (executionContext.getException() != null) return false;

    // check whether transaction is still active before scanning exception handlers
    // this way, the exception handlers can be loaded lazily
    // see https://jira.jboss.org/browse/JBPM-1775
    JbpmContext jbpmContext = executionContext.getJbpmContext();
    if (jbpmContext != null) {
      PersistenceService persistenceService = jbpmContext.getServices().getPersistenceService();
      if (persistenceService instanceof DbPersistenceService) {
        DbPersistenceService dbPersistenceService = (DbPersistenceService) persistenceService;
        return dbPersistenceService.isTransactionActive();
      }
    }

    // no transaction present; likely running in memory
    return true;
  }

  protected ExceptionHandler findExceptionHandler(Throwable exception) {
    if (exceptionHandlers != null) {
      for (Iterator iter = exceptionHandlers.iterator(); iter.hasNext();) {
        ExceptionHandler candidate = (ExceptionHandler) iter.next();
        if (candidate.matches(exception)) return candidate;
      }
    }
    return null;
  }

  public GraphElement getParent() {
    return processDefinition;
  }

  /**
   * @return the ancestors of this graph element ordered by depth.
   */
  public List getParents() {
    GraphElement parent = getParent();
    if (parent == null) return Collections.EMPTY_LIST;

    List parents = new ArrayList();
    parent.addParentChain(parents);
    return parents;
  }

  /**
   * @return this graph element plus the ancestors ordered by depth.
   */
  public List getParentChain() {
    List parents = new ArrayList();
    addParentChain(parents);
    return parents;
  }

  private void addParentChain(List parentChain) {
    parentChain.add(this);
    GraphElement parent = getParent();
    if (parent != null) parent.addParentChain(parentChain);
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!getClass().isInstance(o)) return false;

    GraphElement other = (GraphElement) o;
    if (id != 0 && id == other.getId()) return true;

    GraphElement parent = getParent();
    GraphElement otherParent = other.getParent();

    boolean result;
    if (name != null && parent != null) {
      result = name.equals(other.getName());
    }
    else if (parent instanceof NodeCollection && otherParent instanceof NodeCollection) {
      NodeCollection nodeCollection = (NodeCollection) parent;
      
      // JBPM-3423: equals calls itself recursively via ArrayList.indexOf(this)
      // hashCode's in Java are not 100% reliable (i.e. NOT always unique)
      //   but reliable enough for this situation
      Iterator iter = nodeCollection.getNodes().iterator();
      int index = -1;
      for( int i = 0; iter.hasNext(); ++i ) { 
        int elemHashCode = System.identityHashCode(iter.next());
        if( elemHashCode == System.identityHashCode(this) ) { 
          index = i;
        }
      }
      assert index != -1 : nodeCollection.getNodes();

      NodeCollection otherNodeCollection = (NodeCollection) otherParent;
      int otherIndex = otherNodeCollection.getNodes().indexOf(other);
      result = index == otherIndex;
    }
    else {
      return false;
    }
    return result && parent.equals(otherParent);
  }

  public int hashCode() {
    GraphElement parent = getParent();

    int result = 580399073;
    if (name != null && parent != null) {
      result += name.hashCode();
    }
    else if (parent instanceof NodeCollection) {
      NodeCollection nodeCollection = (NodeCollection) parent;
      int index = nodeCollection.getNodes().indexOf(this);
      assert index != -1 : nodeCollection.getNodes();
      result += index;
    }
    else {
      return super.hashCode();
    }
    return 345105097 * result + parent.hashCode();
  }

  public String toString() {
    return ClassUtil.getSimpleName(getClass())
      + (name != null ? '(' + name + ')' : id != 0 ? "(" + id + ')'
        : '@' + Integer.toHexString(hashCode()));
  }

  // getters and setters //////////////////////////////////////////////////////

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    this.processDefinition = processDefinition;
  }

  // logger ///////////////////////////////////////////////////////////////////
  private static final Log log = LogFactory.getLog(GraphElement.class);
}
