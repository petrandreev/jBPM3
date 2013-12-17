package org.jbpm.jpdl.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class DecisionExpressionTest extends AbstractJbpmTestCase {
  
  public void testBudgetHignerThenThousand() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='d' />" +
      "  </start-state>" +
      "  <decision name='d' " +
      "            expression='#{ ( budget>1000 ? \"important lead\" : ( budget>100 ? \"lead\" : \"beggars\")) }'>" +
      "    <transition name='important lead' to='harras them'/>" +
      "    <transition name='lead' to='put it in the lead db'/>" +
      "    <transition name='beggars' to='forget about it'/>" +
      "  </decision>" +
      "  <state name='harras them' />" +
      "  <state name='put it in the lead db' />" +
      "  <state name='forget about it' />" +
      "</process-definition>"
    );
          
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(3500));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("harras them"), processInstance.getRootToken().getNode());
  }

  public void testBudgetBetweenHundredAndThousand() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='d' />" +
      "  </start-state>" +
      "  <decision name='d' " +
      "            expression='#{ ( budget>1000 ? \"important lead\" : ( budget>100 ? \"lead\" : \"beggars\")) }'>" +
      "    <transition name='important lead' to='harras them'/>" +
      "    <transition name='lead' to='put it in the lead db'/>" +
      "    <transition name='beggars' to='forget about it'/>" +
      "  </decision>" +
      "  <state name='harras them' />" +
      "  <state name='put it in the lead db' />" +
      "  <state name='forget about it' />" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(350));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("put it in the lead db"), processInstance.getRootToken().getNode());
  }

  public void testSmallBudget() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='d' />" +
      "  </start-state>" +
      "  <decision name='d' " +
      "            expression='#{ ( budget>1000 ? \"important lead\" : ( budget>100 ? \"lead\" : \"beggars\")) }'>" +
      "    <transition name='important lead' to='harras them'/>" +
      "    <transition name='lead' to='put it in the lead db'/>" +
      "    <transition name='beggars' to='forget about it'/>" +
      "  </decision>" +
      "  <state name='harras them' />" +
      "  <state name='put it in the lead db' />" +
      "  <state name='forget about it' />" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(35));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("forget about it"), processInstance.getRootToken().getNode());
  }
  
  public void testBooleanToStringConversionForTrue() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='d' />" +
      "  </start-state>" +
      "  <decision name='d' " +
      "            expression='#{budget>1000}'>" +
      "    <transition name='true' to='harras them'/>" +
      "    <transition name='false' to='forget about it'/>" +
      "  </decision>" +
      "  <state name='harras them' />" +
      "  <state name='forget about it' />" +
      "</process-definition>"
    );

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(3500));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("harras them"), processInstance.getRootToken().getNode());
  }
  
  public void testBooleanToStringConversionForFalse() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='d' />" +
      "  </start-state>" +
      "  <decision name='d' " +
      "            expression='#{budget>1000}'>" +
      "    <transition name='true' to='harras them'/>" +
      "    <transition name='false' to='forget about it'/>" +
      "  </decision>" +
      "  <state name='harras them' />" +
      "  <state name='forget about it' />" +
      "</process-definition>"
    );
  
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("budget", new Integer(35));
    processInstance.signal();
    
    assertEquals(processDefinition.getNode("forget about it"), processInstance.getRootToken().getNode());
  }
}
