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
public class TokenCommandTest extends AbstractDbTestCase
{
  
  public void testUnlockTokenCommand() throws Exception {
    String xml = //
       "<process-definition name='TestException'>"  //
      +"   <start-state name='start'>"              //
      +"      <transition to='wait' />"             //
      +"   </start-state>"                          //
      +"   <state name='wait'>"                     //
      +"      <transition to='end' />"              //
      +"   </state>"                                //
      +"   <end-state name='end' />"                //
      +"</process-definition>";
    
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(xml);
    jbpmContext.deployProcessDefinition(processDefinition);
    try {
      ProcessInstance processInstance =  jbpmContext.newProcessInstanceForUpdate("TestException");//processDefinition.createProcessInstance();
      processInstance.getRootToken().signal();
      processInstance.getRootToken().lock("TEST-OWNER");
      long tokenId = processInstance.getRootToken().getId();
      
      processInstance = saveAndReload(processInstance);    
      try {
        processInstance.getRootToken().signal();
        fail("TOKEN IS LOCKED exception expected");
      }
      catch (JbpmException ex) {
        // org.jbpm.JbpmException: this token is locked by TEST-OWNER
        assertEquals("this token is locked by TEST-OWNER", ex.getMessage());
      }
  
      // unlocking without owner is a force unlock -> works
      new UnlockTokenCommand().tokenId(tokenId).execute(jbpmContext);
//      Token token = jbpmContext.loadTokenForUpdate(processInstance.getRootToken().getId());
//      token.foreUnlock();
  
      // unlock with same owner
      processInstance = saveAndReload(processInstance);    
      processInstance.getRootToken().lock("TEST-OWNER");
      processInstance = saveAndReload(processInstance);    
      new UnlockTokenCommand().lockOwner("TEST-OWNER").tokenId(tokenId).execute(jbpmContext);
  
      // unlocking with wrong owner fails
      processInstance = saveAndReload(processInstance);    
      processInstance.getRootToken().lock("TEST-OWNER");
      processInstance = saveAndReload(processInstance);    
      try {
        new UnlockTokenCommand().lockOwner("OTHER-OWNER").tokenId(tokenId).execute(jbpmContext);
        fail("'OTHER-OWNER' can't unlock token exception expected");
      }
      catch (JbpmException ex) {
        // org.jbpm.JbpmException: 'OTHER-OWNER' can't unlock token '1' because it was already locked by 'TEST-OWNER'
        assertTrue("Wrong exception, wasn't 'OTHER-OWNER' can't unlock token", ex.getMessage().indexOf("'OTHER-OWNER' can't unlock token")>=0);
      }
    }
    finally {
      newTransaction();
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());      
    }
  }

  /**
   * Test case which sends a Command for a whole process definition to
   * check if queries are working
   */
  public void testSuspendTokenWithQueryCommand() throws Exception {
    String xml = //
       "<process-definition name='TestToken'>"  //
      +"   <start-state name='start'>"              //
      +"      <transition to='wait' />"             //
      +"   </start-state>"                          //
      +"   <state name='wait'>"                     //
      +"      <transition to='end' />"              //
      +"   </state>"                                //
      +"   <end-state name='end' />"                //
      +"</process-definition>";
    
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(xml);
    jbpmContext.deployProcessDefinition(processDefinition);
    try {
      ProcessInstance processInstance1 =  jbpmContext.newProcessInstanceForUpdate("TestToken");
      processInstance1.getRootToken().signal();
      ProcessInstance processInstance2 =  jbpmContext.newProcessInstanceForUpdate("TestToken");
      
      new SuspendTokenCommand().processName("TestToken").execute(jbpmContext);
  
      newTransaction();      
      processInstance1 = graphSession.loadProcessInstance(processInstance1.getId());
      processInstance2 = graphSession.loadProcessInstance(processInstance2.getId());
      
      assertTrue("process instance 1 is suspended", processInstance1.getRootToken().isSuspended());
      assertTrue("process instance 2 is suspended", processInstance2.getRootToken().isSuspended());

      // only resume process instance 1 (which was signaled and is in state 'wait')
      new ResumeTokenCommand().processName("TestToken").stateName("wait").execute(jbpmContext);

      newTransaction();      
      processInstance1 = graphSession.loadProcessInstance(processInstance1.getId());
      processInstance2 = graphSession.loadProcessInstance(processInstance2.getId());
      
      assertFalse("process instance 1 was resumed", processInstance1.getRootToken().isSuspended());
      assertTrue("process instance 2 is suspended", processInstance2.getRootToken().isSuspended());
    }
    finally {
      newTransaction();
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());      
    }
  }
}
