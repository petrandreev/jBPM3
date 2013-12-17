package org.jbpm.db;

public class JbpmContextGetDbTest extends AbstractDbTestCase
{

  public void testUnexistingProcessInstance()
  {
    assertNull(jbpmContext.getProcessInstance(234234));
  }

  public void testUnexistingProcessInstanceForUpdate()
  {
    assertNull(jbpmContext.getProcessInstanceForUpdate(234234));
  }

  public void testUnexistingToken()
  {
    assertNull(jbpmContext.getToken(234234));
  }

  public void testUnexistingTokenForUpdate()
  {
    assertNull(jbpmContext.getTokenForUpdate(234234));
  }

  public void testUnexistingTaskInstance()
  {
    assertNull(jbpmContext.getTaskInstance(234234));
  }

  public void testUnexistingTaskInstanceForUpdate()
  {
    assertNull(jbpmContext.getTaskInstanceForUpdate(234234));
  }
}
