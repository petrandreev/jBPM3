package org.jbpm.job;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class Timer extends Job {

  private static final long serialVersionUID = 1L;

  private String name;
  private String repeat;
  private String transitionName;
  private Action action;
  private GraphElement graphElement;
  private String calendarResource;

  public Timer() {
  }

  public Timer(Token token) {
    super(token);
  }

  public boolean execute(JbpmContext jbpmContext) throws Exception {
    Token token = getToken();
    ExecutionContext executionContext = new ExecutionContext(token);
    executionContext.setTimer(this);

    TaskInstance taskInstance = getTaskInstance();
    if (taskInstance != null) {
      executionContext.setTaskInstance(taskInstance);
    }

    // first fire the event if there is a graph element specified
    if (graphElement != null) {
      graphElement.fireEvent(Event.EVENTTYPE_TIMER, executionContext);
    }

    // then execute the action if there is one
    if (action != null) {
      if (graphElement != null) {
        graphElement.executeAction(action, executionContext);
      }
      else {
        action.execute(executionContext);
      }
    }

    // then take a transition if one is specified
    // and if no unhandled exception occurred during the action
    String exception = getException();
    if (transitionName != null && exception == null) {
      Node node = token.getNode();
      Transition transition = node.getLeavingTransition(transitionName);
      if (transition != null) {
        token.signal(transition);
      }
      else {
        log.warn(node + " has no leaving transition named " + transitionName);
      }
    }

    // if repeat is specified, reschedule the job
    if (repeat != null) {
      // if a calendar resource is specified, use it to calculate repeat dates
      // https://jira.jboss.org/browse/JBPM-2958
      BusinessCalendar businessCalendar = calendarResource == null ? new BusinessCalendar()
        : new BusinessCalendar(calendarResource);

      Date repeatDate = businessCalendar.add(getDueDate(), new Duration(repeat));
      if (log.isDebugEnabled()) {
        log.debug("scheduling " + this + " for repeat on " + repeatDate);
      }
      setDueDate(repeatDate);

      // unlock timer so that:
      // (a) any job executor thread can acquire it the next time
      // (b) the engine knows it is not executing and can be canceled
      // https://jira.jboss.org/jira/browse/JBPM-2036
      setLockOwner(null);

      return false;
    }

    return true;
  }

  public String toString() {
    StringBuffer text = new StringBuffer("Timer(");

    // name or id
    if (name != null) {
      text.append(name);
    }
    else {
      text.append(getId());
    }

    return text.append(')').toString();
  }

  public String getRepeat() {
    return repeat;
  }

  public void setRepeat(String repeat) {
    this.repeat = repeat;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTransitionName() {
    return transitionName;
  }

  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }

  public GraphElement getGraphElement() {
    return graphElement;
  }

  public void setGraphElement(GraphElement graphElement) {
    this.graphElement = graphElement;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  /**
   * Gets the business calendar resource used for calculating repeat dates.
   */
  public String getCalendarResource() {
    return calendarResource;
  }

  /**
   * Sets the business calendar resource to use for calculating repeat dates.
   */
  public void setCalendarResource(String calendarResource) {
    this.calendarResource = calendarResource;
  }

  private static final Log log = LogFactory.getLog(Timer.class);
}
