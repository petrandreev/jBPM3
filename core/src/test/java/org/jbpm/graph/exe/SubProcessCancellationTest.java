package org.jbpm.graph.exe;

import java.util.Iterator;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class SubProcessCancellationTest extends AbstractDbTestCase {

  public void testWithSubProcess() {
    ProcessDefinition subProcess = ProcessDefinition.parseXmlString("<process-definition name='sub'>"
      + "  <start-state>"
      + "    <transition to='wait' />"
      + "  </start-state>"
      + "  <task-node name='wait'>"
      + "    <task>"
      + "      <timer duedate='2 seconds' class='MyTimerClass' />"
      + "    </task>"
      + "    <transition to='end' />"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(subProcess);

    ProcessDefinition superProcess = ProcessDefinition.parseXmlString("<process-definition name='super'>"
      + "  <start-state>"
      + "    <transition to='subprocess' />"
      + "  </start-state>"
      + "  <process-state name='subprocess'>"
      + "    <sub-process name='sub' />"
      + "    <transition to='s'/>"
      + "  </process-state>"
      + "  <state name='s' />"
      + "</process-definition>");
    deployProcessDefinition(superProcess);

    ProcessInstance pi = jbpmContext.newProcessInstanceForUpdate("super");
    pi.signal();

    processJobs();
    pi = jbpmContext.loadProcessInstance(pi.getId());
    ProcessInstance subPi = pi.getRootToken().getSubProcessInstance();
    assertEquals("wait", subPi.getRootToken().getNode().getName());

    pi.end();
    pi.getTaskMgmtInstance().endAll();

    pi = saveAndReload(pi);
    assertTrue(pi.hasEnded());
    subPi = pi.getRootToken().getSubProcessInstance();
    assertTrue(subPi.hasEnded());

    for (Iterator i = subPi.getTaskMgmtInstance().getTaskInstances().iterator(); i.hasNext();) {
      TaskInstance taskInstance = (TaskInstance) i.next();
      assertFalse(taskInstance.isSignalling());
      assertFalse(taskInstance.hasEnded());
    }
  }
}
