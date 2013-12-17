package org.jbpm.job;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

public class Timer extends Job {

  private static final long serialVersionUID = 1L;

  private final static String dateFormat = "yyyy-MM-dd HH:mm:ss,SSS";
  static BusinessCalendar businessCalendar = new BusinessCalendar();

  String name;
  String repeat;
  String transitionName = null;
  Action action = null;
  GraphElement graphElement = null;

  public Timer() {
  }

  public Timer(Token token) {
    super(token);
  }

  public boolean execute(JbpmContext jbpmContext) throws Exception {
    // register process instance for automatic save
    // see https://jira.jboss.org/jira/browse/JBPM-1015
    jbpmContext.addAutoSaveProcessInstance(processInstance);

    // prepare execution context
    ExecutionContext executionContext = new ExecutionContext(token);
    executionContext.setTimer(this);
    if (taskInstance != null) executionContext.setTaskInstance(taskInstance);

    // first fire the event if there is a graph element specified
    if (graphElement != null) {
      graphElement.fireAndPropagateEvent(Event.EVENTTYPE_TIMER, executionContext);
    }

    // then execute the action if there is one
    if (action != null) {
      try {
        log.debug("executing " + this);
        if (graphElement != null) {
          graphElement.executeAction(action, executionContext);
        }
        else {
          action.execute(executionContext);
        }
      }
      catch (Exception actionException) {
        // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
        log.warn("timer action threw exception", actionException);
        // if there is a graphElement connected to this timer...
        if (graphElement != null) {
          try {
            // give that graphElement a chance to catch the exception
            graphElement.raiseException(actionException, executionContext);
            log.debug("timer exception got handled by '" + graphElement + "'");
          }
          catch (Exception handlerException) {
            // if the exception handler rethrows or the original exception results in a DelegationException...
            throw handlerException;
          }
        }
        else {
          throw actionException;
        }
      }
    }

    // then take a transition if one is specified
    // and if no unhandled exception occurred during the action
    if (transitionName != null && exception == null) {
      if (token.getNode().hasLeavingTransition(transitionName)) {
        token.signal(transitionName);
      }
    }

    // if repeat is specified, reschedule the job
    if (repeat != null) {
      // suppose that it took the timer runner thread a
      // very long time to execute the timers.
      // then the repeat action dueDate could already have passed.
      Duration interval = new Duration(repeat);
      long currentTime = System.currentTimeMillis();

      Date repeatDate = dueDate;
      do {
        repeatDate = businessCalendar.add(repeatDate, interval);
      } while (repeatDate.getTime() <= currentTime);

      log.debug("scheduling " + this + " for repeat on: " + formatDueDate(repeatDate));
      dueDate = repeatDate;

      // unlock timer so that:
      // (a) any job executor thread can acquire it next time
      // (b) other parts of the engine know it is not executing, and can be deleted
      // see https://jira.jboss.org/jira/browse/JBPM-2036
      lockOwner = null;

      return false;
    }

    return true;
  }

  public String toString() {
    StringBuilder text = new StringBuilder("Timer");
    if (name != null || dueDate != null) {
      text.append('(');

      if (name != null) text.append(name).append(",");
      if (dueDate != null) text.append(formatDueDate(dueDate)).append(",");
      if (taskInstance != null) text.append(taskInstance).append(",");

      if (token != null)
        text.append(token);
      else if (processInstance != null) text.append(processInstance);

      text.append(')');
    }
    return text.toString();
  }

  public static String formatDueDate(Date date) {
    return new SimpleDateFormat(dateFormat).format(date);
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

  private static Log log = LogFactory.getLog(Timer.class);
}
