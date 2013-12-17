package org.jbpm.command;

import java.util.Iterator;

import org.jbpm.JbpmException;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * Tests for {@link Command}s working on {@link ProcessInstance} 
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ProcessInstanceCommandTest extends AbstractDbTestCase
{
  private ProcessDefinition processDefinition;
  
  @Override
  protected void tearDown() throws Exception
  {
    if (processDefinition!=null) {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
    processDefinition=null;
    
    super.tearDown();
  }
  
  public void testEmpty() {}

  public void testCancelProcessInstanceCommand() throws Exception {
    String xml = //
      "<?xml version='1.0' encoding='UTF-8'?>"      //
      +"<process-definition name='TestException'>"  //
      +"   <start-state name='start'>"              //
      +"      <transition to='fork' />"             //
      +"   </start-state>"                          //
      +"   <fork name='fork'>"                      //
      +"      <transition name='path1' to='path1' />" //
      +"      <transition name='path2' to='path2' />" //
      +"   </fork>"                                 //
      +"   <state name='path1'>"                    //
      +"      <transition to='join' />"             //
      +"   </state>"                                //
      +"   <state name='path2'>"                    //
      +"      <transition to='join' />"             //
      +"   </state>"                                //
      +"   <join name='join'>"                      //
      +"      <transition to='end' />"              //
      +"   </join>"                                 //
      +"   <end-state name='end' />"                //
      +"</process-definition>";
    
    processDefinition = ProcessDefinition.parseXmlString(xml);      
    jbpmContext.deployProcessDefinition(processDefinition);
    ProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.getRootToken().signal();
    processInstance = saveAndReload(processInstance);
    
    assertFalse(processInstance.getRootToken().hasEnded());
    assertEquals("fork", processInstance.getRootToken().getNode().getName());
    for (Iterator iterator = processInstance.getRootToken().getChildren().values().iterator(); iterator.hasNext();)
    {
      Token childToken = (Token)iterator.next();
      assertFalse(childToken.hasEnded());
    }

    // execute CancelProcessInstanceCommand
    new CancelProcessInstanceCommand(processInstance.getId()).execute(jbpmContext);
    
    // and verify it is canceled
    assertTrue(processInstance.getRootToken().hasEnded());
    assertEquals("fork", processInstance.getRootToken().getNode().getName());
    for (Iterator iterator = processInstance.getRootToken().getChildren().values().iterator(); iterator.hasNext();)
    {
      Token childToken = (Token)iterator.next();
      assertTrue(childToken.hasEnded());
    }
    
    // required to close jbpm context which has an auto save registered for the ProcessInstance
    // If this is missing, the ProcessInstance gets deleted by the tearDown
    // and the auto save will cause an exception!
    super.newTransaction();
  }
    
  public void testSuspendResumeProcessInstanceCommand() throws Exception {
    String xml = //
      "<?xml version='1.0' encoding='UTF-8'?>"      //
      +"<process-definition name='TestException'>"  //
      +"   <start-state name='start'>"              //
      +"      <transition to='fork' />"             //
      +"   </start-state>"                          //
      +"   <fork name='fork'>"                      //
      +"      <transition name='path1' to='path1' />" //
      +"      <transition name='path2' to='path2' />" //
      +"   </fork>"                                 //
      +"   <state name='path1'>"                    //
      +"      <transition to='join' />"             //
      +"   </state>"                                //
      +"   <state name='path2'>"                    //
      +"      <transition to='join' />"             //
      +"   </state>"                                //
      +"   <join name='join'>"                      //
      +"      <transition to='end' />"              //
      +"   </join>"                                 //
      +"   <end-state name='end' />"                //
      +"</process-definition>";
    
    processDefinition = ProcessDefinition.parseXmlString(xml);      
    jbpmContext.deployProcessDefinition(processDefinition);
    ProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.getRootToken().signal();
    processInstance = saveAndReload(processInstance);
    
    assertFalse(processInstance.isSuspended());
    assertFalse(processInstance.getRootToken().isSuspended());
    assertEquals("fork", processInstance.getRootToken().getNode().getName());
    for (Iterator iterator = processInstance.getRootToken().getChildren().values().iterator(); iterator.hasNext();)
    {
      Token childToken = (Token)iterator.next();
      assertFalse(childToken.isSuspended());
    }

    // execute SuspendProcessInstanceCommand
    new SuspendProcessInstanceCommand().processInstanceId(processInstance.getId()).execute(jbpmContext);
    
    // and verify
    assertTrue(processInstance.isSuspended());
    assertTrue(processInstance.getRootToken().isSuspended());
    assertEquals("fork", processInstance.getRootToken().getNode().getName());
    for (Iterator iterator = processInstance.getRootToken().getChildren().values().iterator(); iterator.hasNext();)
    {
      Token childToken = (Token)iterator.next();
      assertTrue(childToken.isSuspended());
      
      try {
        childToken.signal();
        fail("signal should not be accepted on suspended token");
      }
      catch (Exception ex) {
        assertEquals(JbpmException.class, ex.getClass());
        // can't signal token 'path1' (5): it is suspended
        assertTrue("exception should be, that token is suspended", ex.getMessage().indexOf("it is suspended")>0);
      }
    }
    
    // execute ResumeProcessInstanceCommand
    new ResumeProcessInstanceCommand().processInstanceId(processInstance.getId()).execute(jbpmContext);

    // and verify
    assertFalse(processInstance.isSuspended());
    assertFalse(processInstance.getRootToken().isSuspended());
    for (Iterator iterator = processInstance.getRootToken().getChildren().values().iterator(); iterator.hasNext();)
    {
      Token childToken = (Token)iterator.next();
      assertFalse(childToken.isSuspended());
      childToken.signal();
    }
    
    assertEquals("end", processInstance.getRootToken().getNode().getName());
    assertTrue(processInstance.hasEnded());

    // check db state
    processInstance = saveAndReload(processInstance);    

    assertEquals("end", processInstance.getRootToken().getNode().getName());
    assertTrue(processInstance.hasEnded());
  }  
  

}
