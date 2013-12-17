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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.xml.JpdlXmlReader;

/**
 * brings hierarchy into the elements of a process definition by creating a
 * parent-child relation between {@link GraphElement}s.
 */
public class SuperState extends Node implements NodeCollection {

  private static final long serialVersionUID = 1L;

  protected List nodes = null;
  transient Map nodesMap = null;

  public SuperState() {
  }

  public SuperState(String name) {
    super(name);
  }

  // event types //////////////////////////////////////////////////////////////

  private static final String[] EVENT_TYPES = {
    Event.EVENTTYPE_NODE_ENTER,
    Event.EVENTTYPE_NODE_LEAVE,
    Event.EVENTTYPE_TASK_CREATE,
    Event.EVENTTYPE_TASK_ASSIGN,
    Event.EVENTTYPE_TASK_START,
    Event.EVENTTYPE_TASK_END,
    Event.EVENTTYPE_TRANSITION,
    Event.EVENTTYPE_BEFORE_SIGNAL,
    Event.EVENTTYPE_AFTER_SIGNAL,
    Event.EVENTTYPE_SUPERSTATE_ENTER,
    Event.EVENTTYPE_SUPERSTATE_LEAVE,
    Event.EVENTTYPE_SUBPROCESS_CREATED,
    Event.EVENTTYPE_SUBPROCESS_END,
    Event.EVENTTYPE_TIMER
  };

  /**
   * @deprecated arrays are mutable and thus vulnerable to external
   * manipulation. use {@link #getSupportedEventTypes()} instead
   */
  public static final String[] supportedEventTypes = (String[]) EVENT_TYPES.clone();

  public String[] getSupportedEventTypes() {
    return (String[]) EVENT_TYPES.clone();
  }

  // xml //////////////////////////////////////////////////////////////////////

  public void read(Element element, JpdlXmlReader jpdlReader) {
    jpdlReader.readNodes(element, this);
  }

  // behaviour ////////////////////////////////////////////////////////////////

  public void execute(ExecutionContext executionContext) {
    if ((nodes == null) || (nodes.size() == 0)) {
      throw new JbpmException("transition enters superstate +" + this
        + "' and it there is no first child-node to delegate to");
    }
    Node startNode = (Node) nodes.get(0);
    startNode.enter(executionContext);
  }

  // nodes ////////////////////////////////////////////////////////////////////

  // javadoc description in NodeCollection
  public List getNodes() {
    return nodes;
  }

  // javadoc description in NodeCollection
  public Map getNodesMap() {
    if ((nodesMap == null) && (nodes != null)) {
      nodesMap = new HashMap();
      Iterator iter = nodes.iterator();
      while (iter.hasNext()) {
        Node node = (Node) iter.next();
        nodesMap.put(node.getName(), node);
      }
    }
    return nodesMap;
  }

  // javadoc description in NodeCollection
  public Node getNode(String name) {
    return (Node) getNodesMap().get(name);
  }

  // javadoc description in NodeCollection
  public boolean hasNode(String name) {
    return getNodesMap().containsKey(name);
  }

  // javadoc description in NodeCollection
  public Node addNode(Node node) {
    if (node == null)
      throw new IllegalArgumentException("can't add a null node to a superstate");
    if (nodes == null) nodes = new ArrayList();
    nodes.add(node);
    node.superState = this;
    nodesMap = null;
    return node;
  }

  // javadoc description in NodeCollection
  public Node removeNode(Node node) {
    Node removedNode = null;
    if (node == null)
      throw new IllegalArgumentException("can't remove a null node from a superstate");
    if (nodes != null) {
      if (nodes.remove(node)) {
        removedNode = node;
        removedNode.superState = null;
        nodesMap = null;
      }
    }
    return removedNode;
  }

  // javadoc description in NodeCollection
  public void reorderNode(int oldIndex, int newIndex) {
    if ((nodes != null) && (Math.min(oldIndex, newIndex) >= 0)
      && (Math.max(oldIndex, newIndex) < nodes.size())) {
      Object o = nodes.remove(oldIndex);
      nodes.add(newIndex, o);
    }
    else {
      throw new IndexOutOfBoundsException("couldn't reorder element from index '" + oldIndex
        + "' to index '" + newIndex + "' in nodeList '" + nodes + "'");
    }
  }

  // javadoc description in NodeCollection
  public String generateNodeName() {
    return ProcessDefinition.generateNodeName(nodes);
  }

  // javadoc description in NodeCollection
  public Node findNode(String hierarchicalName) {
    return ProcessDefinition.findNode(this, hierarchicalName);
  }

  /**
   * recursively checks if the given node is one of the descendants of this
   * supernode.
   */
  public boolean containsNode(Node node) {
    boolean containsNode = false;
    SuperState parent = node.getSuperState();
    while ((!containsNode) && (parent != null)) {
      if (this.equals(parent)) {
        containsNode = true;
      }
      else {
        parent = parent.getSuperState();
      }
    }
    return containsNode;
  }

  // other ////////////////////////////////////////////////////////////////////

  public GraphElement getParent() {
    GraphElement parent = processDefinition;
    if (superState != null) {
      parent = superState;
    }
    return parent;
  }

  public boolean isSuperStateNode() {
    return true;
  }
}