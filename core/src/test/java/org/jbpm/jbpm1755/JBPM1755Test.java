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
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1755">JBPM-1755</a>
 * @author Alejandro Guizar
 */
public class JBPM1755Test extends AbstractDbTestCase {

  private ProcessDefinition processDefinition;

  private static final int INSTANCE_COUNT = 5;

  protected void setUp() throws Exception {
    super.setUp();
    processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm1755/parallelprocess.xml");
  }

  protected void tearDown() throws Exception {
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
    deployProcessDefinition(processDefinition);

    long[] processInstanceIds = new long[INSTANCE_COUNT];
    for (int i = 0; i < INSTANCE_COUNT; i++) {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstanceIds[i] = processInstance.getId();
      processInstance.getContextInstance().setVariable("eventCallback", new EventCallback());
      processInstance.signal();
      jbpmContext.save(processInstance);
    }

    processJobs();

    for (int i = 0; i < INSTANCE_COUNT; i++) {
      long processInstanceId = processInstanceIds[i];
      assertTrue("expected process instance " + processInstanceId + " to have ended",
        jbpmContext.loadProcessInstance(processInstanceId).hasEnded());
    }
  }

  protected void waitForJobs(long timeout) {
    EventCallback.waitForEvent(INSTANCE_COUNT, Event.EVENTTYPE_PROCESS_END);
  }

}
