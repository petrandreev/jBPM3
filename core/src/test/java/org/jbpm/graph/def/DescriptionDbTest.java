package org.jbpm.graph.def;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.node.TaskNode;

public class DescriptionDbTest extends AbstractDbTestCase {

  public void testProcessDefinitionDescription() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <description>haleluja</description>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertEquals("haleluja", processDefinition.getDescription());
  }

  public void testNodeDescription() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='a'>"
      + "    <description>haleluja</description>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertEquals("haleluja", processDefinition.getNode("a").getDescription());
  }

  public void testTransitionDescription() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <node name='a'>"
      + "    <transition name='self' to='a'>"
      + "      <description>haleluja</description>"
      + "    </transition>"
      + "  </node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    assertEquals("haleluja", processDefinition.getNode("a")
      .getLeavingTransition("self")
      .getDescription());
  }

  public void testTaskDescription() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>"
      + "  <task-node name='a'>"
      + "    <task name='self'>"
      + "      <description>haleluja</description>"
      + "    </task>"
      + "  </task-node>"
      + "</process-definition>");

    processDefinition = saveAndReload(processDefinition);
    TaskNode taskNode = (TaskNode) processDefinition.getNode("a");
    assertEquals("haleluja", taskNode.getTask("self").getDescription());
  }
}
