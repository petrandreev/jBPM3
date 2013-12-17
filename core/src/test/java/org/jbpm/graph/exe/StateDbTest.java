package org.jbpm.graph.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;

public final class StateDbTest extends AbstractDbTestCase {

  public void testDbState() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <start-state name='zero'>"
      + "    <transition to='one' />"
      + "  </start-state>"
      + "  <state name='one'>"
      + "    <transition to='two' />"
      + "  </state>"
      + "  <state name='two'>"
      + "    <transition to='three' />"
      + "  </state>"
      + "  <state name='three'>"
      + "    <transition to='end' />"
      + "  </state>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    assertEquals("zero", processInstance.getRootToken().getNode().getName());

    processInstance = saveAndReload(processInstance);
    processInstance.signal();
    assertEquals("one", processInstance.getRootToken().getNode().getName());

    processInstance = saveAndReload(processInstance);
    processInstance.signal();
    assertEquals("two", processInstance.getRootToken().getNode().getName());

    processInstance = saveAndReload(processInstance);
    processInstance.signal();
    assertEquals("three", processInstance.getRootToken().getNode().getName());

    processInstance = saveAndReload(processInstance);
    processInstance.signal();
    assertEquals("end", processInstance.getRootToken().getNode().getName());
  }
}
