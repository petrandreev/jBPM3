package org.jbpm.graph.node;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;

public class TaskNotificationDbTest extends AbstractDbTestCase {

  public void testDeleteProcessWithTaskNotification() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='tasknotification'>"
      + "  <task-node name='mail'>"
      + "    <task name='email info' notify='yes'>"
      + "      <assignment actor-id='mputz'></assignment>"
      + "    </task>"
      + "    <transition to='end-state1'></transition>"
      + "  </task-node>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);
  }
}
