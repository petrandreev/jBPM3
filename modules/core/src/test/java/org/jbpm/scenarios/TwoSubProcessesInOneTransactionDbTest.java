package org.jbpm.scenarios;

import java.util.List;

import org.hibernate.Query;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class TwoSubProcessesInOneTransactionDbTest extends AbstractDbTestCase {

  public void testTwoSubProcessesInOneTransaction() throws Throwable {
    ProcessDefinition subProcess = ProcessDefinition.parseXmlString(
      "<process-definition name='sub'>" +
      "  <start-state>" +
      "    <transition to='end' />" +
      "  </start-state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    jbpmContext.deployProcessDefinition(subProcess);
    
    ProcessDefinition superProcess = ProcessDefinition.parseXmlString(
      "<process-definition name='super'>" +
      "  <start-state>" +
      "    <transition to='subprocess one' />" +
      "  </start-state>" +
      "  <process-state name='subprocess one'>" +
      "    <sub-process name='sub' />" +
      "    <transition to='subprocess two'/>" +
      "  </process-state>" +
      "  <process-state name='subprocess two'>" +
      "    <sub-process name='sub' />" +
      "    <transition to='wait'/>" +
      "  </process-state>" +
      "  <state name='wait' />" +
      "</process-definition>"
    );
    jbpmContext.deployProcessDefinition(superProcess);
    try
    {
      newTransaction();
      
      ProcessInstance processInstance = jbpmContext.newProcessInstance("super");
      processInstance.signal();
      jbpmContext.save(processInstance);
      
      newTransaction();
      
      Query query = session.createQuery(
        "select pi " +
        "from org.jbpm.graph.exe.ProcessInstance pi " +
        "where pi.processDefinition.name = 'super'"
      );
      List superInstances = query.list();
      assertEquals(1, superInstances.size());
      ProcessInstance superInstance = (ProcessInstance) superInstances.get(0);
      assertEquals("wait", superInstance.getRootToken().getNode().getName());

      query = session.createQuery(
        "select pi " +
        "from org.jbpm.graph.exe.ProcessInstance pi " +
        "where pi.processDefinition.name = 'sub'"
      );
      List subInstances = query.list();
      assertEquals(2, subInstances.size());
      
      ProcessInstance subInstance = (ProcessInstance) subInstances.get(0);
      assertTrue(subInstance.hasEnded());
      assertEquals(superInstance.getRootToken(), subInstance.getSuperProcessToken());
      
      subInstance = (ProcessInstance) subInstances.get(1);
      assertTrue(subInstance.hasEnded());
      assertEquals(superInstance.getRootToken(), subInstance.getSuperProcessToken());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(superProcess.getId());
      jbpmContext.getGraphSession().deleteProcessDefinition(subProcess.getId());
    }
    
  }
}
