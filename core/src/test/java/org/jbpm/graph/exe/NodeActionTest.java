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
package org.jbpm.graph.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;

public class NodeActionTest extends AbstractJbpmTestCase {

  private ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
    "<process-definition>" +
    "  <start-state>" +
    "    <transition to='n' />" +
    "  </start-state>" +
    "  <node name='n'>" +
    "    <action class='org.jbpm.graph.exe.NodeActionTest$RuntimeCalculation'/>" +
    "    <transition name='a' to='a' />" +
    "    <transition name='b' to='b' />" +
    "    <transition name='c' to='c' />" +
    "  </node>" +
    "  <state name='a' />" +
    "  <state name='b' />" +
    "  <state name='c' />" +
    "  <task name='undress' />" +
    "</process-definition>"
  );
        
  private ProcessInstance processInstance = new ProcessInstance(processDefinition);
  private Token token = processInstance.getRootToken();
  private Node n = processDefinition.getNode("n");
  private Node a = processDefinition.getNode("a");
  private Node b = processDefinition.getNode("b");
  private Node c = processDefinition.getNode("c");

  static int scenario = 0;

  public static class RuntimeCalculation implements ActionHandler {
    private static final long serialVersionUID = 1L;
    public void execute(ExecutionContext executionContext) throws Exception {
      if (scenario==1) {
        executionContext.leaveNode("a");
      } else if (scenario==2) {
        executionContext.leaveNode("b");
      } else if (scenario==3) {
        executionContext.leaveNode("c");
      } else if (scenario==4) {
        // do nothing and behave like a state
      }
    }
  }

  public void testSituation1() {
    scenario = 1;
    processInstance.signal();
    assertSame(a, token.getNode());
  }

  public void testSituation2() {
    scenario = 2;
    processInstance.signal();
    assertSame(b, token.getNode());
  }

  public void testSituation3() {
    scenario = 3;
    processInstance.signal();
    assertSame(c, token.getNode());
  }

  public void testSituation4() {
    scenario = 4;
    processInstance.signal();
    assertSame(n, token.getNode());
  }
}
