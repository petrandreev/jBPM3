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

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.util.ClassLoaderUtil;

public class ExceptionHandler implements Serializable {

  private static final long serialVersionUID = 1L;

  long id = 0;
  protected String exceptionClassName = null;
  protected GraphElement graphElement = null;
  protected List<Action> actions = null;

  public ExceptionHandler() {
  }

  public boolean matches( Throwable exception ) {
    boolean matches = true;
    if (exceptionClassName!=null) {
      Class<?> clazz = ClassLoaderUtil.classForName(exceptionClassName);
      if (! clazz.isAssignableFrom(exception.getClass())) {
        matches = false;
      }
    }
    return matches;
  }

  public void handleException(GraphElement graphElement, ExecutionContext executionContext) throws Exception {
    if (actions!=null) {
      for (Action action : actions) {
        graphElement.executeAction(action, executionContext);
      }
    }
  }

  // actions
  /////////////////////////////////////////////////////////////////////////////
  public List<Action> getActions() {
    return actions;
  }
  
  public void addAction(Action action) {
    if (actions==null) actions = new ArrayList<Action>();
    actions.add(action);
  }
  
  public void removeAction(Action action) {
    if (actions!=null) {
      actions.remove(action);
    }
  }

  public void reorderAction(int oldIndex, int newIndex) {
    if (actions!=null) {
      actions.add(newIndex, actions.remove(oldIndex));
    }
  }

  // getters and setters
  /////////////////////////////////////////////////////////////////////////////
  
  public String getExceptionClassName() {
    return exceptionClassName;
  }
  public void setExceptionClassName(String exceptionClassName) {
    this.exceptionClassName = exceptionClassName;
  }
  public GraphElement getGraphElement() {
    return graphElement;
  }
}
