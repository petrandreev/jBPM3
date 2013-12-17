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

import java.io.*;

import org.jbpm.graph.def.*;
import org.jbpm.util.EqualsUtil;

/**
 * is an action that can be added at runtime to the execution of one process instance.
 */
public class RuntimeAction implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  long id = 0;
  int version = 0;
  protected ProcessInstance processInstance = null;
  protected GraphElement graphElement = null;
  protected String eventType = null;
  protected Action action = null;
  
  public RuntimeAction() {
  }

  /**
   * creates a runtime action.  Look up the event with {@link GraphElement#getEvent(String)}
   * and the action with {@link ProcessDefinition#getAction(String)}.  You can only 
   * lookup named actions easily.
   */
  public RuntimeAction(Event event, Action action) {
    this.graphElement = event.getGraphElement();
    this.eventType = event.getEventType();
    this.action = action;
  }

  public RuntimeAction(GraphElement graphElement, String eventType, Action action) {
    this.graphElement = graphElement;
    this.eventType = eventType;
    this.action = action;
  }

  // equals ///////////////////////////////////////////////////////////////////
  // hack to support comparing hibernate proxies against the real objects
  // since this always falls back to ==, we don't need to overwrite the hashcode
  public boolean equals(Object o) {
    return EqualsUtil.equals(this, o);
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
