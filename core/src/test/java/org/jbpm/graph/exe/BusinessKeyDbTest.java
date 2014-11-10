package org.jbpm.graph.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;

public class BusinessKeyDbTest extends AbstractDbTestCase {

  public void testSimpleBusinessKey() {
    ProcessDefinition processDefinition = new ProcessDefinition("businesskeytest");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("businesskeytest");
    processInstance.setKey("businesskey1");

    newTransaction();
    processInstance = jbpmContext.newProcessInstanceForUpdate("businesskeytest");
    processInstance.setKey("businesskey2");

    newTransaction();
    processDefinition = jbpmContext.getGraphSession()
      .findLatestProcessDefinition("businesskeytest");
    processInstance = jbpmContext.getProcessInstance(processDefinition, "businesskey1");
    assertEquals("businesskey1", processInstance.getKey());
  }

  public void testDuplicateBusinessKeyInDifferentProcesses() {
    ProcessDefinition processDefinitionOne = new ProcessDefinition("businesskeytest1");
    deployProcessDefinition(processDefinitionOne);

    ProcessDefinition processDefinitionTwo = new ProcessDefinition("businesskeytest2");
    deployProcessDefinition(processDefinitionTwo);

    jbpmContext.newProcessInstanceForUpdate("businesskeytest1").setKey("duplicatekey");
    jbpmContext.newProcessInstanceForUpdate("businesskeytest2").setKey("duplicatekey");
  }
}
