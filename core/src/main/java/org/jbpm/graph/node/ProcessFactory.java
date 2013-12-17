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
package org.jbpm.graph.node;

import org.jbpm.JbpmException;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;

public class ProcessFactory {

  public static ProcessDefinition createProcessDefinition(String[] nodes, String[] transitions) {
    ProcessDefinition pd = new ProcessDefinition();
    addNodesAndTransitions(pd, nodes, transitions);
    return pd;
  }

  public static void addNodesAndTransitions(ProcessDefinition pd, String[] nodes, String[] transitions) {
    for ( int i = 0; i < nodes.length; i++ ) {
      pd.addNode( createNode( nodes[i] ) );
    }

    for ( int i = 0; i < transitions.length; i++ ) {
      String[] parsedTransition = cutTransitionText( transitions[i] );
      Node from = pd.getNode( parsedTransition[0] );
      Node to = pd.getNode( parsedTransition[2] );
      Transition t = new Transition( parsedTransition[1] );
      t.setProcessDefinition(pd);
      from.addLeavingTransition(t);
      to.addArrivingTransition(t);
    }
  }

  public static String getTypeName(Node node) {
    if (node==null) return null;
    return NodeTypes.getNodeName(node.getClass());
  }

  /**
   * @throws JbpmException if text is null.
   */
  public static Node createNode(String text) {
    if (text==null) throw new JbpmException("text is null");
    
    Node node = null;
    
    String typeName;
    String name;
    
    text = text.trim();
    int spaceIndex = text.indexOf(' ');
    if (spaceIndex!=-1) {
      typeName = text.substring(0, spaceIndex);
      name = text.substring(spaceIndex + 1);
    } else {
      typeName = text;
      name = null;
    }

    Class nodeType = NodeTypes.getNodeType(typeName);
    if ( nodeType==null ) throw new IllegalArgumentException("unknown node type name '" + typeName + "'");
    try {
      node = (Node) nodeType.newInstance();
      node.setName(name);
    } catch (Exception e) {
      throw new JbpmException("couldn't instantiate nodehandler for type '" + typeName + "'");
    }
    return node;
  }

  public static String[] cutTransitionText(String transitionText) {
    String[] parts = new String[3];
    if ( transitionText == null ) {
      throw new JbpmException( "transitionText is null" );
    }
    int start = transitionText.indexOf( "--" );
    if ( start == -1 ) {
      throw new JbpmException( "incorrect transition format exception : nodefrom --transitionname--> nodeto" );
    }
    parts[0] = transitionText.substring(0,start).trim();

    int end = transitionText.indexOf( "-->", start );
    if ( start < end ) {
      parts[1] = transitionText.substring(start+2,end).trim();
    } else {
      parts[1] = null;
    }
    parts[2] = transitionText.substring(end+3).trim();
    return parts;
  }
}
