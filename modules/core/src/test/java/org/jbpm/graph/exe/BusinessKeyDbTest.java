package org.jbpm.graph.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;

public class BusinessKeyDbTest extends AbstractDbTestCase
{

  public void testSimpleBusinessKey()
  {
    ProcessDefinition processDefinition = new ProcessDefinition("businesskeytest");
    jbpmContext.deployProcessDefinition(processDefinition);
    try
    {
      newTransaction();

      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("businesskeytest");
      processInstance.setKey("businesskey1");

      newTransaction();

      processInstance = jbpmContext.newProcessInstanceForUpdate("businesskeytest");
      processInstance.setKey("businesskey2");

      newTransaction();

      processDefinition = jbpmContext.getGraphSession().findLatestProcessDefinition("businesskeytest");
      processInstance = jbpmContext.getProcessInstance(processDefinition, "businesskey1");
      assertEquals("businesskey1", processInstance.getKey());
    }
    finally
    {
      newTransaction();
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }

  }

  public void testDuplicateBusinessKeyInDifferentProcesses()
  {
    ProcessDefinition processDefinitionOne = new ProcessDefinition("businesskeytest1");
    processDefinitionOne = saveAndReload(processDefinitionOne);
    long processDefinitionIdOne = processDefinitionOne.getId();
    
    ProcessDefinition processDefinitionTwo = new ProcessDefinition("businesskeytest2");
    processDefinitionTwo = saveAndReload(processDefinitionTwo);
    long processDefinitionIdTwo = processDefinitionTwo.getId();
    
    try
    {
      jbpmContext.newProcessInstanceForUpdate("businesskeytest1").setKey("duplicatekey");
      newTransaction();
      jbpmContext.newProcessInstanceForUpdate("businesskeytest2").setKey("duplicatekey");
    }
    finally
    {
      newTransaction();
      graphSession.deleteProcessDefinition(processDefinitionIdOne);
      graphSession.deleteProcessDefinition(processDefinitionIdTwo);
    }
  }
}
