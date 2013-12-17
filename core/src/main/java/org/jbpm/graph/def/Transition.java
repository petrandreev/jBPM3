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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.log.TransitionLog;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

public class Transition extends GraphElement {

  private static final long serialVersionUID = 1L;

  protected Node from;
  protected Node to;
  protected String condition;
  transient boolean isConditionEnforced = true;

  // event types //////////////////////////////////////////////////////////////

  private static final String[] EVENT_TYPES = {
    Event.EVENTTYPE_TRANSITION
  };

  /**
   * @deprecated arrays are mutable and thus vulnerable to external manipulation. use
   * {@link #getSupportedEventTypes()} instead
   */
  public static final String[] supportedEventTypes = (String[]) EVENT_TYPES.clone();

  public String[] getSupportedEventTypes() {
    return (String[]) EVENT_TYPES.clone();
  }

  // constructors /////////////////////////////////////////////////////////////

  public Transition() {
  }

  public Transition(String name) {
    super(name);
  }

  // from /////////////////////////////////////////////////////////////////////

  public Node getFrom() {
    return from;
  }

  /**
   * sets the from node unidirectionally. use {@link Node#addLeavingTransition(Transition)} to
   * get bidirectional relations mgmt.
   */
  public void setFrom(Node from) {
    this.from = from;
  }

  // to ///////////////////////////////////////////////////////////////////////

  /**
   * sets the to node unidirectionally. use {@link Node#addArrivingTransition(Transition)} to
   * get bidirectional relations mgmt.
   */
  public void setTo(Node to) {
    this.to = to;
  }

  public Node getTo() {
    return to;
  }

  /**
   * the condition expression for this transition.
   */
  public String getCondition() {
    return condition;
  }

  public void setCondition(String conditionExpression) {
    this.condition = conditionExpression;
  }

  public boolean isConditionEnforced() {
    return isConditionEnforced;
  }

  public void setConditionEnforced(boolean conditionEnforced) {
    isConditionEnforced = conditionEnforced;
  }

  /**
   * @deprecated call {@link #setConditionEnforced(boolean) setConditionEnforced(false)} instead
   */
  public void removeConditionEnforcement() {
    isConditionEnforced = false;
  }

  // behaviour ////////////////////////////////////////////////////////////////

  /**
   * passes execution over this transition.
   */
  public void take(ExecutionContext executionContext) {
    if (condition != null && isConditionEnforced) {
      Boolean result = (Boolean) JbpmExpressionEvaluator.evaluate(condition, executionContext, Boolean.class);
      if (!Boolean.TRUE.equals(result)) {
        throw new JbpmException("condition '" + condition + "' guarding " + this + " not met");
      }
    }

    // update the runtime context information
    Token token = executionContext.getToken();
    token.setNode(null);

    // start the transition log
    TransitionLog transitionLog = new TransitionLog(this,
      executionContext.getTransitionSource());
    token.startCompositeLog(transitionLog);
    try {
      // fire leave events for superstates (if any)
      fireSuperStateLeaveEvents(executionContext);

      // fire the transition event (if any)
      fireEvent(Event.EVENTTYPE_TRANSITION, executionContext);

      // fire enter events for superstates (if any)
      Node destination = fireSuperStateEnterEvents(executionContext);
      // update the ultimate destinationNode of this transition
      transitionLog.setDestinationNode(destination);
    }
    finally {
      // end the transition log
      token.endCompositeLog();
    }
    // pass the token to the destinationNode node
    to.enter(executionContext);
  }

  Node fireSuperStateEnterEvents(ExecutionContext executionContext) {
    // calculate the actual destinationNode node
    Node destination = to;
    while (destination != null && destination.isSuperStateNode()) {
      List nodes = destination.getNodes();
      destination = nodes != null && !nodes.isEmpty() ? (Node) nodes.get(0) : null;
    }

    if (destination == null) {
      String transitionName = name != null ? '\'' + name + '\'' : "in node '" + from + '\'';
      throw new JbpmException("transition " + transitionName + " has no destination");
    }

    // optimisation: check if there is a candidate superstate to be entered
    if (destination.getSuperState() != null) {
      // collect all the superstates being left
      List leavingSuperStates = collectAllSuperStates(destination, from);
      // reverse order so that events fire from outer to inner superstates
      Collections.reverse(leavingSuperStates);
      // fire a superstate-enter event for all superstates being left
      fireSuperStateEvents(leavingSuperStates, Event.EVENTTYPE_SUPERSTATE_ENTER, executionContext);
    }

    return destination;
  }

  void fireSuperStateLeaveEvents(ExecutionContext executionContext) {
    // optimisation: check if there is a candidate superstate to be left
    if (executionContext.getTransitionSource().getSuperState() != null) {
      // collect all the superstates being left
      List leavingSuperStates = collectAllSuperStates(executionContext.getTransitionSource(), to);
      // fire a node-leave event for all superstates being left
      fireSuperStateEvents(leavingSuperStates, Event.EVENTTYPE_SUPERSTATE_LEAVE, executionContext);
    }
  }

  /**
   * collect all superstates of a that do not contain node b.
   */
  static List collectAllSuperStates(Node a, Node b) {
    SuperState superState = a.getSuperState();
    List leavingSuperStates = new ArrayList();
    while (superState != null) {
      if (!superState.containsNode(b)) {
        leavingSuperStates.add(superState);
        superState = superState.getSuperState();
      }
      else {
        superState = null;
      }
    }
    return leavingSuperStates;
  }

  /**
   * fires the give event on all the superstates in the list.
   */
  void fireSuperStateEvents(List superStates, String eventType,
    ExecutionContext executionContext) {
    for (Iterator iter = superStates.iterator(); iter.hasNext();) {
      SuperState leavingSuperState = (SuperState) iter.next();
      leavingSuperState.fireEvent(eventType, executionContext);
    }
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Transition)) return false;

    Transition other = (Transition) o;
    if (id != 0 && id == other.getId()) return true;

    return (name != null ? name.equals(other.getName()) : other.getName() == null)
      && from != null && from.equals(other.getFrom());
  }

  public int hashCode() {
    if (from == null) return System.identityHashCode(this);

    int result = 580399073 + (name != null ? name.hashCode() : 0);
    result = 345105097 * result + from.hashCode();
    return result;
  }

  public String toString() {
    return "Transition("
      + (name != null ? name + ')' : (from != null && to != null) ? from + "->" + to
        : id != 0 ? id + ")" : '@' + Integer.toHexString(hashCode()));
  }

  // other
  // ///////////////////////////////////////////////////////////////////////////

  public void setName(String name) {
    if (from != null) {
      if (from.hasLeavingTransition(name)) {
        throw new IllegalArgumentException("cannot rename " + this + " because " + from
          + " already has a transition named " + name);
      }
      Map fromLeavingTransitions = from.getLeavingTransitionsMap();
      fromLeavingTransitions.put(name, this);
    }
    this.name = name;
  }

  public GraphElement getParent() {
    if (from != null && to != null) {
      if (from.equals(to)) return from.getParent();

      for (GraphElement fromParent = from; fromParent != null; fromParent = fromParent.getParent()) {
        for (GraphElement toParent = to; toParent != null; toParent = toParent.getParent()) {
          if (fromParent.equals(toParent)) return fromParent;
        }
      }
    }
    return processDefinition;
  }
}
