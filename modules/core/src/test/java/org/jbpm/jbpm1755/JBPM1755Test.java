package org.jbpm.jbpm1755;

import org.hibernate.LockMode;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.Join;

/**
 * Allow process author to set the parent token lock mode in the join token.
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1755
 * 
 * @author Alejandro Guizar
 */
public class JBPM1755Test extends AbstractDbTestCase {

  private ProcessDefinition processDefinition;

  private static final int PROCESS_INSTANCE_COUNT = 5;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm1755/parallelprocess.xml");
    startJobExecutor();
  }

  @Override
  protected void tearDown() throws Exception {
    stopJobExecutor();
    graphSession.deleteProcessDefinition(processDefinition.getId());
    EventCallback.clear();
    super.tearDown();
  }

  public void testReadLock() {
    launchProcessInstances(LockMode.READ);
  }

  public void testUpgradeLock() {
    launchProcessInstances(LockMode.UPGRADE);
  }

  public void testForceLock() {
    launchProcessInstances(LockMode.FORCE);
  }

  private void launchProcessInstances(LockMode lockMode) {
    Join join = (Join) processDefinition.getNode("join1");
    join.setParentLockMode(lockMode.toString());
    jbpmContext.deployProcessDefinition(processDefinition);

    long[] processInstanceIds = new long[PROCESS_INSTANCE_COUNT];
    for (int i = 0; i < PROCESS_INSTANCE_COUNT; i++) {
      newTransaction();
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstanceIds[i] = processInstance.getId();
      processInstance.getContextInstance().setVariable("eventCallback", new EventCallback());
      processInstance.signal();
      jbpmContext.save(processInstance);
    }

    commitAndCloseSession();
    try {
      EventCallback.waitForEvent(PROCESS_INSTANCE_COUNT, Event.EVENTTYPE_PROCESS_END);
    }
    finally {
      beginSessionTransaction();
    }

    for (int i = 0; i < PROCESS_INSTANCE_COUNT; i++) {
      long processInstanceId = processInstanceIds[i];
      assertTrue("expected process instance " + processInstanceId + " to have ended",
          jbpmContext.loadProcessInstance(processInstanceId).hasEnded());
    }
  }

}
