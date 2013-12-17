package org.jbpm.jpdl.el.impl;

import java.util.regex.Pattern;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.ExpressionEvaluator;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.FunctionMapper;

public class JbpmExpressionEvaluator {

  private static ExpressionEvaluator expressionEvaluator;

  /**
   * @deprecated set configuration entry <code>jbpm.expression.evaluator</code> instead
   */
  public static void setExpressionEvaluator(ExpressionEvaluator expressionEvaluator) {
    JbpmExpressionEvaluator.expressionEvaluator = expressionEvaluator;
  }

  public static ExpressionEvaluator getExpressionEvaluator() {
    return expressionEvaluator != null ? expressionEvaluator
      : (ExpressionEvaluator) Configs.getObject("jbpm.expression.evaluator");
  }

  private static VariableResolver variableResolver;

  /**
   * @deprecated set configuration entry <code>jbpm.variable.resolver</code> instead
   */
  public static void setVariableResolver(VariableResolver variableResolver) {
    JbpmExpressionEvaluator.variableResolver = variableResolver;
  }

  public static VariableResolver getVariableResolver() {
    return variableResolver != null ? variableResolver
      : (VariableResolver) Configs.getObject("jbpm.variable.resolver");
  }

  /** @deprecated call {@link #getVariableResolver()} instead */
  public static VariableResolver getUsedVariableResolver() {
    return getVariableResolver();
  }

  private static FunctionMapper functionMapper;

  /**
   * @deprecated set configuration entry <code>jbpm.function.mapper</code> instead
   */
  public static void setFunctionMapper(FunctionMapper functionMapper) {
    JbpmExpressionEvaluator.functionMapper = functionMapper;
  }

  public static FunctionMapper getFunctionMapper() {
    return functionMapper != null ? functionMapper
      : Configs.hasObject("jbpm.function.mapper") ? (FunctionMapper) Configs.getObject("jbpm.function.mapper")
        : null;
  }

  /** @deprecated call {@link #getFunctionMapper()} instead */
  public static FunctionMapper getUsedFunctionMapper() {
    return getFunctionMapper();
  }

  public static Object evaluate(String expression, ExecutionContext executionContext) {
    return evaluate(expression, executionContext, Object.class);
  }

  public static Object evaluate(String expression, ExecutionContext executionContext,
    Class expectedType) {
    return evaluate(expression, executionContext, expectedType, getVariableResolver(),
      getFunctionMapper());
  }

  public static Object evaluate(String expression, ExecutionContext executionContext,
    VariableResolver variableResolver, FunctionMapper functionMapper) {
    return evaluate(expression, executionContext, Object.class, variableResolver,
      functionMapper);
  }

  public static Object evaluate(String expression, ExecutionContext executionContext,
    Class expectedType, VariableResolver variableResolver, FunctionMapper functionMapper) {
    ExecutionContext.pushCurrentContext(executionContext);
    try {
      return getExpressionEvaluator().evaluate(translateExpressionToDollars(expression),
        expectedType, variableResolver, functionMapper);
    }
    finally {
      ExecutionContext.popCurrentContext(executionContext);
    }
  }

  private static Pattern hashPattern = Pattern.compile("\\#\\{([^\\}]*)\\}");

  private static String translateExpressionToDollars(String expression) {
    return hashPattern.matcher(expression).replaceAll("\\${$1}");
  }
}
