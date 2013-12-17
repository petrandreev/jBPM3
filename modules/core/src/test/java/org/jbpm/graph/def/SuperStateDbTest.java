package org.jbpm.graph.def;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jbpm.db.AbstractDbTestCase;

public class SuperStateDbTest extends AbstractDbTestCase {

  public void testGetNodesWithSuperState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='processwithsuperstates'>" +
      "  <node name='phase zero'/>" +
      "  <super-state name='phase one'>" +
      "    <node name='ignition' />" +
      "    <node name='explosion' />" +
      "    <super-state name='cleanup'>" +
      "      <node name='take brush' />" +
      "      <node name='sweep floor' />" +
      "      <node name='blow dry' />" +
      "    </super-state>" +
      "    <node name='repare' />" +
      "  </super-state>" +
      "</process-definition>"
    );

    processDefinition = saveAndReload(processDefinition);
    try
    {
      Set expectedNodeNames = new HashSet();
      expectedNodeNames.add("phase zero");
      expectedNodeNames.add("phase one");
      
      Set nodeNames = getNodeNames(processDefinition.getNodes());
      
      assertEquals(expectedNodeNames, nodeNames);
      
      SuperState phaseOne = (SuperState) processDefinition.getNode("phase one");
      
      expectedNodeNames = new HashSet();
      expectedNodeNames.add("ignition");
      expectedNodeNames.add("explosion");
      expectedNodeNames.add("cleanup");
      expectedNodeNames.add("repare");
      
      nodeNames = getNodeNames(phaseOne.getNodes());

      assertEquals(expectedNodeNames, nodeNames);

      SuperState cleanup = (SuperState) phaseOne.getNode("cleanup");

      expectedNodeNames = new HashSet();
      expectedNodeNames.add("take brush");
      expectedNodeNames.add("sweep floor");
      expectedNodeNames.add("blow dry");
      
      nodeNames = getNodeNames(cleanup.getNodes());

      assertEquals(expectedNodeNames, nodeNames);
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
    
  }

  private Set getNodeNames(List nodes) {
    Set nodeNames = new HashSet();
    Iterator iter = nodes.iterator();
    while (iter.hasNext()) {
      Node node = (Node) iter.next();
      nodeNames.add(node.getName());
    }
    return nodeNames;
  }
}
