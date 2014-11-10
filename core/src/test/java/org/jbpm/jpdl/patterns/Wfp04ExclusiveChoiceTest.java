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
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_4.swf
 * 
 * <p>
 * we make a distinction in the api between process and client based decisions.
 * the first tests show the situations as described in the pattern. after that a
 * demonstration of client based decision is added.
 * </p>
 * 
 * <p>
 * process based decisions makes use of a decision node. the node has a piece of
 * programming logic associated that calculates the leaving transition name. the
 * programming logic is executed within the calculation of the next state of the
 * process instance.
 * </p>
 * 
 * <p>
 * client based decisions allow clients to select on of the multiple transitions
 * that leave the current state.
 * </p>
 */
public class Wfp04ExclusiveChoiceTest extends AbstractJbpmTestCase {

  static ProcessDefinition exclusiveChoiceProcessDefinition =
    ProcessDefinition.parseXmlString("<process-definition>" + "  <start-state name='start'>"
      + "    <transition to='a' />" + "  </start-state>" + "  <state name='a'>"
      + "    <transition to='xor' />" + "  </state>" + "  <decision name='xor'>"
      + "    <transition name='forget about it' to='d'/>"
      + "    <transition name='urgent' to='b'>" + "      <condition>#{scenario==1}</condition>"
      + "    </transition>" + "    <transition name='dont care' to='c'>"
      + "      <condition>#{scenario==2}</condition>" + "    </transition>" + "  </decision>"
      + "  <state name='b' />" + "  <state name='c' />" + "  <state name='d' />"
      + "</process-definition>");
  static {
    exclusiveChoiceProcessDefinition.addDefinition(new ContextDefinition());
  }

  /**
   * situation 1
   */
  public void testExclusiveChoiceSituation1() {
    ProcessDefinition pd = exclusiveChoiceProcessDefinition;

    ProcessInstance pi = new ProcessInstance(pd);
    pi.signal();

    Token root = pi.getRootToken();
    assertSame(pd.getNode("a"), root.getNode());

    pi.getContextInstance().setVariable("scenario", new Integer(1));
    root.signal();

    assertSame(pd.getNode("b"), root.getNode());
  }

  /**
   * situation 2
   */
  public void testExclusiveChoiceSituation2() {
    ProcessDefinition pd = exclusiveChoiceProcessDefinition;

    ProcessInstance pi = new ProcessInstance(pd);
    pi.signal();

    Token root = pi.getRootToken();
    assertSame(pd.getNode("a"), root.getNode());

    pi.getContextInstance().setVariable("scenario", new Integer(2));
    root.signal();

    assertSame(pd.getNode("c"), root.getNode());
  }

}
