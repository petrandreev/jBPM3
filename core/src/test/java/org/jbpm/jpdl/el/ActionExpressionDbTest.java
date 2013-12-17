package org.jbpm.jpdl.el;

import java.io.Serializable;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class ActionExpressionDbTest extends AbstractDbTestCase {
  
  public static class MyActionObject implements Serializable
  {
    private static final long serialVersionUID = 1L;

    public void gettingStarted()
    {
      ContextInstance contextInstance = ExecutionContext.currentExecutionContext().getContextInstance();
      contextInstance.setVariable("getting started", "done");
    }

    public void halfWayThere()
    {
      ContextInstance contextInstance = ExecutionContext.currentExecutionContext().getContextInstance();
      contextInstance.setVariable("half way there", "done");
    }

    public void concluding()
    {
      ContextInstance contextInstance = ExecutionContext.currentExecutionContext().getContextInstance();
      contextInstance.setVariable("concluding", "done");
    }
  }

  public void testActionExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <event type='node-leave'>" +
      "      <action expression='#{myActionObject.gettingStarted}' />" +
      "    </event>" +
      "    <transition to='wait a bit'>" +
      "      <action expression='#{myActionObject.halfWayThere}' />" +
      "    </transition>" +
      "  </start-state>" +
      "  <state name='wait a bit'>" +
      "    <transition to='end' />" +
      "  </state>" +
      "  <end-state name='end' />" +
      "  <event type='process-end'>" +
      "    <action expression='#{myActionObject.concluding}' />" +
      "  </event>" +
      "</process-definition>" 
    );
    session.save(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      ContextInstance contextInstance = processInstance.getContextInstance();
      contextInstance.setVariable("myActionObject", new MyActionObject());
      
      processInstance = saveAndReload(processInstance);
      contextInstance = processInstance.getContextInstance();
      
      processInstance.signal();

      assertEquals("done", contextInstance.getVariable("getting started"));
      assertEquals("done", contextInstance.getVariable("half way there"));
      assertNull(contextInstance.getVariable("concluding"));

      processInstance = saveAndReload(processInstance);
      
      processInstance.signal();
      
      processInstance = saveAndReload(processInstance);
      contextInstance = processInstance.getContextInstance();
      
      assertEquals("done", contextInstance.getVariable("getting started"));
      assertEquals("done", contextInstance.getVariable("half way there"));
      assertEquals("done", contextInstance.getVariable("concluding"));
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
