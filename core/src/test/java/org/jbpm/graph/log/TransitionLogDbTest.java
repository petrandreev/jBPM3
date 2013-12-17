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
package org.jbpm.graph.log;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;

public class TransitionLogDbTest extends AbstractDbTestCase {

  public void testTransitionLogTransition() {
    Transition transition = new Transition();
    session.save(transition);
    
    TransitionLog transitionLog = new TransitionLog(transition, null);
    transitionLog = (TransitionLog) saveAndReload(transitionLog);
    assertNotNull(transitionLog.getTransition());
    
    session.delete(transitionLog);
    session.delete(transition);
  }

  public void testTransitionLogSourceNode() {
    Node sourceNode = new Node();
    session.save(sourceNode);
    
    TransitionLog transitionLog = new TransitionLog(null, sourceNode);
    transitionLog = (TransitionLog) saveAndReload(transitionLog);
    assertNotNull(transitionLog.getSourceNode());
    
    session.delete(transitionLog);
    session.delete(sourceNode);
  }

  public void testTransitionLogDestinationNode() {
    Node destinationNode = new Node();
    session.save(destinationNode);
    
    TransitionLog transitionLog = new TransitionLog(null, null);
    transitionLog.setDestinationNode(destinationNode);
    transitionLog = (TransitionLog) saveAndReload(transitionLog);
    assertNotNull(transitionLog.getDestinationNode());

    session.delete(transitionLog);
    session.delete(destinationNode);
  }

}
