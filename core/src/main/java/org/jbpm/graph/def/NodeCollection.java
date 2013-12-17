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
import java.util.*;

/**
 * is a common supertype for a ProcessDefinition and a SuperState.
 */
public interface NodeCollection extends Serializable {
  
  /**
   * is the ordered list of nodes.
   */
  List getNodes();

  /**
   * maps node-names to nodes.  returns an empty map if 
   * no nodes are contained.
   */
  Map getNodesMap();

  /**
   * retrieves a node by name.
   * @return the node or null if no such node is present.
   */
  Node getNode(String name);
  
  /**
   * is true if this node-collection contains a node with the 
   * given name, false otherwise. 
   */
  boolean hasNode(String name);

  /**
   * adds the given node to this node-collection.
   * @return the added node.
   * @throws IllegalArgumentException if node is null.
   */
  Node addNode(Node node);

  /**
   * removes the given node from this node-collection.
   * @return the removed node or null if the node was not present in this collection.
   * @throws IllegalArgumentException if node is null or if the node is not contained in this nodecollection.
   */
  Node removeNode(Node node);

  /**
   * changes the order of the nodes : the node on oldIndex 
   * is removed and inserted in the newIndex. All nodes inbetween 
   * the old and the new index shift one index position.
   * @throws IndexOutOfBoundsException
   */
  void reorderNode(int oldIndex, int newIndex);

  /**
   * generates a new name for a node to be added to this collection.
   */
  String generateNodeName();
  
  /**
   * finds the node by the given hierarchical name.  use .. for 
   * the parent, use slashes '/' to separate the node names.
   */
  Node findNode(String hierarchicalName);
}
