package org.jbpm.command;

import org.jbpm.JbpmException;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * Tests for {@link Command}s working on {@link Token}
 * 
 * @author bernd.ruecker@camunda.com
 */
public class TokenCommandTest extends AbstractDbTestCase {

  public void testUnlockTokenCommand() throws Exception {
    String xml = "<process-definition name='TestException'>"
      + "   <start-state name='start'>"
      + "      <transition to='wait' />"
      + "   </start-state>"
      + "   <state name='wait'>"
      + "      <transition to='end' />"
      + "   </state>"
      + "   <end-state name='end' />"
      + "</process-definition>";

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(xml);
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("TestException");
    long tokenId = processInstance.getRootToken().getId();
    processInstance.getRootToken().signal();
    processInstance.getRootToken().lock("TEST-OWNER");

    processInstance = saveAndReload(processInstance);
    try {
      processInstance.getRootToken().signal();
      fail("TOKEN IS LOCKED exception expected");
    }
    catch (JbpmException ex) {
      // token is locked
      assert ex.getMessage().indexOf("locked") != -1 : ex.getMessage();
    }

    // unlocking without owner is a force unlock -> works
    new UnlockTokenCommand().tokenId(tokenId).execute(jbpmContext);

    // unlock with same owner
    processInstance = saveAndReload(processInstance);
    processInstance.getRootToken().lock("TEST-OWNER");

    processInstance = saveAndReload(processInstance);
    new UnlockTokenCommand().lockOwner("TEST-OWNER")
    .tokenId(tokenId)
    .execute(jbpmContext);

    // unlocking with wrong owner fails
    processInstance = saveAndReload(processInstance);
    processInstance.getRootToken().lock("TEST-OWNER");

    processInstance = saveAndReload(processInstance);
    try {
      new UnlockTokenCommand().lockOwner("OTHER-OWNER")
      .tokenId(tokenId)
      .execute(jbpmContext);
      fail("CANNOT UNLOCK TOKEN exception expected");
    }
    catch (JbpmException ex) {
      // token is locked
      assert ex.getMessage().indexOf("cannot unlock") != -1 : ex.getMessage();
    }
  }

}
