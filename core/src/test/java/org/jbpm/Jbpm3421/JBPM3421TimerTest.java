package org.jbpm.Jbpm3421;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class JBPM3421TimerTest extends AbstractDbTestCase {

  public void testCancelEventExecutionInTaskTimer() {
    // setup
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm3421/processdef.xml");
    deployProcessDefinition(processDefinition);
    startJobExecutor();

    // test
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("jbpm3421");
    processInstance.getContextInstance().setVariable("eventCallback", new EventCallback());
    processInstance.signal();
    assertEquals("firstNode", processInstance.getRootToken().getNode().getName());

    // Wait for timer and verify that it fired..
    newTransaction();
    EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
    long processInstanceId = processInstance.getId();
    assertEquals("firstNode", jbpmContext.loadProcessInstance(processInstanceId)
      .getRootToken()
      .getNode()
      .getName());

    // Task timer does not have a transition (timer fires == do nothing)
    // so we signal again
    jbpmContext.loadProcessInstance(processInstanceId).signal();

    // Wait for process to end
    newTransaction();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue(jbpmContext.loadProcessInstance(processInstanceId).hasEnded());

    // clean up
    stopJobExecutor();
    EventCallback.clear();
  }

}
