package org.jbpm.taskmgmt.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class BlockingTaskDbTest extends AbstractDbTestCase {

  public void testBlockingTask() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='blockingprocess'>"
      + "  <start-state>"
      + "    <transition to='approval' />"
      + "  </start-state>"
      + "  <task-node name='approval'>"
      + "    <task name='approve' blocking='true' />"
      + "    <transition name='approve' to='order'/>"
      + "    <transition name='reject'  to='cancelled'/>"
      + "  </task-node>"
      + "  <state name='order' />"
      + "  <state name='cancelled' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("blockingprocess");
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    assertEquals("approval", processInstance.getRootToken().getNode().getName());

    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();
    assertTrue(taskInstance.isBlocking());

    try {
      processInstance.signal();
      fail("expected RuntimeException");
    }
    catch (IllegalStateException e) {
      assertTrue("expected " + e + " to be about blocking tasks", e.getMessage()
        .indexOf("blocking tasks") != -1);
    }
  }
}
