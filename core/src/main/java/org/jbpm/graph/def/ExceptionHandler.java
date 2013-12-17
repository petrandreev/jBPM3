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
import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.util.ClassLoaderUtil;

public class ExceptionHandler implements Serializable {

  private static final long serialVersionUID = 1L;

  long id = 0;
  protected String exceptionClassName;
  protected GraphElement graphElement;
  protected List actions;

  private transient Class exceptionClass;

  public ExceptionHandler() {
  }

  public boolean matches(Throwable exception) {
    return exceptionClassName != null ? getExceptionClass().isInstance(exception) : true;
  }

  private Class getExceptionClass() {
    if (exceptionClass == null) {
      try {
        exceptionClass = ClassLoaderUtil.classForName(exceptionClassName);
      }
      catch (ClassNotFoundException e) {
        throw new JbpmException("exception class not found: " + exceptionClassName, e);
      }
    }
    return exceptionClass;
  }

  public void handleException(GraphElement graphElement, ExecutionContext executionContext)
    throws Exception {
    if (actions != null) {
      for (Iterator iter = actions.iterator(); iter.hasNext();) {
        Action action = (Action) iter.next();
        graphElement.executeAction(action, executionContext);
      }
    }
  }

  // actions
  // ///////////////////////////////////////////////////////////////////////////
  public List getActions() {
    return actions;
  }

  public void addAction(Action action) {
    if (actions == null) actions = new ArrayList();
    actions.add(action);
  }

  public void removeAction(Action action) {
    if (actions != null) actions.remove(action);
  }

  public void reorderAction(int oldIndex, int newIndex) {
    if (actions != null) actions.add(newIndex, actions.remove(oldIndex));
  }

  // getters and setters
  // ///////////////////////////////////////////////////////////////////////////

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
