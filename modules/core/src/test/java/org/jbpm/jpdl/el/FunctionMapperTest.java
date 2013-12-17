package org.jbpm.jpdl.el;

import java.lang.reflect.Method;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

public class FunctionMapperTest extends AbstractJbpmTestCase {

  public void testDefault() {
    Token token = new Token();
    ExecutionContext executionContext = new ExecutionContext(token);
    try {
      JbpmExpressionEvaluator.evaluate("${sum(2, 3)}", executionContext);
      fail("expected exception");
    } catch (JbpmException e) {
      //OK
    }
  }

  public static class TestFunctionMapper implements FunctionMapper {
    public Method resolveFunction(String prefix, String localName) {
      try {
        return TestFunctionMapper.class.getMethod("sum", new Class[]{int.class, int.class});
      } catch (Exception e) {
        throw new RuntimeException("couldn't get method sum", e);
      }
    }
    public static int sum(int a, int b) {
      return a+b;
    }
  }

  public void testTestMapper() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <bean name='jbpm.function.mapper' class='"+TestFunctionMapper.class.getName()+"' />" +
      "</jbpm-configuration>"
    );
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      Token token = new Token();
      ExecutionContext executionContext = new ExecutionContext(token);
      Object result = JbpmExpressionEvaluator.evaluate("${sum(2, 3)}", executionContext);
      assertEquals(new Integer(5), result);
      
    } finally {
      jbpmContext.close();
    }
  }
}
