package org.jbpm.jbpm2778;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * accept-propagated-events does not affect script.
 * 
 * @author Alejandro Guizar
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2778">JBPM-2778</a>
 */
public class JBPM2778Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2778/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  public void testScriptAcceptPropagatedEvents() throws Exception {
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("jbpm2778");
    processInstance.signal();

    Integer calls = (Integer) processInstance.getContextInstance().getVariable("calls");
    assertEquals(1, calls.intValue());

    processInstance.signal();
    assert processInstance.hasEnded() : "expected " + processInstance + " to have ended";
  }
}
