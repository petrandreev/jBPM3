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
package org.jbpm.jpdl.patterns;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_1.swf
 */
public class Wfp01SequenceTest extends AbstractJbpmTestCase {

  public void testSequence() {
    // create a simple definition with 3 states in sequence
    ProcessDefinition pd = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <state name='a'>" +
      "    <transition to='b' />" +
      "  </state>" +
      "  <state name='b'>" +
      "    <transition to='c' />" +
      "  </state>" +
      "  <state name='c'>" +
      "    <transition to='end' />" +
      "  </state>" +
      "  <end-state name='end'/>" +
      "</process-definition>"
    );

    ProcessInstance pi = new ProcessInstance( pd );
    pi.signal();
    Token token = pi.getRootToken();
    assertSame( pd.getNode("a"), token.getNode() );
    
    token.signal();
    assertSame( pd.getNode("b"), token.getNode() );

    token.signal();
    assertSame( pd.getNode("c"), token.getNode() );

    token.signal();
    assertSame( pd.getNode("end"), token.getNode() );
  }
}
