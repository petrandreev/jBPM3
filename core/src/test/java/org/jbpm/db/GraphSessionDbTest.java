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

import java.util.Iterator;
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
    deployProcessDefinition(processDefinition);

    // get the assigned id
    long processDefinitionId = processDefinition.getId();
    // start a new transaction
    newTransaction();
    // load the process definition by the id
    processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
    // check the result
    assertEquals("auction", processDefinition.getName());
  }

  public void testFindProcessDefinitionByNameAndVersion() {
    // put 3 process definitions in the database with the same name,
    // but different versions
    ProcessDefinition processDefinitionOne = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinitionOne);

    ProcessDefinition processDefinitionTwo = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinitionTwo);
    // get the assigned id of the second version
    long secondVersionProcessDefinitionId = processDefinitionTwo.getId();

    ProcessDefinition processDefinitionThree = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinitionThree);

    // load the process definition by name and version
    processDefinitionTwo = graphSession.findProcessDefinition("auction", 2);
    assertEquals(secondVersionProcessDefinitionId, processDefinitionTwo.getId());
    assertEquals("auction", processDefinitionTwo.getName());
    assertEquals(2, processDefinitionTwo.getVersion());
  }

  public void testFindLatestProcessDefinition() throws Exception {
    // put 3 process definitions in the database with the same name,
    // but different versions
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);
    // get the assigned id of the last verions
    long lastVersionProcessDefinitionId = processDefinition.getId();

    processDefinition = graphSession.findLatestProcessDefinition("auction");
    assertEquals(lastVersionProcessDefinitionId, processDefinition.getId());
    assertEquals("auction", processDefinition.getName());
    assertEquals(3, processDefinition.getVersion());
  }

  public void testFindAllProcessDefinitions() throws Exception {
    // put 3 process definitions in the database with the same name,
    // but different versions
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    deployProcessDefinition(processDefinition);

    List allProcessDefinitions = graphSession.findAllProcessDefinitions();
    assertEquals(5, allProcessDefinitions.size());
    assertEquals(3, ((ProcessDefinition) allProcessDefinitions.get(0)).getVersion());
    assertEquals("auction", ((ProcessDefinition) allProcessDefinitions.get(0)).getName());
    assertEquals(2, ((ProcessDefinition) allProcessDefinitions.get(1)).getVersion());
    assertEquals("auction", ((ProcessDefinition) allProcessDefinitions.get(1)).getName());
    assertEquals(1, ((ProcessDefinition) allProcessDefinitions.get(2)).getVersion());
    assertEquals("auction", ((ProcessDefinition) allProcessDefinitions.get(2)).getName());
    assertEquals(2, ((ProcessDefinition) allProcessDefinitions.get(3)).getVersion());
    assertEquals("bake cake", ((ProcessDefinition) allProcessDefinitions.get(3)).getName());
    assertEquals(1, ((ProcessDefinition) allProcessDefinitions.get(4)).getVersion());
    assertEquals("bake cake", ((ProcessDefinition) allProcessDefinitions.get(4)).getName());
  }

  public void testFindAllProcessDefinitionVersions() throws Exception {
    // put 3 process definitions in the database with the same name,
    // but different versions
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    deployProcessDefinition(processDefinition);

    processDefinition = new ProcessDefinition("bake cake");
    deployProcessDefinition(processDefinition);

    List allProcessDefinitionVersions = graphSession.findAllProcessDefinitionVersions("auction");
    assertEquals(3, allProcessDefinitionVersions.size());
    assertEquals(3, ((ProcessDefinition) allProcessDefinitionVersions.get(0)).getVersion());
    assertEquals("auction", ((ProcessDefinition) allProcessDefinitionVersions.get(0)).getName());
    assertEquals(2, ((ProcessDefinition) allProcessDefinitionVersions.get(1)).getVersion());
    assertEquals("auction", ((ProcessDefinition) allProcessDefinitionVersions.get(1)).getName());
    assertEquals(1, ((ProcessDefinition) allProcessDefinitionVersions.get(2)).getVersion());
    assertEquals("auction", ((ProcessDefinition) allProcessDefinitionVersions.get(2)).getName());

    allProcessDefinitionVersions = graphSession.findAllProcessDefinitionVersions("bake cake");
    assertEquals(2, allProcessDefinitionVersions.size());
    assertEquals(2, ((ProcessDefinition) allProcessDefinitionVersions.get(0)).getVersion());
    assertEquals("bake cake",
      ((ProcessDefinition) allProcessDefinitionVersions.get(0)).getName());
    assertEquals(1, ((ProcessDefinition) allProcessDefinitionVersions.get(1)).getVersion());
    assertEquals("bake cake",
      ((ProcessDefinition) allProcessDefinitionVersions.get(1)).getName());
  }

  public void testSaveAndLoadProcessInstance() {
    ProcessDefinition processDefinition = new ProcessDefinition("auction");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance = saveAndReload(processInstance);
    assertNotNull(processInstance);
  }

  public void testUpdateProcessInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='auction'>"
      + "  <start-state name='s' />"
      + "  <node name='n' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);

    processInstance = saveAndReload(processInstance);
    long pid = processInstance.getId();

    assertEquals("s", processInstance.getRootToken().getNode().getName());
    processInstance.getRootToken().setNode(processInstance.getProcessDefinition().getNode("n"));

    processInstance = saveAndReload(processInstance);
    assertEquals("n", processInstance.getRootToken().getNode().getName());
    assertEquals(pid, processInstance.getId());
  }

  public void testFindProcessInstancesByProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='auction'>"
      + "  <start-state name='s' />"
      + "  <node name='n' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    jbpmContext.save(new ProcessInstance(processDefinition));
    jbpmContext.save(new ProcessInstance(processDefinition));
    jbpmContext.save(new ProcessInstance(processDefinition));

    newTransaction();
    List processInstances = graphSession.findProcessInstances(processDefinition.getId());
    assertEquals(3, processInstances.size());

    // process instances should be ordered from recent to old
    long previousStart = System.currentTimeMillis();
    for (Iterator iter = processInstances.iterator(); iter.hasNext();) {
      ProcessInstance processInstance = (ProcessInstance) iter.next();
      long processStart = processInstance.getStart().getTime();
      assertTrue(previousStart >= processStart);
      previousStart = processStart;
    }
  }

  public void testDeleteProcessInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='auction'>"
      + "  <start-state name='s' />"
      + "  <node name='n' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    jbpmContext.save(processInstance);

    newTransaction();
    graphSession.deleteProcessInstance(processInstance.getId());

    newTransaction();
    assertEquals(0, graphSession.findProcessInstances(processDefinition.getId()).size());
  }

  public void testDeleteProcessInstanceWithVariables() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='auction'>"
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
    deployProcessDefinition(processDefinition);

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
    List processInstances = graphSession.findProcessInstances(processDefinition.getId());
    assertEquals(0, processInstances.size());
  }

  public void testDeleteProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='deleteme'>"
      + "  <start-state name='s' />"
      + "  <node name='n' />"
      + "</process-definition>");
    jbpmContext.deployProcessDefinition(processDefinition);

    newTransaction();
    try {
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));
      jbpmContext.save(new ProcessInstance(processDefinition));
    }
    finally {
      graphSession.deleteProcessDefinition(processDefinition.getId());
    }

    newTransaction();
    assertEquals(0, graphSession.findAllProcessDefinitionVersions(processDefinition.getName())
      .size());
    assertEquals(0, graphSession.findProcessInstances(processDefinition.getId()).size());
  }

  public void testLatestProcessDefinitions() {
    deployProcessDefinition(new ProcessDefinition("websale"));
    deployProcessDefinition(new ProcessDefinition("websale"));
    deployProcessDefinition(new ProcessDefinition("websale"));

    deployProcessDefinition(new ProcessDefinition("change nappy"));
    deployProcessDefinition(new ProcessDefinition("change nappy"));

    List latestProcessDefinitions = graphSession.findLatestProcessDefinitions();
    assertEquals(2, latestProcessDefinitions.size());
    assertEquals(3, getVersionOfProcess("websale", latestProcessDefinitions));
    assertEquals(2, getVersionOfProcess("change nappy", latestProcessDefinitions));
  }

  private int getVersionOfProcess(String name, List latestProcessDefinitions) {
    for (Iterator iter = latestProcessDefinitions.iterator(); iter.hasNext();) {
      ProcessDefinition processDefinition = (ProcessDefinition) iter.next();
      if (name.equals(processDefinition.getName())) return processDefinition.getVersion();
    }
    return -1;
  }
}
