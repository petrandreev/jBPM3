package org.jbpm.jpdl.el.impl;


import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.ELException;
import org.jbpm.jpdl.el.ExpressionEvaluator;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.FunctionMapper;

public class JbpmExpressionEvaluator {

  static ExpressionEvaluator evaluator = new ExpressionEvaluatorImpl();
  public static void setExpressionEvaluator(ExpressionEvaluator expressionEvaluator) {
    JbpmExpressionEvaluator.evaluator = expressionEvaluator;
  }
  
  static VariableResolver variableResolver = null;
  public static void setVariableResolver(VariableResolver variableResolver) {
    JbpmExpressionEvaluator.variableResolver = variableResolver;
  }
  
  static FunctionMapper functionMapper = null;
  public static void setFunctionMapper(FunctionMapper functionMapper) {
    JbpmExpressionEvaluator.functionMapper = functionMapper;
  }
  
  public static Object evaluate(String expression, ExecutionContext executionContext) {
    return evaluate(expression, executionContext, getUsedVariableResolver(), getUsedFunctionMapper());
  }

  public static Object evaluate(String expression, ExecutionContext executionContext, VariableResolver usedVariableResolver, FunctionMapper functionMapper) {
    Object result = null;
    
    ExecutionContext.pushCurrentContext(executionContext);
    try {
      String dollarExpression = translateExpressionToDollars(expression);
      result = evaluator.evaluate(dollarExpression, Object.class, usedVariableResolver, functionMapper);

    } catch (ELException e) {
      
      throw new JbpmException("couldn't evaluate expression '"+expression+"'", (e.getRootCause()!=null ? e.getRootCause() : e));
    } finally {
      ExecutionContext.popCurrentContext(executionContext);
    }
    
    return result;
  }

  static String translateExpressionToDollars(String expression) {
    int hashIndex = expression.indexOf("#{");
    if (hashIndex == -1) return expression;
    
    char[] expressionChars = expression.toCharArray();
    do {
      expressionChars[hashIndex] = '$';
      hashIndex = expression.indexOf("#{", hashIndex + 2);
    } while (hashIndex != -1);
    return new String(expressionChars);
  }

  public static VariableResolver getUsedVariableResolver() {
    if (variableResolver!=null) {
      return variableResolver;
    }
    return (VariableResolver) JbpmConfiguration.Configs.getObject("jbpm.variable.resolver");
  }

  public static FunctionMapper getUsedFunctionMapper() {
    if (functionMapper!=null) {
      return functionMapper;
    }
    if (JbpmConfiguration.Configs.hasObject("jbpm.function.mapper")) {
      return (FunctionMapper) JbpmConfiguration.Configs.getObject("jbpm.function.mapper");
    }
    return null;
  }
}
