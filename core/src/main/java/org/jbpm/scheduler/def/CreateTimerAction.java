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
package org.jbpm.scheduler.def;

import java.util.Calendar;
import java.util.Date;

import org.dom4j.Element;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.job.Timer;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.util.Clock;

public class CreateTimerAction extends Action {

  private static final long serialVersionUID = 1L;

  private String timerName;
  private String dueDate;
  private String repeat;
  private String transitionName;
  private Action timerAction;

  public void read(Element actionElement, JpdlXmlReader jpdlReader) {
    timerName = actionElement.attributeValue("name");
    timerAction = jpdlReader.readSingleAction(actionElement);

    dueDate = actionElement.attributeValue("duedate");
    if (dueDate == null) {
      jpdlReader.addWarning("due date not specified on create timer: "
        + actionElement.getPath());
    }
    repeat = actionElement.attributeValue("repeat");
    if (jpdlReader.readBoolean(repeat, false)) {
      repeat = dueDate;
    }
    transitionName = actionElement.attributeValue("transition");

    if (transitionName != null && repeat != null) {
      repeat = null;
      jpdlReader.addWarning("ignoring repeat on create timer with transition: "
        + actionElement.getPath());
    }
  }

  public void execute(ExecutionContext executionContext) throws Exception {
    // grab jbpm context
    JbpmContext jbpmContext = executionContext.getJbpmContext();
    if (jbpmContext == null) {
      throw new JbpmException("jbpm context unavailable");
    }
    // retrieve scheduler service
    SchedulerService schedulerService = jbpmContext.getServices().getSchedulerService();
    if (schedulerService == null) {
      throw new JbpmException("scheduler service unavailable");
    }
    // schedule timer
    Timer timer = createTimer(executionContext);
    schedulerService.createTimer(timer);
  }

  public String toString() {
    return "CreateTimerAction(" + timerName + ')';
  }

  protected Timer createTimer(ExecutionContext executionContext) {
    Timer timer = new Timer(executionContext.getToken());
    timer.setName(timerName);
    timer.setRepeat(repeat);

    // calculate due date
    if (dueDate != null) {
      Date dueDateDate;

      // evaluate base date expression
      if (dueDate.startsWith("#{") || dueDate.startsWith("${")) {
        int braceIndex = dueDate.indexOf('}');
        if (braceIndex == -1) {
          throw new JbpmException("invalid due date, closing brace missing: " + dueDate);
        }

        String baseDateExpression = dueDate.substring(0, braceIndex + 1);
        Object result = JbpmExpressionEvaluator.evaluate(baseDateExpression, executionContext);

        Date baseDate;
        if (result instanceof Date) {
          baseDate = (Date) result;
        }
        else if (result instanceof Calendar) {
          baseDate = ((Calendar) result).getTime();
        }
        else {
          throw new JbpmException(baseDateExpression + " returned " + result
            + " instead of date or calendar");
        }

        String durationString = dueDate.substring(braceIndex + 1).trim();
        if (durationString.length() > 0) {
          char durationSeparator = durationString.charAt(0);
          if (durationSeparator != '+' && durationSeparator != '-') {
            throw new JbpmException("invalid due date, '+' or '-' missing after expression: "
              + dueDate);
          }
          dueDateDate = new BusinessCalendar().add(baseDate, new Duration(durationString));
        }
        else {
          dueDateDate = baseDate;
        }
      }
      // take current time as base date
      else {
        dueDateDate = new BusinessCalendar().add(Clock.getCurrentTime(), new Duration(dueDate));
      }
      timer.setDueDate(dueDateDate);
    }

    timer.setAction(timerAction);
    timer.setTransitionName(transitionName);
    timer.setGraphElement(executionContext.getEventSource());
    timer.setTaskInstance(executionContext.getTaskInstance());

    // if this action was executed for a graph element
    Event event = getEvent();
    GraphElement graphElement;
    if (event != null && (graphElement = event.getGraphElement()) != null) {
      try {
        executionContext.setTimer(timer);
        // fire the create timer event on the same graph element
        graphElement.fireEvent(Event.EVENTTYPE_TIMER_CREATE, executionContext);
      }
      finally {
        executionContext.setTimer(null);
      }
    }

    return timer;
  }

  public String getDueDate() {
    return dueDate;
  }

  public void setDueDate(String dueDateDuration) {
    this.dueDate = dueDateDuration;
  }

  public String getRepeat() {
    return repeat;
  }

  public void setRepeat(String repeatDuration) {
    this.repeat = repeatDuration;
  }

  public String getTransitionName() {
    return transitionName;
  }

  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }

  public String getTimerName() {
    return timerName;
  }

  public void setTimerName(String timerName) {
    this.timerName = timerName;
  }

  public Action getTimerAction() {
    return timerAction;
  }

  public void setTimerAction(Action timerAction) {
    this.timerAction = timerAction;
  }
}
