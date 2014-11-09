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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

import org.jbpm.JbpmException;
import org.jbpm.graph.action.ActionTypes;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.log.NodeLog;
import org.jbpm.job.ExecuteNodeJob;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.jpdl.xml.Parsable;
import org.jbpm.util.Clock;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class Node extends GraphElement implements Parsable {

  private static final long serialVersionUID = 1L;

  public static class NodeType implements Serializable {

    private final String name;
    private static final Map values = new HashMap();

    private static final long serialVersionUID = 1L;

    public static final NodeType Node = new NodeType("Node");
    public static final NodeType StartState = new NodeType("StartState");
    public static final NodeType EndState = new NodeType("EndState");
    public static final NodeType State = new NodeType("State");
    public static final NodeType Task = new NodeType("Task");
    public static final NodeType Fork = new NodeType("Fork");
    public static final NodeType Join = new NodeType("Join");
    public static final NodeType Decision = new NodeType("Decision");

    protected NodeType(String name) {
      this.name = name;
      values.put(name, this);
    }

    public String toString() {
      return name;
    }

    public static NodeType valueOf(String name) {
      return (NodeType) values.get(name);
    }

    private Object readResolve() throws ObjectStreamException {
      NodeType nodeType = valueOf(name);
      if (nodeType == null) {
        throw new InvalidObjectException("invalid node type: " + name);
      }
      return nodeType;
    }
  };

  protected List leavingTransitions;
  private transient Map leavingTransitionMap;
  protected Set arrivingTransitions;
  protected Action action;
  protected SuperState superState;
  protected boolean isAsync;
  protected boolean isAsyncExclusive;

  public NodeType getNodeType() {
    return NodeType.Node;
  }

  /** @deprecated no use for this method */
  public String getNameExt() {
    String name = getName();
    if (name == null) name = "#anonymous" + getNodeType();
    return name;
  }

  // event types //////////////////////////////////////////////////////////////

  private static final String[] EVENT_TYPES = {
    Event.EVENTTYPE_NODE_ENTER,
    Event.EVENTTYPE_NODE_LEAVE,
    Event.EVENTTYPE_BEFORE_SIGNAL,
    Event.EVENTTYPE_AFTER_SIGNAL
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

  /**
   * creates an unnamed node.
   */
  public Node() {
  }

  /**
   * creates a node with the given name.
   */
  public Node(String name) {
    super(name);
  }

  public void read(Element nodeElement, JpdlXmlReader jpdlXmlReader) {
    action = jpdlXmlReader.readSingleAction(nodeElement);
  }

  public void write(Element nodeElement) {
    if (action != null) {
      String actionName = ActionTypes.getActionName(action.getClass());
      Element actionElement = nodeElement.addElement(actionName);
      action.write(actionElement);
    }
  }

  // leaving transitions //////////////////////////////////////////////////////

  public List getLeavingTransitions() {
    return leavingTransitions;
  }

  public List getLeavingTransitionsList() {
    return leavingTransitions;
  }

  /**
   * are the leaving {@link Transition}s, mapped by their name (String).
   */
  public Map getLeavingTransitionsMap() {
    if (leavingTransitionMap == null && leavingTransitions != null) {
      // initialize the cached leaving transition map
      leavingTransitionMap = new HashMap();
      ListIterator iter = leavingTransitions.listIterator(leavingTransitions.size());
      while (iter.hasPrevious()) {
        Transition leavingTransition = (Transition) iter.previous();
        leavingTransitionMap.put(leavingTransition.getName(), leavingTransition);
      }
    }
    return leavingTransitionMap;
  }

  /**
   * creates a bidirection relation between this node and the given leaving transition.
   * 
   * @throws IllegalArgumentException if leavingTransition is null.
   */
  public Transition addLeavingTransition(Transition leavingTransition) {
    if (leavingTransition == null) {
      throw new IllegalArgumentException("leaving transition is null");
    }
    if (leavingTransitions == null) leavingTransitions = new ArrayList();
    leavingTransition.from = this;
    leavingTransitions.add(leavingTransition);
    leavingTransitionMap = null;
    return leavingTransition;
  }

  /**
   * removes the bidirectional relation between this node and the given leaving transition.
   * 
   * @throws IllegalArgumentException if leavingTransition is null.
   */
  public void removeLeavingTransition(Transition leavingTransition) {
    if (leavingTransition == null) {
      throw new IllegalArgumentException("leaving transition is null");
    }
    if (leavingTransitions != null && leavingTransitions.remove(leavingTransition)) {
      leavingTransition.from = null;
      leavingTransitionMap = null;
    }
  }

  /**
   * checks for the presence of a leaving transition with the given name. the leaving
   * transitions of the supernode are taken into account as well.
   * 
   * @return true if this node has a leaving transition with the given name, false otherwise.
   */
  public boolean hasLeavingTransition(String transitionName) {
    return getLeavingTransition(transitionName) != null;
  }

  /**
   * retrieves a leaving transition by name. the leaving transitions of the supernode are taken
   * into account as well.
   */
  public Transition getLeavingTransition(String transitionName) {
    if (leavingTransitions != null) {
      for (Iterator i = leavingTransitions.iterator(); i.hasNext();) {
        Transition transition = (Transition) i.next();
        if (transitionName != null ? transitionName.equals(transition.getName())
          : transition.getName() == null) return transition;
      }
    }
    return superState != null ? superState.getLeavingTransition(transitionName) : null;
  }

  /**
   * tells whether this node lacks leaving transitions.
   */
  public boolean hasNoLeavingTransitions() {
    return (leavingTransitions == null || leavingTransitions.isEmpty())
      && (superState == null || superState.hasNoLeavingTransitions());
  }

  /**
   * generates a new name for a transition that will be added as a leaving transition.
   */
  public String generateNextLeavingTransitionName() {
    String name = null;
    if (leavingTransitions != null && containsName(leavingTransitions, null)) {
      int n = 1;
      while (containsName(leavingTransitions, Integer.toString(n)))
        n++;
      name = Integer.toString(n);
    }
    return name;
  }

  boolean containsName(List leavingTransitions, String name) {
    for (Iterator iter = leavingTransitions.iterator(); iter.hasNext();) {
      Transition transition = (Transition) iter.next();
      if (name != null ? name.equals(transition.getName()) : transition.getName() == null)
        return true;
    }
    return false;
  }

  // default leaving transition and leaving transition ordering ///////////////

  /**
   * is the default leaving transition.
   */
  public Transition getDefaultLeavingTransition() {
    if (leavingTransitions != null && !leavingTransitions.isEmpty()) {
      // select the first unconditional transition
      for (Iterator i = leavingTransitions.iterator(); i.hasNext();) {
        Transition transition = (Transition) i.next();
        if (transition.getCondition() == null) return transition;
      }
      // there is no unconditional transition, just pick the first one
      return (Transition) leavingTransitions.get(0);
    }
    else if (superState != null) {
      return superState.getDefaultLeavingTransition();
    }
    return null;
  }

  /**
   * moves one leaving transition from the oldIndex and inserts it at the newIndex.
   */
  public void reorderLeavingTransition(int oldIndex, int newIndex) {
    if (leavingTransitions != null && Math.min(oldIndex, newIndex) >= 0
      && Math.max(oldIndex, newIndex) < leavingTransitions.size()) {
      Object transition = leavingTransitions.remove(oldIndex);
      leavingTransitions.add(newIndex, transition);
    }
  }

  // arriving transitions /////////////////////////////////////////////////////

  /**
   * are the arriving transitions.
   */
  public Set getArrivingTransitions() {
    return arrivingTransitions;
  }

  /**
   * add a bidirection relation between this node and the given arriving transition.
   * 
   * @throws IllegalArgumentException if t is null.
   */
  public Transition addArrivingTransition(Transition arrivingTransition) {
    if (arrivingTransition == null) {
      throw new IllegalArgumentException("arriving transition is null");
    }
    if (arrivingTransitions == null) arrivingTransitions = new HashSet();
    arrivingTransition.to = this;
    arrivingTransitions.add(arrivingTransition);
    return arrivingTransition;
  }

  /**
   * removes the bidirection relation between this node and the given arriving transition.
   * 
   * @throws IllegalArgumentException if t is null.
   */
  public void removeArrivingTransition(Transition arrivingTransition) {
    if (arrivingTransition == null) {
      throw new IllegalArgumentException("arriving transition is null");
    }
    if (arrivingTransitions != null && arrivingTransitions.remove(arrivingTransition)) {
      arrivingTransition.to = null;
    }
  }

  // various //////////////////////////////////////////////////////////////////

  /**
   * is the {@link SuperState} or the {@link ProcessDefinition} in which this node is contained.
   */
  public GraphElement getParent() {
    GraphElement parent;
    if (superState != null)
      parent = superState;
    else
      parent = processDefinition;
    return parent;
  }

  // behaviour methods ////////////////////////////////////////////////////////

  /**
   * called by a transition to pass execution to this node.
   */
  public void enter(ExecutionContext executionContext) {
    Token token = executionContext.getToken();

    // update the runtime context information
    token.setNode(this);

    // register entrance time so that a node-log can be generated upon leaving
    token.setNodeEnter(Clock.getCurrentTime());

    // fire the leave-node event for this node
    fireEvent(Event.EVENTTYPE_NODE_ENTER, executionContext);

    // remove the transition references from the runtime context
    executionContext.setTransition(null);
    executionContext.setTransitionSource(null);

    // execute the node
    if (isAsync) {
      ExecuteNodeJob job = createAsyncContinuationJob(token);
      executionContext.getJbpmContext().getServices().getMessageService().send(job);
      token.lock(job.toString());
    }
    else {
      execute(executionContext);
    }
  }

  protected ExecuteNodeJob createAsyncContinuationJob(Token token) {
    ExecuteNodeJob job = new ExecuteNodeJob(token);
    job.setNode(this);
    job.setDueDate(new Date());
    job.setExclusive(isAsyncExclusive);
    return job;
  }

  /**
   * override this method to customize the node behaviour.
   */
  public void execute(ExecutionContext executionContext) {
    // if there is a custom action associated with this node
    if (action != null) {
      // execute the action
      executeAction(action, executionContext);
    }
    else {
      // leave the node over the default transition
      leave(executionContext);
    }
  }

  /**
   * called by the implementation of this node to continue execution over the default
   * transition.
   */
  public void leave(ExecutionContext executionContext) {
    leave(executionContext, getDefaultLeavingTransition());
  }

  /**
   * called by the implementation of this node to continue execution over the specified
   * transition.
   */
  public void leave(ExecutionContext executionContext, String transitionName) {
    Transition transition = getLeavingTransition(transitionName);
    if (transition == null) throw new JbpmException("no such transition: " + transitionName);

    leave(executionContext, transition);
  }

  /**
   * called by the implementation of this node to continue execution over the given transition.
   */
  public void leave(ExecutionContext executionContext, Transition transition) {
    if (transition == null) throw new JbpmException("transition is null");

    Token token = executionContext.getToken();
    token.setNode(this);
    executionContext.setTransition(transition);

    // fire the leave-node event for this node
    fireEvent(Event.EVENTTYPE_NODE_LEAVE, executionContext);

    // log this node
    if (token.getNodeEnter() != null) {
      addNodeLog(token);
    }

    // update the runtime information for taking the transition
    // the transitionSource is used to calculate events on superstates
    executionContext.setTransitionSource(this);

    // take the transition
    transition.take(executionContext);
  }

  protected void addNodeLog(Token token) {
    token.addLog(new NodeLog(this, token.getNodeEnter(), Clock.getCurrentTime()));
  }

  // ///////////////////////////////////////////////////////////////////////////

  public ProcessDefinition getProcessDefinition() {
    return superState != null ? superState.getProcessDefinition() : processDefinition;
  }

  // change the name of a node ////////////////////////////////////////////////
  /**
   * updates the name of this node
   */
  public void setName(String name) {
    if (isDifferent(this.name, name)) {
      String oldName = this.name;
      if (superState != null) {
        if (superState.hasNode(name)) {
          throw new IllegalArgumentException("Super state '" + superState + "' already has a node named " + name);
        }
        Map nodes = superState.getNodesMap();
        nodes.remove(oldName);
        nodes.put(name, this);
      }
      else if (processDefinition != null) {
        if (processDefinition.hasNode(name)) {
          throw new IllegalArgumentException("Process definition '" + processDefinition + "' contains two nodes with name " + name);
        }
        Map nodeMap = processDefinition.getNodesMap();
        nodeMap.remove(oldName);
        nodeMap.put(name, this);
      }
      this.name = name;
    }
  }

  private static boolean isDifferent(String name1, String name2) {
    return !(name1 != null ? name1.equals(name2) : name2 == null);
  }

  /**
   * the slash separated name that includes all the superstate names.
   */
  public String getFullyQualifiedName() {
    return superState != null ? superState.getFullyQualifiedName() + '/' + name : name;
  }

  /** indicates wether this node is a superstate. */
  public boolean isSuperStateNode() {
    return false;
  }

  /** returns a list of child nodes (only applicable for {@link SuperState})s. */
  public List getNodes() {
    return null;
  }

  // getters and setters //////////////////////////////////////////////////////

  public SuperState getSuperState() {
    return superState;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
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

  public void setAsyncExclusive(boolean isAsyncExclusive) {
    this.isAsyncExclusive = isAsyncExclusive;
    if (isAsyncExclusive) isAsync = true;
  }
}
