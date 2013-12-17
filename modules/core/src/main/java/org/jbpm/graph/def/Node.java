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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.jbpm.msg.MessageService;
import org.jbpm.svc.Services;
import org.jbpm.util.Clock;

public class Node extends GraphElement implements Parsable
{

  private static final long serialVersionUID = 1L;
  
  public enum NodeType { Node, StartState, EndState, State, Task, Fork, Join, Decision };

  protected List<Transition> leavingTransitions = null;
  transient Map<String, Transition> leavingTransitionMap = null;
  protected Set<Transition> arrivingTransitions = null;
  protected Action action = null;
  protected SuperState superState = null;
  protected boolean isAsync = false;
  protected boolean isAsyncExclusive = false;

  
  public NodeType getNodeType()
  {
    return NodeType.Node;
  }
  
  public String getNameExt()
  {
    String name = super.getName();
    if (name == null)
      name = "#anonymous" + getNodeType();
    
    return name;
  }

  // event types //////////////////////////////////////////////////////////////

  public static final String[] supportedEventTypes = new String[] { 
    Event.EVENTTYPE_NODE_ENTER, 
    Event.EVENTTYPE_NODE_LEAVE, 
    Event.EVENTTYPE_BEFORE_SIGNAL,
    Event.EVENTTYPE_AFTER_SIGNAL 
  };

  public String[] getSupportedEventTypes()
  {
    return supportedEventTypes;
  }

  // constructors /////////////////////////////////////////////////////////////

  /**
   * creates an unnamed node.
   */
  public Node()
  {
  }

  /**
   * creates a node with the given name.
   */
  public Node(String name)
  {
    super(name);
  }

  public void read(Element nodeElement, JpdlXmlReader jpdlXmlReader)
  {
    action = jpdlXmlReader.readSingleAction(nodeElement);
  }

  public void write(Element nodeElement)
  {
    if (action != null)
    {
      String actionName = ActionTypes.getActionName(action.getClass());
      Element actionElement = nodeElement.addElement(actionName);
      action.write(actionElement);
    }
  }

  // leaving transitions //////////////////////////////////////////////////////

  public List<Transition> getLeavingTransitions()
  {
    return leavingTransitions;
  }

  public List<Transition> getLeavingTransitionsList()
  {
    return leavingTransitions;
  }

  /**
   * are the leaving {@link Transition}s, mapped by their name (java.lang.String).
   */
  public Map<String, Transition> getLeavingTransitionsMap()
  {
    if ((leavingTransitionMap == null) && (leavingTransitions != null))
    {
      // initialize the cached leaving transition map
      leavingTransitionMap = new HashMap<String, Transition>();
      ListIterator<Transition> iter = leavingTransitions.listIterator(leavingTransitions.size());
      while (iter.hasPrevious())
      {
        Transition leavingTransition = iter.previous();
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
  public Transition addLeavingTransition(Transition leavingTransition)
  {
    if (leavingTransition == null)
      throw new IllegalArgumentException("can't add a null leaving transition to an node");
    if (leavingTransitions == null)
      leavingTransitions = new ArrayList<Transition>();
    leavingTransitions.add(leavingTransition);
    leavingTransition.from = this;
    leavingTransitionMap = null;
    return leavingTransition;
  }

  /**
   * removes the bidirection relation between this node and the given leaving transition.
   * 
   * @throws IllegalArgumentException if leavingTransition is null.
   */
  public void removeLeavingTransition(Transition leavingTransition)
  {
    if (leavingTransition == null)
      throw new IllegalArgumentException("can't remove a null leavingTransition from an node");
    if (leavingTransitions != null)
    {
      if (leavingTransitions.remove(leavingTransition))
      {
        leavingTransition.from = null;
        leavingTransitionMap = null;
      }
    }
  }

  /**
   * checks for the presence of a leaving transition with the given name.
   * 
   * @return true if this node has a leaving transition with the given name, false otherwise.
   */
  public boolean hasLeavingTransition(String transitionName)
  {
    if (leavingTransitions == null)
      return false;
    return getLeavingTransitionsMap().containsKey(transitionName);
  }

  /**
   * retrieves a leaving transition by name. note that also the leaving transitions of the supernode are taken into
   * account.
   */
  public Transition getLeavingTransition(String transitionName)
  {
    Transition transition = null;
    if (leavingTransitions != null)
    {
      transition = getLeavingTransitionsMap().get(transitionName);
    }
    if ((transition == null) && (superState != null))
    {
      transition = superState.getLeavingTransition(transitionName);
    }
    return transition;
  }

  /**
   * true if this transition has leaving transitions.
   */
  public boolean hasNoLeavingTransitions()
  {
    return (((leavingTransitions == null) || (leavingTransitions.size() == 0)) && ((superState == null) || (superState.hasNoLeavingTransitions())));
  }

  /**
   * generates a new name for a transition that will be added as a leaving transition.
   */
  public String generateNextLeavingTransitionName()
  {
    String name = null;
    if (leavingTransitions != null && containsName(leavingTransitions, null))
    {
      int n = 1;
      while (containsName(leavingTransitions, Integer.toString(n)))
        n++;
      name = Integer.toString(n);
    }
    return name;
  }

  boolean containsName(List<Transition> leavingTransitions, String name)
  {
    if (name != null) {
      for (Transition transition : leavingTransitions) {
        if (name.equals(transition.getName()))
          return true;
      }
    }
    else {
      for (Transition transition : leavingTransitions) {
        if (transition.getName() == null)
          return true;
      }
    }
    return false;
  }

  // default leaving transition and leaving transition ordering ///////////////

  /**
   * is the default leaving transition.
   */
  public Transition getDefaultLeavingTransition()
  {
    Transition defaultTransition = null;
    if (leavingTransitions != null)
    {
      // Select the first unconditional transition
      for (Transition auxTransition : leavingTransitions)
      {
        if (auxTransition.getCondition() == null)
        {
          defaultTransition = auxTransition;
          break;
        }
      }
    }
    else if (superState != null)
    {
      defaultTransition = superState.getDefaultLeavingTransition();
    }
    return defaultTransition;
  }

  /**
   * moves one leaving transition from the oldIndex and inserts it at the newIndex.
   */
  public void reorderLeavingTransition(int oldIndex, int newIndex)
  {
    if ((leavingTransitions != null) && (Math.min(oldIndex, newIndex) >= 0) && (Math.max(oldIndex, newIndex) < leavingTransitions.size()))
    {
      Transition o = leavingTransitions.remove(oldIndex);
      leavingTransitions.add(newIndex, o);
    }
  }

  // arriving transitions /////////////////////////////////////////////////////

  /**
   * are the arriving transitions.
   */
  public Set<Transition> getArrivingTransitions()
  {
    return arrivingTransitions;
  }

  /**
   * add a bidirection relation between this node and the given arriving transition.
   * 
   * @throws IllegalArgumentException if t is null.
   */
  public Transition addArrivingTransition(Transition arrivingTransition)
  {
    if (arrivingTransition == null)
      throw new IllegalArgumentException("can't add a null arrivingTransition to a node");
    if (arrivingTransitions == null)
      arrivingTransitions = new HashSet<Transition>();
    arrivingTransitions.add(arrivingTransition);
    arrivingTransition.to = this;
    return arrivingTransition;
  }

  /**
   * removes the bidirection relation between this node and the given arriving transition.
   * 
   * @throws IllegalArgumentException if t is null.
   */
  public void removeArrivingTransition(Transition arrivingTransition)
  {
    if (arrivingTransition == null)
      throw new IllegalArgumentException("can't remove a null arrivingTransition from a node");
    if (arrivingTransitions != null)
    {
      if (arrivingTransitions.remove(arrivingTransition))
      {
        arrivingTransition.to = null;
      }
    }
  }

  // various //////////////////////////////////////////////////////////////////

  /**
   * is the {@link SuperState} or the {@link ProcessDefinition} in which this node is contained.
   */
  public GraphElement getParent()
  {
    GraphElement parent = processDefinition;
    if (superState != null)
      parent = superState;
    return parent;
  }

  // behaviour methods ////////////////////////////////////////////////////////

  /**
   * called by a transition to pass execution to this node.
   */
  public void enter(ExecutionContext executionContext)
  {
    Token token = executionContext.getToken();

    // update the runtime context information
    token.setNode(this);

    // fire the leave-node event for this node
    fireEvent(Event.EVENTTYPE_NODE_ENTER, executionContext);

    // keep track of node entrance in the token, so that a node-log can be generated at node leave time.
    token.setNodeEnter(Clock.getCurrentTime());

    // remove the transition references from the runtime context
    executionContext.setTransition(null);
    executionContext.setTransitionSource(null);

    // execute the node
    if (isAsync)
    {
      ExecuteNodeJob job = createAsyncContinuationJob(token);
      MessageService messageService = (MessageService)Services.getCurrentService(Services.SERVICENAME_MESSAGE);
      messageService.send(job);
      token.lock(job.toString());
    }
    else
    {
      execute(executionContext);
    }
  }

  protected ExecuteNodeJob createAsyncContinuationJob(Token token)
  {
    ExecuteNodeJob job = new ExecuteNodeJob(token);
    job.setNode(this);
    job.setDueDate(new Date());
    job.setExclusive(isAsyncExclusive);
    return job;
  }

  /**
   * override this method to customize the node behaviour.
   */
  public void execute(ExecutionContext executionContext)
  {
    // if there is a custom action associated with this node
    if (action != null)
    {
      try
      {
        // execute the action
        executeAction(action, executionContext);

      }
      catch (Exception exception)
      {
        // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
        // search for an exception handler or throw to the client
        raiseException(exception, executionContext);
      }

    }
    else
    {
      // let this node handle the token
      // the default behaviour is to leave the node over the default transition.
      leave(executionContext);
    }
  }

  /**
   * called by the implementation of this node to continue execution over the default transition.
   */
  public void leave(ExecutionContext executionContext)
  {
    leave(executionContext, getDefaultLeavingTransition());
  }

  /**
   * called by the implementation of this node to continue execution over the specified transition.
   */
  public void leave(ExecutionContext executionContext, String transitionName)
  {
    Transition transition = getLeavingTransition(transitionName);
    if (transition == null)
    {
      throw new JbpmException("transition '" + transitionName + "' is not a leaving transition of node '" + this + "'");
    }
    leave(executionContext, transition);
  }

  /**
   * called by the implementation of this node to continue execution over the given transition.
   */
  public void leave(ExecutionContext executionContext, Transition transition)
  {
    if (transition == null)
      throw new JbpmException("can't leave node '" + this + "' without leaving transition");
    Token token = executionContext.getToken();
    token.setNode(this);
    executionContext.setTransition(transition);

    // fire the leave-node event for this node
    fireEvent(Event.EVENTTYPE_NODE_LEAVE, executionContext);

    // log this node
    if (token.getNodeEnter() != null)
    {
      addNodeLog(token);
    }

    // update the runtime information for taking the transition
    // the transitionSource is used to calculate events on superstates
    executionContext.setTransitionSource(this);

    // take the transition
    transition.take(executionContext);
  }

  protected void addNodeLog(Token token)
  {
    token.addLog(new NodeLog(this, token.getNodeEnter(), Clock.getCurrentTime()));
  }

  // ///////////////////////////////////////////////////////////////////////////

  public ProcessDefinition getProcessDefinition()
  {
    ProcessDefinition pd = this.processDefinition;
    if (superState != null)
    {
      pd = superState.getProcessDefinition();
    }
    return pd;
  }

  // change the name of a node ////////////////////////////////////////////////
  /**
   * updates the name of this node
   */
  public void setName(String name)
  {
    if (name != null && name.contains("/"))
      throw new IllegalArgumentException("Invalid node name: " + name);

    if (isDifferent(this.name, name))
    {
      String oldName = this.name;
      if (superState != null)
      {
        if (superState.hasNode(name))
        {
          throw new IllegalArgumentException("couldn't set name '" + name + "' on node '" + this
              + "'cause the superState of this node has already another child node with the same name");
        }
        Map<String, Node> nodes = superState.getNodesMap();
        nodes.remove(oldName);
        nodes.put(name, this);
      }
      else if (processDefinition != null)
      {
        if (processDefinition.hasNode(name))
        {
          throw new IllegalArgumentException("couldn't set name '" + name + "' on node '" + this
              + "'cause the process definition of this node has already another node with the same name");
        }
        Map<String, Node> nodeMap = processDefinition.getNodesMap();
        nodeMap.remove(oldName);
        nodeMap.put(name, this);
      }
      this.name = name;
    }
  }

  boolean isDifferent(String name1, String name2)
  {
    if ((name1 != null) && (name1.equals(name2)))
    {
      return false;
    }
    else if ((name1 == null) && (name2 == null))
    {
      return false;
    }
    return true;
  }

  /**
   * the slash separated name that includes all the superstate names.
   */
  public String getFullyQualifiedName()
  {
    String fullyQualifiedName = name;
    if (superState != null)
    {
      fullyQualifiedName = superState.getFullyQualifiedName() + "/" + name;
    }
    return fullyQualifiedName;
  }

  /** indicates wether this node is a superstate. */
  public boolean isSuperStateNode()
  {
    return false;
  }

  /** returns a list of child nodes (only applicable for {@link SuperState})s. */
  public List<Node> getNodes()
  {
    return null;
  }

  // getters and setters //////////////////////////////////////////////////////

  public SuperState getSuperState()
  {
    return superState;
  }

  public Action getAction()
  {
    return action;
  }

  public void setAction(Action action)
  {
    this.action = action;
  }

  public boolean isAsync()
  {
    return isAsync;
  }

  public void setAsync(boolean isAsync)
  {
    this.isAsync = isAsync;
  }

  public boolean isAsyncExclusive()
  {
    return isAsyncExclusive;
  }

  public void setAsyncExclusive(boolean isAsyncExclusive)
  {
    this.isAsyncExclusive = isAsyncExclusive;
  }
}
