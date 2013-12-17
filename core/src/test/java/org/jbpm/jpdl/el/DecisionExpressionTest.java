package org.jbpm.jpdl.el;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class DecisionExpressionTest extends AbstractJbpmTestCase {

  public static class Customer {
    String priority;
    int number;

    public Customer(String priority) {
      this.priority = priority;
    }

    public Customer(int number) {
      this.number = number;
    }

    public String getPriority() {
      return priority;
    }

    public int getNumber() {
      return number;
    }
  }

  public ProcessDefinition createCustomerPriorityProcess() {
    return ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='d'/>"
        + "  </start-state>"
        + "  <decision name='d' expression='#{customer.priority}'>"
        + "    <transition name='LOW' to='l' />"
        + "    <transition name='MEDIUM' to='m' />"
        + "    <transition name='HIGH' to='h' />"
        + "  </decision>"
        + "  <state name='l' />"
        + "  <state name='m' />"
        + "  <state name='h' />"
        + "</process-definition>");
  }

  public void testCustomerPriorityLow() {
    ProcessDefinition processDefinition = createCustomerPriorityProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer("LOW"));
    processInstance.signal();
    assertEquals("l", processInstance.getRootToken().getNode().getName());
  }

  public void testCustomerPriorityMedium() {
    ProcessDefinition processDefinition = createCustomerPriorityProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer("MEDIUM"));
    processInstance.signal();
    assertEquals("m", processInstance.getRootToken().getNode().getName());
  }

  public void testCustomerPriorityUndefined() {
    ProcessDefinition processDefinition = createCustomerPriorityProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer("UNDEFINED"));
    try {
      processInstance.signal();
      fail("expected exception");
    }
    catch (JbpmException e) {
      assert e.getMessage().indexOf("UNDEFINED") != -1 : e;
    }
  }

  public void testCustomerPriorityNull() {
    ProcessDefinition processDefinition = createCustomerPriorityProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer(null));
    try {
      processInstance.signal();
      fail("expected exception");
    }
    catch (JbpmException e) {
      assert e.getMessage().indexOf("transition") != -1 : e;
    }
  }

  public ProcessDefinition createBooleanExpressionProcess() {
    return ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='d'/>"
        + "  </start-state>"
        + "  <decision name='d' expression='#{customer.number > 5}'>"
        + "    <transition name='true' to='high-numbered-customer' />"
        + "    <transition name='false' to='low-numbered-customer' />"
        + "  </decision>"
        + "  <state name='high-numbered-customer' />"
        + "  <state name='low-numbered-customer' />"
        + "</process-definition>");
  }

  public void testBooleanExpressionTrue() {
    ProcessDefinition processDefinition = createBooleanExpressionProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer(9));
    processInstance.signal();
    assertEquals("high-numbered-customer", processInstance.getRootToken().getNode().getName());
  }

  public void testBooleanExpressionFalse() {
    ProcessDefinition processDefinition = createBooleanExpressionProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer(3));
    processInstance.signal();
    assertEquals("low-numbered-customer", processInstance.getRootToken().getNode().getName());
  }

  public ProcessDefinition createConditionProcess() {
    return ProcessDefinition.parseXmlString("<process-definition>"
        + "  <start-state>"
        + "    <transition to='d'/>"
        + "  </start-state>"
        + "  <decision name='d'>"
        + "    <transition to='high-numbered-customer'>"
        + "      <condition  expression='#{customer.number > 5}' />"
        + "    </transition>"
        + "    <transition to='medium-numbered-customer'>"
        + "      <condition expression='#{customer.number == 5}' />"
        + "    </transition>"
        + "    <transition to='low-numbered-customer' />"
        + "  </decision>"
        + "  <state name='high-numbered-customer' />"
        + "  <state name='medium-numbered-customer' />"
        + "  <state name='low-numbered-customer' />"
        + "</process-definition>");
  }

  public void testConditionHigh() {
    ProcessDefinition processDefinition = createConditionProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer(9));
    processInstance.signal();
    assertEquals("high-numbered-customer", processInstance.getRootToken().getNode().getName());
  }

  public void testConditionMedium() {
    ProcessDefinition processDefinition = createConditionProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer(5));
    processInstance.signal();
    assertEquals("medium-numbered-customer", processInstance.getRootToken().getNode().getName());
  }

  public void testConditionLow() {
    ProcessDefinition processDefinition = createConditionProcess();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer(3));
    processInstance.signal();
    assertEquals("low-numbered-customer", processInstance.getRootToken().getNode().getName());
  }
}
