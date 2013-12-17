package org.jbpm.taskmgmt.exe;

import java.util.Collection;
import java.util.Iterator;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class EndTasksDbTest extends AbstractDbTestCase 
{
  
  public static class Buzz implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
      throw new RuntimeException("buzz");
    }
  }

  public void testCancel() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='endtasksprocess'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'>" +
      "      <action class='"+Buzz.class.getName()+"' />" +
      "    </transition>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    
    jbpmContext.deployProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("endtasksprocess");
      processInstance.signal();

      processInstance = saveAndReload(processInstance);

      assertEquals("approval", processInstance.getRootToken().getNode().getName());
      processInstance = saveAndReload(processInstance);
      processInstance.signal("cancel");
      assertEquals("cancelled", processInstance.getRootToken().getNode().getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testApprove() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='endtasksprocess'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'/>" +
      "    <transition name='reject'  to='cancelled'/>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    jbpmContext.deployProcessDefinition(processDefinition);
    try
    {
      newTransaction();

      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("endtasksprocess");
      processInstance.signal();

      processInstance = saveAndReload(processInstance);

      assertEquals("approval", processInstance.getRootToken().getNode().getName());
      TaskInstance taskInstance = (TaskInstance) processInstance
          .getTaskMgmtInstance()
          .getTaskInstances()
          .iterator()
          .next();
      
      taskInstance.end("approve");
      assertEquals("process", processInstance.getRootToken().getNode().getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testReject() 
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='endtasksprocess'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'/>" +
      "    <transition name='reject'  to='cancelled'/>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    jbpmContext.deployProcessDefinition(processDefinition);
    try
    {
      newTransaction();

      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("endtasksprocess");
      processInstance.signal();

      processInstance = saveAndReload(processInstance);

      assertEquals("approval", processInstance.getRootToken().getNode().getName());
      TaskInstance taskInstance = (TaskInstance) processInstance
          .getTaskMgmtInstance()
          .getTaskInstances()
          .iterator()
          .next();
      
      taskInstance.end("reject");
      assertEquals("cancelled", processInstance.getRootToken().getNode().getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTaskInstancesAfterCancellation() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='endtasksprocess'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'/>" +
      "    <transition name='reject'  to='cancelled'/>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    jbpmContext.deployProcessDefinition(processDefinition);
    try
    {
      newTransaction();

      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("endtasksprocess");
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      processInstance = saveAndReload(processInstance);
      processInstance.signal("cancel");

      Collection taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
      Iterator iter = taskInstances.iterator();
      while(iter.hasNext()) {
        TaskInstance taskInstance = (TaskInstance) iter.next();
        assertTrue(taskInstance.getName()+" ended", taskInstance.hasEnded());
        assertFalse(taskInstance.getName()+" not cancelled", taskInstance.isCancelled());
        assertFalse(taskInstance.getName()+" not blocking", taskInstance.isBlocking());
        assertFalse(taskInstance.getName()+" not signalling", taskInstance.isSignalling());
      }
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
