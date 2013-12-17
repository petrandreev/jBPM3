package org.jbpm.scenarios;

import java.util.List;

import org.hibernate.criterion.Restrictions;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class TwoSubProcessesInOneTransactionDbTest extends AbstractDbTestCase {

  public void testTwoSubProcessesInOneTransaction() throws Throwable {
    ProcessDefinition subProcess = ProcessDefinition.parseXmlString("<process-definition name='sub'>"
      + "  <start-state>"
      + "    <transition to='end' />"
      + "  </start-state>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(subProcess);

    ProcessDefinition superProcess = ProcessDefinition.parseXmlString("<process-definition name='super'>"
      + "  <start-state>"
      + "    <transition to='subprocess one' />"
      + "  </start-state>"
      + "  <process-state name='subprocess one'>"
      + "    <sub-process name='sub' />"
      + "    <transition to='subprocess two'/>"
      + "  </process-state>"
      + "  <process-state name='subprocess two'>"
      + "    <sub-process name='sub' />"
      + "    <transition to='wait'/>"
      + "  </process-state>"
      + "  <state name='wait' />"
      + "</process-definition>");
    deployProcessDefinition(superProcess);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("super");
    processInstance.signal();
    jbpmContext.save(processInstance);

    processJobs();
    ProcessInstance superInstance = (ProcessInstance) session.createCriteria(ProcessInstance.class)
      .createAlias("processDefinition", "pd")
      .add(Restrictions.eq("pd.name", "super"))
      .uniqueResult();
    assertEquals("wait", superInstance.getRootToken().getNode().getName());

    List subInstances = session.createCriteria(ProcessInstance.class)
      .createAlias("processDefinition", "pd")
      .add(Restrictions.eq("pd.name", "sub"))
      .list();
    assertEquals(2, subInstances.size());

    ProcessInstance subInstance = (ProcessInstance) subInstances.get(0);
    assertTrue(subInstance.hasEnded());
    assertEquals(superInstance.getRootToken(), subInstance.getSuperProcessToken());

    subInstance = (ProcessInstance) subInstances.get(1);
    assertTrue(subInstance.hasEnded());
    assertEquals(superInstance.getRootToken(), subInstance.getSuperProcessToken());
  }
}
