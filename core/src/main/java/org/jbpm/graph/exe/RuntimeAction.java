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

import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * is an action added at runtime to the execution of one process instance.
 */
public class RuntimeAction implements Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected ProcessInstance processInstance;
  protected GraphElement graphElement;
  protected String eventType;
  protected Action action;

  public RuntimeAction() {
  }

  /**
   * creates a runtime action. Look up the event with
   * {@link GraphElement#getEvent(String)} and the action with
   * {@link ProcessDefinition#getAction(String)}. You can only lookup named
   * actions easily.
   */
  public RuntimeAction(Event event, Action action) {
    this.graphElement = event.getGraphElement();
    this.eventType = event.getEventType();
    this.action = action;
  }

  public RuntimeAction(GraphElement graphElement, String eventType,
      Action action) {
    this.graphElement = graphElement;
    this.eventType = eventType;
    this.action = action;
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RuntimeAction)) return false;

    RuntimeAction other = (RuntimeAction) o;
    if (id != 0 && id == other.getId()) return true;

    return eventType.equals(other.getEventType())
      && graphElement.equals(other.getGraphElement())
      && processInstance.equals(other.getProcessInstance());
  }

  public int hashCode() {
    int result = 560044783 + eventType.hashCode();
    result = 279308149 * result + graphElement.hashCode();
    result = 106268467 * result + processInstance.hashCode();
    return result;
  }

  // getters and setters //////////////////////////////////////////////////////

  public long getId() {
    return id;
  }

  public ProcessInstance getProcessInstance() {
    return processInstance;
  }

  public Action getAction() {
    return action;
  }

  public String getEventType() {
    return eventType;
  }

  public GraphElement getGraphElement() {
    return graphElement;
  }
}
