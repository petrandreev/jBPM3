package org.jbpm.graph.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class SubProcessCancellationTest extends AbstractDbTestCase
{
  public void testWithSubProcess()
  {
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
    jbpmContext.deployProcessDefinition(subProcess);

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
    jbpmContext.deployProcessDefinition(superProcess);

    newTransaction();
    try
    {
      ProcessInstance pi = jbpmContext.newProcessInstanceForUpdate("super");
      pi.signal();

      ProcessInstance subPi = pi.getRootToken().getSubProcessInstance();
      assertEquals("wait", subPi.getRootToken().getNode().getName());

      pi = saveAndReload(pi);
      pi.end();
      pi.getTaskMgmtInstance().endAll();

      pi = saveAndReload(pi);
      assertTrue(pi.hasEnded());
      subPi = pi.getRootToken().getSubProcessInstance();
      assertTrue(subPi.hasEnded());

      for (TaskInstance taskInstance : subPi.getTaskMgmtInstance().getTaskInstances())
      {
        assertFalse(taskInstance.isSignalling());
        assertFalse(taskInstance.hasEnded());
      }
    }
    finally
    {
      graphSession.deleteProcessDefinition(superProcess.getId());
      graphSession.deleteProcessDefinition(subProcess.getId());
    }
  }
}
