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

import java.util.Map;

import org.dom4j.Element;

import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.jpdl.xml.Parsable;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class Action implements ActionHandler, Parsable {

  private static final long serialVersionUID = 1L;

  long id;
  protected String name;
  protected boolean isPropagationAllowed = true;
  protected boolean isAsync;
  protected boolean isAsyncExclusive;
  protected Action referencedAction;
  protected Delegation actionDelegation;
  protected String actionExpression;
  protected Event event;
  protected ProcessDefinition processDefinition;

  public Action() {
  }

  public Action(Delegation actionDelegate) {
    this.actionDelegation = actionDelegate;
  }

  public String toString() {
    StringBuffer result = new StringBuffer("Action(");
    if (name != null) {
      result.append(name);
    }
    else if (actionDelegation != null) {
      result.append(actionDelegation);
    }
    else if (actionExpression != null) {
      result.append(actionExpression);
    }
    else if (referencedAction != null) {
      result.append(referencedAction.getName());
    }
    return result.append(')').toString();
  }

  public void read(Element actionElement, JpdlXmlReader jpdlReader) {
    String expression = actionElement.attributeValue("expression");
    if (expression != null) {
      actionExpression = expression;
    }
    else if (actionElement.attribute("ref-name") != null) {
      jpdlReader.addUnresolvedActionReference(actionElement, this);
    }
    else if (actionElement.attribute("class") != null) {
      actionDelegation = new Delegation();
      actionDelegation.read(actionElement, jpdlReader);
    }
    else {
      jpdlReader.addWarning("action does not have class nor ref-name attribute: "
        + actionElement.getPath());
    }
  }

  public void write(Element actionElement) {
    if (actionDelegation != null) {
      actionDelegation.write(actionElement);
    }
  }

  public void execute(ExecutionContext executionContext) throws Exception {
    Thread currentThread = Thread.currentThread();
    ClassLoader contextClassLoader = currentThread.getContextClassLoader();
    try {
      // set context class loader correctly for delegation class
      // (https://jira.jboss.org/jira/browse/JBPM-1448)
      ClassLoader processClassLoader =
          JbpmConfiguration.getProcessClassLoader(executionContext.getProcessDefinition());
      currentThread.setContextClassLoader(processClassLoader);

      if (referencedAction != null) {
        referencedAction.execute(executionContext);
      }
      else if (actionExpression != null) {
        JbpmExpressionEvaluator.evaluate(actionExpression, executionContext);
      }
      else if (actionDelegation != null) {
        ActionHandler actionHandler =
            (ActionHandler) actionDelegation.getInstance();
        actionHandler.execute(executionContext);
      }
    }
    finally {
      currentThread.setContextClassLoader(contextClassLoader);
    }
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Action)) return false;

    Action other = (Action) o;
    if (id != 0 && id == other.getId()) return true;

    if (name != null) {
      // named actions are unique at the process definition level
      return name.equals(other.getName())
        && processDefinition.equals(other.getProcessDefinition());
    }
    return (actionDelegation != null ? actionDelegation.equals(other.getActionDelegation())
        : actionExpression != null ? actionExpression.equals(other.getActionExpression())
            : referencedAction != null ? referencedAction.equals(other.getReferencedAction())
                : false)
      && event.equals(other.getEvent());
  }

  public int hashCode() {
    int result = 1397928647;
    if (name != null) {
      // named actions are unique at the process definition level
      result += name.hashCode();
      result = 1290535769 * result + processDefinition.hashCode();
    }
    else {
      if (actionDelegation != null) {
        result += actionDelegation.hashCode();
      }
      else if (actionExpression != null) {
        result += actionExpression.hashCode();
      }
      else if (referencedAction != null) {
        result += referencedAction.hashCode();
      }
      result = 1290535769 * result + event.hashCode();
    }
    return result;
  }

  // getters and setters //////////////////////////////////////////////////////

  public boolean acceptsPropagatedEvents() {
    return isPropagationAllowed;
  }

  public boolean isPropagationAllowed() {
    return isPropagationAllowed;
  }

  public void setPropagationAllowed(boolean isPropagationAllowed) {
    this.isPropagationAllowed = isPropagationAllowed;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    // if the process definition is already set
    if (processDefinition != null) {
      // update the process definition action map
      Map actionMap = processDefinition.getActions();
      // the != string comparison is to avoid null pointer checks.
      // no problem if the body is executed a few times too much :-)
      if ( (this.name != null && this.name.equals(name) || name != null) && actionMap != null) {
        actionMap.remove(this.name);
        actionMap.put(name, this);
      }
    }
  
    // then update the name
    this.name = name;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public ProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    this.processDefinition = processDefinition;
  }

  public Delegation getActionDelegation() {
    return actionDelegation;
  }

  public void setActionDelegation(Delegation instantiatableDelegate) {
    this.actionDelegation = instantiatableDelegate;
  }

  public Action getReferencedAction() {
    return referencedAction;
  }

  public void setReferencedAction(Action referencedAction) {
    this.referencedAction = referencedAction;
  }

  public boolean isAsync() {
    return isAsync;
  }

  public void setAsync(boolean isAsync) {
    this.isAsync = isAsync;
  }

  public boolean isAsyncExclusive() {
    return isAsyncExclusive;
  }

  public void setAsyncExclusive(boolean asyncExclusive) {
    isAsyncExclusive = asyncExclusive;
    if (asyncExclusive) isAsync = true;
  }

  public String getActionExpression() {
    return actionExpression;
  }

  public void setActionExpression(String actionExpression) {
    this.actionExpression = actionExpression;
  }
}
