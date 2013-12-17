package org.jbpm.jpdl.el;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class ExpressionTest extends AbstractJbpmTestCase {
  
  public static class MyBean {
    public String getAccountNumber() {
      return "abc12345";
    }
    public Map getDetails() {
      Map details = new HashMap();
      details.put("revenue", new Long(8234235));
      return details;
    }
    public String myMethod() {
      myMethodInvocationCount++;
      return "result of myMethod";
    }
    public void buzz() {
      throw new RuntimeException("buzz");
    }
  }
  
  ExecutionContext executionContext = null;
  
  protected void setUp() throws Exception
  {
    super.setUp();
    ProcessDefinition processDefinition = new ProcessDefinition();
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    executionContext = new ExecutionContext(processInstance.getRootToken());
  }
  
  public void testProperty() throws Exception {
    executionContext.setVariable("mb", new MyBean());

    String expression = "#{mb.accountNumber}";
    Object result = JbpmExpressionEvaluator.evaluate(expression, executionContext);
    assertEquals("abc12345", result);
  }

  public void testMapExpression() throws Exception {
    executionContext.setVariable("mb", new MyBean());

    String expression = "#{mb.details['revenue']}";
    Object result = JbpmExpressionEvaluator.evaluate(expression, executionContext);
    assertEquals(new Long(8234235), result);
  }

  static int myMethodInvocationCount = 0;

  public void testMethod() throws Exception {
    executionContext.setVariable("mb", new MyBean());

    myMethodInvocationCount = 0;
    String expression = "#{mb.myMethod}";
    Object result = JbpmExpressionEvaluator.evaluate(expression, executionContext);
    assertEquals("result of myMethod", result);
    assertEquals(1, myMethodInvocationCount);
  }
  
  public void testTaskInstanceName() throws Exception {
    TaskInstance taskInstance = new TaskInstance("hallo");
    executionContext.setTaskInstance(taskInstance);

    String expression = "#{taskInstance.name}";
    Object result = JbpmExpressionEvaluator.evaluate(expression, executionContext);
    assertEquals("hallo", result);
  }

  public void testTaskInstanceVariable() throws Exception {
    TaskInstance taskInstance = new TaskInstance("hallo");
    taskInstance.setVariableLocally("expectedNbrOfHours", new Float(5.5));
    executionContext.setTaskInstance(taskInstance);
    
    String expression = "#{taskInstance.variables['expectedNbrOfHours']}";
    Object result = JbpmExpressionEvaluator.evaluate(expression, executionContext);
    assertEquals(new Float(5.5), result);
  }

  public void testPieceOfText() throws Exception {
    TaskInstance taskInstance = new TaskInstance("hallo");
    taskInstance.setVariableLocally("expectedNbrOfHours", new Float(5.5));

    String expression = "a, b, cdefg";
    Object result = JbpmExpressionEvaluator.evaluate(expression, executionContext);
    assertEquals("a, b, cdefg", result);
  }
  
  public void testExpressionException() {
    executionContext.setVariable("mb", new MyBean());
    String expression = "#{mb.buzz}";
    try {
      JbpmExpressionEvaluator.evaluate(expression, executionContext);
      fail("expected exception");
    } catch (JbpmException e) {
      // OK
      Throwable cause = e.getCause();
      assertEquals(RuntimeException.class, cause.getClass());
      assertEquals("buzz", cause.getMessage());
    }
  }
}
