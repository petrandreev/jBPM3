package org.jbpm.graph.def;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jbpm.db.AbstractDbTestCase;

public class SuperStateDbTest extends AbstractDbTestCase {

  public void testGetNodesWithSuperState() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='phase zero'/>"
      + "  <super-state name='phase one'>"
      + "    <node name='ignition' />"
      + "    <node name='explosion' />"
      + "    <super-state name='cleanup'>"
      + "      <node name='take brush' />"
      + "      <node name='sweep floor' />"
      + "      <node name='blow dry' />"
      + "    </super-state>"
      + "    <node name='repare' />"
      + "  </super-state>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    Set expectedNodeNames = new HashSet();
    expectedNodeNames.add("phase zero");
    expectedNodeNames.add("phase one");
    assertEquals(expectedNodeNames, getNodeNames(processDefinition.getNodes()));

    SuperState phaseOne = (SuperState) processDefinition.getNode("phase one");
    expectedNodeNames.clear();
    expectedNodeNames.add("ignition");
    expectedNodeNames.add("explosion");
    expectedNodeNames.add("cleanup");
    expectedNodeNames.add("repare");
    assertEquals(expectedNodeNames, getNodeNames(phaseOne.getNodes()));

    SuperState cleanup = (SuperState) phaseOne.getNode("cleanup");
    expectedNodeNames.clear();
    expectedNodeNames.add("take brush");
    expectedNodeNames.add("sweep floor");
    expectedNodeNames.add("blow dry");
    assertEquals(expectedNodeNames, getNodeNames(cleanup.getNodes()));
  }

  private static Set getNodeNames(List nodes) {
    Set nodeNames = new HashSet();
    for (Iterator iter = nodes.iterator(); iter.hasNext();) {
      Node node = (Node) iter.next();
      nodeNames.add(node.getName());
    }
    return nodeNames;
  }
}
