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
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.log.TransitionLog;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

public class Transition extends GraphElement
{
  private static final long serialVersionUID = 1L;

  protected Node from = null;
  protected Node to = null;
  protected String condition = null;
  transient boolean isConditionEnforced = true;

  // event types //////////////////////////////////////////////////////////////

  public static final String[] supportedEventTypes = new String[] { Event.EVENTTYPE_TRANSITION };

  public String[] getSupportedEventTypes()
  {
    return supportedEventTypes;
  }

  // constructors /////////////////////////////////////////////////////////////

  public Transition()
  {
  }

  public Transition(String name)
  {
    super(name);
  }

  // from /////////////////////////////////////////////////////////////////////

  public Node getFrom()
  {
    return from;
  }

  /*
   * sets the from node unidirectionally. use {@link Node#addLeavingTransition(Transition)} to get bidirectional relations mgmt.
   */
  public void setFrom(Node from)
  {
    this.from = from;
  }

  // to ///////////////////////////////////////////////////////////////////////

  /*
   * sets the to node unidirectionally. use {@link Node#addArrivingTransition(Transition)} to get bidirectional relations mgmt.
   */
  public void setTo(Node to)
  {
    this.to = to;
  }

  public Node getTo()
  {
    return to;
  }

  /*
   * the condition expresssion for this transition.
   */
  public String getCondition()
  {
    return condition;
  }

  public void setCondition(String conditionExpression)
  {
    this.condition = conditionExpression;
  }

  public void removeConditionEnforcement()
  {
    isConditionEnforced = false;
  }

  // behaviour ////////////////////////////////////////////////////////////////

  /*
   * passes execution over this transition.
   */
  public void take(ExecutionContext executionContext)
  {
    // update the runtime context information
    executionContext.getToken().setNode(null);

    Token token = executionContext.getToken();

    if ((condition != null) && (isConditionEnforced))
    {
      Object result = JbpmExpressionEvaluator.evaluate(condition, executionContext);
      if (result == null)
      {
        throw new JbpmException("transition condition " + condition + " evaluated to null");
      }
      else if (!(result instanceof Boolean))
      {
        throw new JbpmException("transition condition " + condition + " evaluated to non-boolean: " + result.getClass().getName());
      }
      else if (!((Boolean)result).booleanValue())
      {
        throw new JbpmException("transition condition " + condition + " evaluated to 'false'");
      }
    }

    // start the transition log
    TransitionLog transitionLog = new TransitionLog(this, executionContext.getTransitionSource());
    token.startCompositeLog(transitionLog);
    try
    {

      // fire leave events for superstates (if any)
      fireSuperStateLeaveEvents(executionContext);

      // fire the transition event (if any)
      fireEvent(Event.EVENTTYPE_TRANSITION, executionContext);

      // fire enter events for superstates (if any)
      Node destination = fireSuperStateEnterEvents(executionContext);
      // update the ultimate destinationNode of this transition
      transitionLog.setDestinationNode(destination);

    }
    finally
    {
      // end the transition log
      token.endCompositeLog();
    }

    // pass the token to the destinationNode node
    to.enter(executionContext);
  }

  Node fireSuperStateEnterEvents(ExecutionContext executionContext)
  {
    // calculate the actual destinationNode node
    Node destination = to;
    while (destination != null && destination.isSuperStateNode())
    {
      List<Node> nodes = destination.getNodes();
      destination = nodes != null && !nodes.isEmpty() ? (Node)nodes.get(0) : null;
    }

    if (destination == null)
    {
      String transitionName = (name != null ? "'" + name + "'" : "in node '" + from + "'");
      throw new JbpmException("transition " + transitionName + " doesn't have destination. check your processdefinition.xml");
    }

    // performance optimisation: check if at least there is a candidate superstate to be entered.
    if (destination.getSuperState() != null)
    {
      // collect all the superstates being left
      List<SuperState> leavingSuperStates = collectAllSuperStates(destination, from);
      // reverse the order so that events are fired from outer to inner superstates
      Collections.reverse(leavingSuperStates);
      // fire a superstate-enter event for all superstates being left
      fireSuperStateEvents(leavingSuperStates, Event.EVENTTYPE_SUPERSTATE_ENTER, executionContext);
    }

    return destination;
  }

  void fireSuperStateLeaveEvents(ExecutionContext executionContext)
  {
    // performance optimisation: check if at least there is a candidate superstate to be left.
    if (executionContext.getTransitionSource().getSuperState() != null)
    {
      // collect all the superstates being left
      List<SuperState> leavingSuperStates = collectAllSuperStates(executionContext.getTransitionSource(), to);
      // fire a node-leave event for all superstates being left
      fireSuperStateEvents(leavingSuperStates, Event.EVENTTYPE_SUPERSTATE_LEAVE, executionContext);
    }
  }

  /*
   * collect all superstates of a that do not contain node b.
   */
  static List<SuperState> collectAllSuperStates(Node a, Node b)
  {
    SuperState superState = a.getSuperState();
    List<SuperState> leavingSuperStates = new ArrayList<SuperState>();
    while (superState != null)
    {
      if (!superState.containsNode(b))
      {
        leavingSuperStates.add(superState);
        superState = superState.getSuperState();
      }
      else
      {
        superState = null;
      }
    }
    return leavingSuperStates;
  }

  /*
   * fires the give event on all the superstates in the list.
   */
  void fireSuperStateEvents(List<SuperState> superStates, String eventType, ExecutionContext executionContext)
  {
    for (SuperState leavingSuperState : superStates) {
      leavingSuperState.fireEvent(eventType, executionContext);
    }
  }

  // other
  // ///////////////////////////////////////////////////////////////////////////

  public void setName(String name)
  {
    if (from != null)
    {
      if (from.hasLeavingTransition(name))
      {
        throw new IllegalArgumentException("couldn't set name '" + name + "' on transition '" + this
            + "'cause the from-node of this transition has already another leaving transition with the same name");
      }
      Map<String, Transition> fromLeavingTransitions = from.getLeavingTransitionsMap();
      fromLeavingTransitions.remove(this.name);
      fromLeavingTransitions.put(name, this);
    }
    this.name = name;
  }

  public GraphElement getParent()
  {
    GraphElement parent = null;
    if ((from != null) && (to != null))
    {
      if (from.equals(to))
      {
        parent = from.getParent();
      }
      else
      {
        outerLoop:
        for (GraphElement fromParent : from.getParentChain()) {
          for (GraphElement toParent : to.getParentChain()) {
            if (fromParent == toParent) {
              parent = fromParent;
              break outerLoop;
            }
          }
        }
      }
    }
    return parent;
  }
}
