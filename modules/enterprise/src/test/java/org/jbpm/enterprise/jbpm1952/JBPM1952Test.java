package org.jbpm.enterprise.jbpm1952;

import junit.framework.Test;

import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.enterprise.AbstractEnterpriseTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;

/**
 * Use JMS instead of DBMS for storing jobs, so that each job is not taken by multiple job executor
 * threads.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1952">JBPM-1952</a>
 * @author Alejandro Guizar
 */
public class JBPM1952Test extends AbstractEnterpriseTestCase {

  private static final int PROCESS_EXECUTION_COUNT = 20;

  public static Test suite() {
    return new IntegrationTestSetup(JBPM1952Test.class, "enterprise-test.war");
  }

  public void testStaleStateInAsyncFork() {
    deployProcessDefinition("<process-definition name='jbpm1952'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}' />"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <transition to='b' />"
        + "  </node>"
        + "  <node name='b' async='true'>"
        + "    <transition to='fork' />"
        + "  </node>"
        + "  <fork name='fork'>"
        + "    <transition to='c1' name='to_c1'/>"
        + "    <transition to='c2' name='to_c2'/>"
        + "    <transition to='c3' name='to_c3'/>"
        + "  </fork>"
        + "  <node name='c1' async='true'>"
        + "    <transition to='join' />"
        + "  </node>"
        + "  <node name='c2' async='true'>"
        + "    <transition to='join' />"
        + "  </node>"
        + "  <node name='c3' async='true'>"
        + "    <transition to='join' />"
        + "  </node>"
        + "  <join name='join' async='exclusive'>"
        + "    <transition to='d' />"
        + "  </join>"
        + "  <node name='d' async='true'>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end'/>"
        + "</process-definition>");

    long[] processInstanceIds = new long[PROCESS_EXECUTION_COUNT];
    for (int i = 0; i < PROCESS_EXECUTION_COUNT; i++) {
      processInstanceIds[i] = startProcessInstance("jbpm1952").getId();
    }

    EventCallback.waitForEvent(PROCESS_EXECUTION_COUNT, Event.EVENTTYPE_PROCESS_END);

    for (int i = 0; i < PROCESS_EXECUTION_COUNT; i++) {
      long processInstanceId = processInstanceIds[i];
      assertTrue("expected process instance " + processInstanceId + " to have ended",
          hasProcessInstanceEnded(processInstanceId));
    }
  }

}
