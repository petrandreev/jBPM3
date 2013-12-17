package org.jbpm.jbpm3509;

import org.jbpm.jpdl.el.ELException;
import org.jbpm.jpdl.el.Expression;
import org.jbpm.jpdl.el.ExpressionEvaluator;
import org.jbpm.jpdl.el.FunctionMapper;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.impl.ExpressionEvaluatorImpl;

public class TestExpressionEvaluator extends ExpressionEvaluator {

  protected static String BAD_VALUE = "bad value";
  private static ExpressionEvaluatorImpl realExpressionEvaluator = new ExpressionEvaluatorImpl();

  // public constructor
  public TestExpressionEvaluator() { }
  
  public Expression parseExpression(String expression, Class expectedType, FunctionMapper fMapper) 
    throws ELException {
    return realExpressionEvaluator.parseExpression(expression, expectedType, fMapper);
  }

  // return a Boolean object when possible
  public Object evaluate(String expression, Class expectedType, VariableResolver vResolver,
    FunctionMapper fMapper) throws ELException {
    Object result = realExpressionEvaluator.evaluate(expression, expectedType, vResolver, fMapper);
    
    // return boolean if possible
    if( result instanceof String ) { 
        String resultStr = (String) result;
        if( resultStr.toLowerCase().matches("test|false") ) { 
          return new Boolean(resultStr.toLowerCase());
        }
        if( resultStr.matches(BAD_VALUE) ) { 
          return this;
        }
    }
    
    // return object (which is a string) otherwise
    return result;
  }

}
