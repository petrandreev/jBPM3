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

import java.util.Collection;

import org.dom4j.Element;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.xml.JpdlXmlReader;

/**
 * a interleaving end node should have 2 leaving transitions.
 * one with the name 'back' that has the interleaving start node as 
 * destinationNode.  and one with the name 'done' that specifies the 
 * destinationNode in case the interleaving is done.   
 * Alternatively, the back and done transitions can be specified 
 * in this interleave handler.
 */
public class InterleaveEnd extends Node {
  
  private static final long serialVersionUID = 1L;
  
  Transition back = null;
  Transition done = null;

  public InterleaveEnd() {
  }

  public InterleaveEnd(String name) {
    super(name);
  }

  public void read(Element element, JpdlXmlReader jpdlReader) {
    // TODO
  }

  public void write(Element element) {
    // TODO
  }

  public void execute(ExecutionContext executionContext) {
    Token token = executionContext.getToken();
    Node interleaveEndNode = token.getNode();
    Collection transitionNames = getInterleaveStart().retrieveTransitionNames(token);
    // if the set is *not* empty
    if ( ! transitionNames.isEmpty() ) {
      // go back to the interleave start handler
      String backTransitionName = "back";
      if ( back != null ) {
        backTransitionName = back.getName();
      }
      interleaveEndNode.leave(executionContext, backTransitionName);
    } else {
      // leave the to the
      getInterleaveStart().removeTransitionNames(token);
      String doneTransitionName = "done";
      if ( done != null ) {
        doneTransitionName = done.getName();
      }
      interleaveEndNode.leave(executionContext, doneTransitionName);
    }
  }
  
  public InterleaveStart getInterleaveStart() {
    return (InterleaveStart) getLeavingTransition("back").getTo();
  }

  public Transition getBack() {
    return back;
  }
  public void setBack(Transition back) {
    this.back = back;
  }
  public Transition getDone() {
    return done;
  }
  public void setDone(Transition done) {
    this.done = done;
  }
}
