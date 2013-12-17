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
import java.util.List;

public class Event implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String EVENTTYPE_TRANSITION = "transition";
  public static final String EVENTTYPE_BEFORE_SIGNAL = "before-signal";
  public static final String EVENTTYPE_AFTER_SIGNAL = "after-signal";
  public static final String EVENTTYPE_PROCESS_START = "process-start";
  public static final String EVENTTYPE_PROCESS_END = "process-end";
  public static final String EVENTTYPE_NODE_ENTER = "node-enter";
  public static final String EVENTTYPE_NODE_LEAVE = "node-leave";
  public static final String EVENTTYPE_SUPERSTATE_ENTER = "superstate-enter";
  public static final String EVENTTYPE_SUPERSTATE_LEAVE = "superstate-leave";
  public static final String EVENTTYPE_SUBPROCESS_CREATED = "subprocess-created";
  public static final String EVENTTYPE_SUBPROCESS_END = "subprocess-end";
  public static final String EVENTTYPE_TASK_CREATE = "task-create";
  public static final String EVENTTYPE_TASK_ASSIGN = "task-assign";
  public static final String EVENTTYPE_TASK_START = "task-start";
  public static final String EVENTTYPE_TASK_END = "task-end";
  public static final String EVENTTYPE_TIMER_CREATE = "timer-create";
  public static final String EVENTTYPE_TIMER = "timer";

  long id;
  protected String eventType;
  protected GraphElement graphElement;
  protected List actions;

  // constructors /////////////////////////////////////////////////////////////

  public Event() {
  }

  public Event(String eventType) {
    this.eventType = eventType;
  }

  public Event(GraphElement graphElement, String eventType) {
    this.graphElement = graphElement;
    this.eventType = eventType;
  }

  // actions //////////////////////////////////////////////////////////////////

  /**
   * is the list of actions associated to this event.
   * 
   * @return an empty list if no actions are associated.
   */
  public List getActions() {
    return actions;
  }

  public boolean hasActions() {
    return actions != null && actions.size() > 0;
  }

  public Action addAction(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("action is null");
    }
    if (actions == null) actions = new ArrayList();
    actions.add(action);
    action.event = this;
    return action;
  }

  public void removeAction(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("action is null");
    }
    if (actions != null && actions.remove(action)) {
      action.event = null;
    }
  }

  public String toString() {
    return eventType;
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Event)) return false;

    Event other = (Event) o;
    if (id != 0 && id == other.getId()) return true;

    return eventType.equals(other.getEventType())
      && graphElement.equals(other.getGraphElement());
  }
  
  public int hashCode() {
    int result = 560044783 + eventType.hashCode();
    result = 279308149 * result + graphElement.hashCode();
    return result;
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getEventType() {
    return eventType;
  }

  public GraphElement getGraphElement() {
    return graphElement;
  }

  public long getId() {
    return id;
  }
}
