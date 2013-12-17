package org.jbpm;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class DefaultConfigurationTest extends AbstractJbpmTestCase {
  
  public void testDefaultConfiguration() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='one' />" +
      "  </start-state>" +
      "  <state name='one'>" +
      "    <transition to='two' />" +
      "  </state>" +
      "  <state name='two'>" +
      "    <transition to='three' />" +
      "  </state>" +
      "  <state name='three'>" +
      "    <transition to='end' />" +
      "  </state>" +
      "  <end-state name='end' />" +
      "</process-definition>"
    );
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    assertEquals("start", processInstance.getRootToken().getNode().getName());
    processInstance.signal();
    assertEquals("one", processInstance.getRootToken().getNode().getName());
    processInstance.signal();
    assertEquals("two", processInstance.getRootToken().getNode().getName());
    processInstance.signal();
    assertEquals("three", processInstance.getRootToken().getNode().getName());
    processInstance.signal();
    assertTrue(processInstance.hasEnded());
  }
}
