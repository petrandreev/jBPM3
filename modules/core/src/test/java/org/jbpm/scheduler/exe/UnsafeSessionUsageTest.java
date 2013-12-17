package org.jbpm.scheduler.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class UnsafeSessionUsageTest extends AbstractDbTestCase {

  public void testTimerRepeat() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <state name='a'>" +
      "    <timer name='reminder' duedate='0 seconds' >" +
      "      <action class='org.jbpm.scheduler.exe.TimerDbTest$NoOp' />" +
      "    </timer>" +
      "    <transition to='b'/>" +
      "    <transition name='back' to='a'/>" +
      "  </state>" +
      "  <state name='b'/>" +
      "</process-definition>"
    );
    processDefinition = saveAndReload(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();
      
      jbpmContext.save(processInstance);

      processJobs(5000);
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
