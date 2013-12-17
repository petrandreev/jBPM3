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
package org.jbpm.msg.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class AsyncExecutionDbTest extends AbstractDbTestCase {

  static List recordedNodes = new ArrayList();

  public static class RecordNode implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Node node = executionContext.getNode();
      recordedNodes.add(node.getName());
      node.leave(executionContext);
    }
  }

  public void testAsyncExecution() throws Exception {

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='one' />"
        + "  </start-state>"
        + "  <node async='true' name='one'>"
        + "    <action class='org.jbpm.msg.command.AsyncExecutionDbTest$RecordNode' />"
        + "    <transition to='two' />"
        + "  </node>"
        + "  <node async='exclusive' name='two'>"
        + "    <action class='org.jbpm.msg.command.AsyncExecutionDbTest$RecordNode' />"
        + "    <transition to='three' />"
        + "  </node>"
        + "  <node async='true' name='three'>"
        + "    <action class='org.jbpm.msg.command.AsyncExecutionDbTest$RecordNode' />"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      jbpmContext.save(processInstance);

      assertEquals(processDefinition.getNode("one"), processInstance.getRootToken().getNode());
      assertEquals(1, getNbrOfJobsAvailable());

      processJobs(5000);

      assertEquals(0, getNbrOfJobsAvailable());

      List expectedNodes = new ArrayList();
      expectedNodes.add("one");
      expectedNodes.add("two");
      expectedNodes.add("three");

      assertEquals(expectedNodes, recordedNodes);

      processDefinition = graphSession.loadProcessDefinition(processDefinition.getId());
      processInstance = graphSession.loadProcessInstance(processInstance.getId());
      assertTrue(processInstance.hasEnded());
      assertEquals(processDefinition.getNode("end"), processInstance.getRootToken().getNode());
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }

  }

  static Set recordedActionNumbers = new HashSet();

  public static class RecordAction implements ActionHandler {

    private static final long serialVersionUID = 1L;
    String nbr;

    public void execute(ExecutionContext executionContext) throws Exception {
      recordedActionNumbers.add(nbr);
    }
  }

  public void testAsyncAction() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <event type='process-start'>"
        + "    <action async='true' class='"
        + RecordAction.class.getName()
        + "'>"
        + "      <nbr>1</nbr>"
        + "    </action>"
        + "    <action async='exclusive' class='"
        + RecordAction.class.getName()
        + "'>"
        + "      <nbr>2</nbr>"
        + "    </action>"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='one'>"
        + "      <action async='true' class='"
        + RecordAction.class.getName()
        + "'>"
        + "        <nbr>3</nbr>"
        + "      </action>"
        + "      <action async='exclusive' class='"
        + RecordAction.class.getName()
        + "'>"
        + "        <nbr>4</nbr>"
        + "      </action>"
        + "    </transition>"
        + "  </start-state>"
        + "  <node name='one'>"
        + "    <event type='node-enter'>"
        + "      <action async='true' class='"
        + RecordAction.class.getName()
        + "'>"
        + "        <nbr>5</nbr>"
        + "      </action>"
        + "      <action async='exclusive' class='"
        + RecordAction.class.getName()
        + "'>"
        + "        <nbr>6</nbr>"
        + "      </action>"
        + "    </event>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    processDefinition = saveAndReload(processDefinition);
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      jbpmContext.save(processInstance);
      assertEquals(processDefinition.getNode("end"), processInstance.getRootToken().getNode());
      assertEquals(6, getNbrOfJobsAvailable());
      assertEquals(0, recordedActionNumbers.size());

      processJobs(5000);

      assertEquals(0, getNbrOfJobsAvailable());

      HashSet expected = new HashSet();
      expected.add("1");
      expected.add("2");
      expected.add("3");
      expected.add("4");
      expected.add("5");
      expected.add("6");

      assertEquals(expected, recordedActionNumbers);
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }

  }
}
