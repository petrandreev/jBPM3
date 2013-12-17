/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.graph.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.xml.JpdlXmlReader;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.ParseException;
import bsh.TargetError;

public class Script extends Action {

  private static final long serialVersionUID = 1L;

  protected String expression = null;
  protected Set<VariableAccess> variableAccesses = null;

  public void read(Element scriptElement, JpdlXmlReader jpdlReader) {
    if (scriptElement.isTextOnly()) {
      expression = scriptElement.getText();
    }
    else {
      this.variableAccesses = new HashSet<VariableAccess>(
          jpdlReader.readVariableAccesses(scriptElement));
      expression = scriptElement.element("expression").getText();
    }
  }

  public void execute(ExecutionContext executionContext) throws Exception {
    Map<String, Object> outputMap = eval(executionContext);
    setVariables(outputMap, executionContext);
  }

  public Map<String, Object> eval(Token token) {
    return eval(new ExecutionContext(token));
  }

  public Map<String, Object> eval(ExecutionContext executionContext) {
    Map<String, Object> inputMap = createInputMap(executionContext);
    Set<String> outputNames = getOutputNames();
    return eval(inputMap, outputNames);
  }

  public Map<String, Object> createInputMap(ExecutionContext executionContext) {
    Token token = executionContext.getToken();

    Map<String, Object> inputMap = new HashMap<String, Object>();
    inputMap.put("executionContext", executionContext);
    inputMap.put("token", token);
    inputMap.put("node", executionContext.getNode());
    inputMap.put("task", executionContext.getTask());
    inputMap.put("taskInstance", executionContext.getTaskInstance());

    // if no readable variableInstances are specified,
    ContextInstance contextInstance = executionContext.getContextInstance();
    if (!hasReadableVariable()) {
      // we copy all the variableInstances of the context into the interpreter
      Map<String, Object> variables = contextInstance.getVariables(token);
      if (variables != null) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
          String variableName = entry.getKey();
          Object variableValue = entry.getValue();
          inputMap.put(variableName, variableValue);
        }
      }

    }
    else {
      // we only copy the specified variableInstances into the interpreter
      for (VariableAccess variableAccess : variableAccesses) {
        if (variableAccess.isReadable()) {
          String variableName = variableAccess.getVariableName();
          String mappedName = variableAccess.getMappedName();
          Object variableValue = contextInstance.getVariable(variableName, token);
          inputMap.put(mappedName, variableValue);
        }
      }
    }

    return inputMap;
  }

  public Map<String, Object> eval(Map<String, Object> inputMap, Set<String> outputNames) {
    Map<String, Object> outputMap = new HashMap<String, Object>();

    try {
      log.debug("script input: " + inputMap);
      Interpreter interpreter = new Interpreter();
      for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
        String inputName = entry.getKey();
        Object inputValue = entry.getValue();
        interpreter.set(inputName, inputValue);
      }
      interpreter.eval(expression);
      for (String outputName : outputNames) {
        Object outputValue = interpreter.get(outputName);
        outputMap.put(outputName, outputValue);
      }
      log.debug("script output: " + outputMap);
    }
    catch (ParseException e) {
      throw new DelegationException("parse error occurred", e);
    }
    catch (TargetError e) {
      throw new DelegationException("script threw exception", e.getTarget());
    }
    catch (EvalError e) {
      log.warn("exception during evaluation of script expression", e);
      throw new DelegationException("script evaluation failed", e);
    }

    return outputMap;
  }

  public void addVariableAccess(VariableAccess variableAccess) {
    if (variableAccesses == null) variableAccesses = new HashSet<VariableAccess>();
    variableAccesses.add(variableAccess);
  }

  Set<String> getOutputNames() {
    Set<String> outputNames = new HashSet<String>();
    if (variableAccesses != null) {
      for (VariableAccess variableAccess : variableAccesses) {
        if (variableAccess.isWritable()) {
          outputNames.add(variableAccess.getMappedName());
        }
      }
    }
    return outputNames;
  }

  boolean hasReadableVariable() {
    if (variableAccesses == null) return false;
    for (VariableAccess variableAccess : variableAccesses) {
      if (variableAccess.isReadable()) {
        return true;
      }
    }
    return false;
  }

  void setVariables(Map<String, Object> outputMap, ExecutionContext executionContext) {
    if ((outputMap != null) && (!outputMap.isEmpty()) && (executionContext != null)) {
      Map<String, String> variableNames = getVariableNames();
      ContextInstance contextInstance = executionContext.getContextInstance();
      Token token = executionContext.getToken();

      for (Map.Entry<String, String> entry : variableNames.entrySet()) {
        String mappedName = entry.getKey();
        String variableName = entry.getValue();
        contextInstance.setVariable(variableName, outputMap.get(mappedName), token);
      }
    }
  }

  Map<String, String> getVariableNames() {
    Map<String, String> variableNames = new HashMap<String, String>();
    for (VariableAccess variableAccess : variableAccesses) {
      if (variableAccess.isWritable()) {
        variableNames.put(variableAccess.getMappedName(), variableAccess.getVariableName());
      }
    }
    return variableNames;
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public Set<VariableAccess> getVariableAccesses() {
    return variableAccesses;
  }

  public void setVariableAccesses(Set<VariableAccess> variableAccesses) {
    this.variableAccesses = variableAccesses;
  }

  private static final Log log = LogFactory.getLog(Script.class);
}
