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
package org.jbpm.db;

import java.util.List;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class GraphSessionDbTest extends AbstractDbTestCase {

  public void testProcessDefinitionSaveAndLoad() {
    // create a process definition
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    // save it in the database
    graphSession.saveProcessDefinition(processDefinition);
    try {
      // get the assigned id
      long processDefinitionId = processDefinition.getId();
      // start a new transaction
      newTransaction();
      // load the process definition by the id
      processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
      // check the result
      assertEquals("auction", processDefinition.getName());
    }
    finally {
      // cleanup
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testFindProcessDefinitionByNameAndVersion() {
    // put 3 process definitions in the database with the same name, but
    // different versions
    ProcessDefinition processDefinitionOne = new ProcessDefinition("auction");
    processDefinitionOne.setVersion(1);
    graphSession.saveProcessDefinition(processDefinitionOne);

    ProcessDefinition processDefinitionTwo = new ProcessDefinition("auction");
    processDefinitionTwo.setVersion(2);
    graphSession.saveProcessDefinition(processDefinitionTwo);
    // get the assigned id of the second verions
    long secondVersionProcessDefinitionId = processDefinitionTwo.getId();

    ProcessDefinition processDefinitionThree = new ProcessDefinition("auction");
    processDefinitionThree.setVersion(3);
    graphSession.saveProcessDefinition(processDefinitionThree);

    // start a new transaction
    newTransaction();

    // load the process definition by the id
    processDefinitionTwo = graphSession.findProcessDefinition("auction", 2);
    assertEquals(secondVersionProcessDefinitionId, processDefinitionTwo.getId());
    assertEquals("auction", processDefinitionTwo.getName());
    assertEquals(2, processDefinitionTwo.getVersion());

    // start a new transaction
    newTransaction();

    // cleanup
    graphSession.deleteProcessDefinition(processDefinitionOne.getId());
    newTransaction();
    graphSession.deleteProcessDefinition(processDefinitionTwo.getId());
    newTransaction();
    graphSession.deleteProcessDefinition(processDefinitionThree.getId());
  }

  public void testFindLatestProcessDefinition() throws Exception {
    // put 3 process definitions in the database with the same name, but
    // different versions
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(1);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(2);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(3);
    graphSession.saveProcessDefinition(processDefinition);
    // get the assigned id of the last verions
    long lastVersionProcessDefinitionId = processDefinition.getId();

    newTransaction();

    processDefinition = graphSession.findLatestProcessDefinition("auction");
    assertEquals(lastVersionProcessDefinitionId, processDefinition.getId());
    assertEquals("auction", processDefinition.getName());
    assertEquals(3, processDefinition.getVersion());

    newTransaction();

    // cleanup
    processDefinition = graphSession.findProcessDefinition("auction", 1);
    graphSession.deleteProcessDefinition(processDefinition.getId());

    processDefinition = graphSession.findProcessDefinition("auction", 2);
    graphSession.deleteProcessDefinition(processDefinition.getId());

    processDefinition = graphSession.findProcessDefinition("auction", 3);
    graphSession.deleteProcessDefinition(processDefinition.getId());
  }

  public void testFindAllProcessDefinitions() throws Exception {
    ensureCleanProcessDefinitionTable();

    // put 3 process definitions in the database with the same name, but
    // different versions
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(1);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(2);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(3);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    processDefinition.setVersion(1);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    processDefinition.setVersion(2);
    graphSession.saveProcessDefinition(processDefinition);

    newTransaction();

    try {
      List<ProcessDefinition> allProcessDefinitions = graphSession.findAllProcessDefinitions();
      assertEquals(5, allProcessDefinitions.size());
      assertEquals(3, allProcessDefinitions.get(0).getVersion());
      assertEquals("auction", allProcessDefinitions.get(0).getName());
      assertEquals(2, allProcessDefinitions.get(1).getVersion());
      assertEquals("auction", allProcessDefinitions.get(1).getName());
      assertEquals(1, allProcessDefinitions.get(2).getVersion());
      assertEquals("auction", allProcessDefinitions.get(2).getName());
      assertEquals(2, allProcessDefinitions.get(3).getVersion());
      assertEquals("bake cake", allProcessDefinitions.get(3).getName());
      assertEquals(1, allProcessDefinitions.get(4).getVersion());
      assertEquals("bake cake", allProcessDefinitions.get(4).getName());

      newTransaction();
    }
    finally {
      processDefinition = graphSession.findProcessDefinition("auction", 1);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("auction", 2);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("auction", 3);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("bake cake", 1);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("bake cake", 2);
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testFindAllProcessDefinitionVersions() throws Exception {
    // put 3 process definitions in the database with the same name, but
    // different versions
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(1);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(2);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    processDefinition.setVersion(3);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    processDefinition.setVersion(1);
    graphSession.saveProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    processDefinition.setVersion(2);
    graphSession.saveProcessDefinition(processDefinition);

    newTransaction();

    try {
      List<ProcessDefinition> allProcessDefinitionVersions = graphSession.findAllProcessDefinitionVersions("auction");
      assertEquals(3, allProcessDefinitionVersions.size());
      assertEquals(3, allProcessDefinitionVersions.get(0).getVersion());
      assertEquals("auction", allProcessDefinitionVersions.get(0).getName());
      assertEquals(2, allProcessDefinitionVersions.get(1).getVersion());
      assertEquals("auction", allProcessDefinitionVersions.get(1).getName());
      assertEquals(1, allProcessDefinitionVersions.get(2).getVersion());
      assertEquals("auction", allProcessDefinitionVersions.get(2).getName());

      allProcessDefinitionVersions = graphSession.findAllProcessDefinitionVersions("bake cake");
      assertEquals(2, allProcessDefinitionVersions.size());
      assertEquals(2, allProcessDefinitionVersions.get(0).getVersion());
      assertEquals("bake cake", allProcessDefinitionVersions.get(0).getName());
      assertEquals(1, allProcessDefinitionVersions.get(1).getVersion());
      assertEquals("bake cake", allProcessDefinitionVersions.get(1).getName());

      newTransaction();
    }
    finally {
      processDefinition = graphSession.findProcessDefinition("auction", 1);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("auction", 2);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("auction", 3);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("bake cake", 1);
      graphSession.deleteProcessDefinition(processDefinition.getId());

      processDefinition = graphSession.findProcessDefinition("bake cake", 2);
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testSaveAndLoadProcessInstance() {
    ProcessInstance processInstance = new ProcessInstance();
    processInstance = saveAndReload(processInstance);
    try {
      assertNotNull(processInstance);
    }
    finally {
      graphSession.deleteProcessInstance(processInstance);
    }
  }

  public void testUpdateProcessInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='s' />"
        + "  <node name='n' />"
        + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    
    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);

      processInstance = saveAndReload(processInstance);
      long pid = processInstance.getId();

      assertEquals("s", processInstance.getRootToken().getNode().getName());
      processInstance.getRootToken().setNode(processInstance.getProcessDefinition().getNode("n"));

      processInstance = saveAndReload(processInstance);
      assertEquals("n", processInstance.getRootToken().getNode().getName());
      assertEquals(pid, processInstance.getId());

      newTransaction();
    }
    finally {
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testFindProcessInstancesByProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='s' />"
        + "  <node name='n' />"
        + "</process-definition>");

    graphSession.saveProcessDefinition(processDefinition);

    try {
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));

      newTransaction();

      List<ProcessInstance> processInstances = graphSession.findProcessInstances(processDefinition.getId());
      assertEquals(3, processInstances.size());

      // process instances should be ordered from recent to old
      long previousStart = System.currentTimeMillis();
      for (ProcessInstance processInstance : processInstances) {
        long processStart = processInstance.getStart().getTime();
        assertTrue(previousStart >= processStart);
        previousStart = processStart;
      }

      newTransaction();
    }
    finally {
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testDeleteProcessInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='s' />"
        + "  <node name='n' />"
        + "</process-definition>");
    graphSession.saveProcessDefinition(processDefinition);

    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      jbpmContext.save(processInstance);

      newTransaction();

      graphSession.deleteProcessInstance(processInstance.getId());

      newTransaction();

      assertEquals(0, graphSession.findProcessInstances(processDefinition.getId()).size());

      newTransaction();
    }
    finally {
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testDeleteProcessInstanceWithVariables() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state name='start'>"
        + "    <transition to='fork' />"
        + "  </start-state>"
        + "  <fork name='fork'>"
        + "    <transition name='a' to='a' />"
        + "    <transition name='b' to='b' />"
        + "  </fork>"
        + "  <state name='a' />"
        + "  <state name='b' />"
        + "</process-definition>");

    graphSession.saveProcessDefinition(processDefinition);

    try {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      Token tokenA = processInstance.findToken("/a");
      Token tokenB = processInstance.findToken("/b");

      ContextInstance contextInstance = processInstance.getContextInstance();
      contextInstance.setVariable("r", "rrrrrr");
      contextInstance.createVariable("a", "aaaaaa", tokenA);
      contextInstance.createVariable("b", "bbbbbb", tokenB);

      processInstance = saveAndReload(processInstance);

      graphSession.deleteProcessInstance(processInstance);

      newTransaction();

      List<ProcessInstance> processInstances = graphSession.findProcessInstances(processDefinition.getId());
      assertEquals(0, processInstances.size());

      newTransaction();
    }
    finally {
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testDeleteProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='deleteme'>"
        + "  <start-state name='s' />"
        + "  <node name='n' />"
        + "</process-definition>");
    graphSession.saveProcessDefinition(processDefinition);

    try {
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));

      newTransaction();
    }
    finally {
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }

    newTransaction();

    assertEquals(0, graphSession.findAllProcessDefinitionVersions(processDefinition.getName()).size());
    assertEquals(0, graphSession.findProcessInstances(processDefinition.getId()).size());
  }

  public void testLatestProcessDefinitions() {
    ensureCleanProcessDefinitionTable();

    ProcessDefinition websale = new ProcessDefinition("websale");
    jbpmContext.deployProcessDefinition(websale);
    jbpmContext.deployProcessDefinition(websale);
    jbpmContext.deployProcessDefinition(websale);

    ProcessDefinition changeNappy = new ProcessDefinition("change nappy");
    jbpmContext.deployProcessDefinition(changeNappy);
    jbpmContext.deployProcessDefinition(changeNappy);

    newTransaction();

    List<ProcessDefinition> latestProcessDefinitions = graphSession.findLatestProcessDefinitions();
    assertEquals(2, latestProcessDefinitions.size());
    assertEquals(3, getVersionOfProcess("websale", latestProcessDefinitions));
    assertEquals(2, getVersionOfProcess("change nappy", latestProcessDefinitions));

    newTransaction();

    // cleanup
    graphSession.deleteProcessDefinition(websale.getId());
    graphSession.deleteProcessDefinition(changeNappy.getId());
  }

  private void ensureCleanProcessDefinitionTable() {
    List<ProcessDefinition> processDefinitions = graphSession.findAllProcessDefinitions();
    if (!processDefinitions.isEmpty()) {
      System.err.println("FIXME: "+ getClass().getName() + "." + getName() +
          " found " + processDefinitions.size() + " process definitions left over");
      for (ProcessDefinition processDefinition : processDefinitions) {
        graphSession.deleteProcessDefinition(processDefinition);
      }
    }
  }

  private int getVersionOfProcess(String name, List<ProcessDefinition> latestProcessDefinitions) {
    for (ProcessDefinition processDefinition : latestProcessDefinitions) {
      if (name.equals(processDefinition.getName())) {
        return processDefinition.getVersion();
      }
    }
    return -1;
  }
}
