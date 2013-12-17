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
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.svc.Services;
import org.jbpm.util.Clock;

public class CreateTimerAction extends Action {

  private static final long serialVersionUID = 1L;
  static BusinessCalendar businessCalendar = new BusinessCalendar(); 

  String timerName = null;
  String dueDate = null;
  String repeat = null;
  String transitionName = null;
  Action timerAction = null;

  public void read(Element actionElement, JpdlXmlReader jpdlReader) {
    timerName = actionElement.attributeValue("name");
    timerAction = jpdlReader.readSingleAction(actionElement);

    dueDate = actionElement.attributeValue("duedate");
    if (dueDate==null) {
      jpdlReader.addWarning("no duedate specified in create timer action '"+actionElement+"'");
    }
    repeat = actionElement.attributeValue("repeat");
    if ( "true".equalsIgnoreCase(repeat)
         || "yes".equalsIgnoreCase(repeat) ) {
      repeat = dueDate;
    }
    transitionName = actionElement.attributeValue("transition");
    
    if ( (transitionName!=null)
         && (repeat!=null) 
       ) {
      repeat = null;
      jpdlReader.addProblem(new Problem(Problem.LEVEL_WARNING, "ignoring repeat on timer with transition "+actionElement.asXML()));
    }
  }

  public void execute(ExecutionContext executionContext) throws Exception {
    Timer timer = createTimer(executionContext);
    SchedulerService schedulerService = (SchedulerService) Services.getCurrentService(Services.SERVICENAME_SCHEDULER);
    schedulerService.createTimer(timer);
  }

  protected Timer createTimer(ExecutionContext executionContext) {
    Date baseDate = null;
    Date dueDateDate = null;
    Duration duration;
    String durationString = null;
    String durationSeparator = null;
    Timer timer = new Timer(executionContext.getToken());
    timer.setName(timerName);
    timer.setRepeat(repeat);
    if (dueDate!=null) {
      if (dueDate.startsWith("#")) {
        String baseDateEL = dueDate.substring(0,dueDate.indexOf("}")+1);
        Object o = JbpmExpressionEvaluator.evaluate(baseDateEL, executionContext);
        if (o instanceof Date) {
          baseDate = (Date) o;          
        } else {
          if (o instanceof Calendar) {
            baseDate = ((Calendar) o).getTime();
          } else {
            throw new JbpmException("Invalid basedate type: " + baseDateEL + " is of type " + o.getClass().getName() +". Only Date and Calendar are supported");
          }
        }
        int endOfELIndex = dueDate.indexOf("}");
        if (endOfELIndex < (dueDate.length() -1) ) {
          durationSeparator = dueDate.substring(endOfELIndex+1).trim().substring(0,1);
          if ( !(durationSeparator.equals("+") || durationSeparator.equals("-") ) ){ 
            throw new JbpmException("Invalid duedate, + or - missing after EL");
          }
          durationString = dueDate.substring(endOfELIndex+1).trim();
        }
      } else {
        durationString = dueDate;
      }
      if (baseDate != null && (durationString == null || durationString.length() == 0)) {
        dueDateDate = baseDate;
      } else {
        duration = new Duration(durationString);
        dueDateDate = businessCalendar.add( (baseDate != null) ? baseDate : Clock.getCurrentTime(), duration );
      }
      timer.setDueDate(dueDateDate);
    }
    timer.setAction(timerAction);
    timer.setTransitionName(transitionName);
    timer.setGraphElement(executionContext.getEventSource());
    timer.setTaskInstance(executionContext.getTaskInstance());
    
    // if this action was executed for a graph element
    if ( (getEvent()!=null)
         && (getEvent().getGraphElement()!=null)
       ) {
      GraphElement graphElement = getEvent().getGraphElement();
      try {
        executionContext.setTimer(timer);
        // fire the create timer event on the same graph element
        graphElement.fireEvent(Event.EVENTTYPE_TIMER_CREATE, executionContext);
      } finally {
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
