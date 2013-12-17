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
public class ProcessInstanceCommandTest extends AbstractDbTestCase {

  public void testCancelProcessInstanceCommand() throws Exception {
    String xml = "<process-definition name='TestException'>"
      + "   <start-state name='start'>"
      + "      <transition to='fork' />"
      + "   </start-state>"
      + "   <fork name='fork'>"
      + "      <transition name='path1' to='path1' />"
      + "      <transition name='path2' to='path2' />"
      + "   </fork>"
      + "   <state name='path1'>"
      + "      <transition to='join' />"
      + "   </state>"
      + "   <state name='path2'>"
      + "      <transition to='join' />"
      + "   </state>"
      + "   <join name='join'>"
      + "      <transition to='end' />"
      + "   </join>"
      + "   <end-state name='end' />"
      + "</process-definition>";
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(xml);
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    assert !processInstance.hasEnded() : processInstance;

    Token rootToken = processInstance.getRootToken();
    assertEquals("fork", rootToken.getNode().getName());
    for (Iterator iterator = rootToken.getChildren().values().iterator(); iterator.hasNext();) {
      Token childToken = (Token) iterator.next();
      assert !childToken.hasEnded() : childToken;
    }

    // execute CancelProcessInstanceCommand
    new CancelProcessInstanceCommand(processInstance.getId()).execute(jbpmContext);

    // and verify it is canceled
    assert rootToken.hasEnded() : processInstance;
    assertEquals("fork", rootToken.getNode().getName());
    for (Iterator iterator = rootToken.getChildren().values().iterator(); iterator.hasNext();) {
      Token childToken = (Token) iterator.next();
      assert childToken.hasEnded() : childToken;
    }
  }

  public void testSuspendResumeProcessInstanceCommand() throws Exception {
    String xml = "<?xml version='1.0'?>"
      + "<process-definition name='TestException'>"
      + "   <start-state name='start'>"
      + "      <transition to='fork' />"
      + "   </start-state>"
      + "   <fork name='fork'>"
      + "      <transition name='path1' to='path1' />"
      + "      <transition name='path2' to='path2' />"
      + "   </fork>"
      + "   <state name='path1'>"
      + "      <transition to='join' />"
      + "   </state>"
      + "   <state name='path2'>"
      + "      <transition to='join' />"
      + "   </state>"
      + "   <join name='join'>"
      + "      <transition to='end' />"
      + "   </join>"
      + "   <end-state name='end' />"
      + "</process-definition>";
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(xml);
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    assert !processInstance.isSuspended() : processInstance;

    Token rootToken = processInstance.getRootToken();
    assert !rootToken.isSuspended() : rootToken;
    assertEquals("fork", rootToken.getNode().getName());
    for (Iterator iterator = rootToken.getChildren().values().iterator(); iterator.hasNext();) {
      Token childToken = (Token) iterator.next();
      assert !childToken.isSuspended() : childToken;
    }

    // execute SuspendProcessInstanceCommand
    new SuspendProcessInstanceCommand().processInstanceId(
      processInstance.getId()).execute(jbpmContext);

    // and verify
    assert processInstance.isSuspended() : processInstance;
    assert rootToken.isSuspended() : rootToken;
    assertEquals("fork", rootToken.getNode().getName());
    for (Iterator iterator = rootToken.getChildren().values().iterator(); iterator.hasNext();) {
      Token childToken = (Token) iterator.next();
      assert childToken.isSuspended() : childToken;

      try {
        childToken.signal();
        fail("signal should not be accepted on suspended token");
      }
      catch (JbpmException ex) {
        // token is suspended
        assert ex.getMessage().indexOf("suspended") != 0 : ex.getMessage();
      }
    }

    // execute ResumeProcessInstanceCommand
    new ResumeProcessInstanceCommand().processInstanceId(
      processInstance.getId()).execute(jbpmContext);

    // and verify
    assert !processInstance.isSuspended() : processInstance;
    assert !rootToken.isSuspended() : rootToken;
    for (Iterator iter = rootToken.getChildren().values().iterator(); iter.hasNext();) {
      Token childToken = (Token) iter.next();
      assert !childToken.isSuspended() : childToken;
      childToken.signal();
    }

    assertEquals("end", rootToken.getNode().getName());
    assert processInstance.hasEnded() : processInstance;

    // check db state
    processInstance = saveAndReload(processInstance);
    assertEquals("end", processInstance.getRootToken().getNode().getName());
    assert processInstance.hasEnded() : processInstance;
  }

}
