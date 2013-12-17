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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

  protected String expression;
  protected Set variableAccesses;

  public void read(Element scriptElement, JpdlXmlReader jpdlReader) {
    if (scriptElement.isTextOnly()) {
      expression = scriptElement.getTextTrim();
    }
    else {
      variableAccesses = new HashSet(jpdlReader.readVariableAccesses(scriptElement));
      expression = scriptElement.element("expression").getTextTrim();
    }
  }

  public void execute(ExecutionContext executionContext) throws Exception {
    Map outputMap = eval(executionContext);
    setVariables(outputMap, executionContext);
  }

  public String toString() {
    return "Script(" + expression + ')';
  }

  public Map eval(Token token) {
    return eval(new ExecutionContext(token));
  }

  public Map eval(ExecutionContext executionContext) {
    Map inputMap = createInputMap(executionContext);
    Set outputNames = getOutputNames();
    return eval(inputMap, outputNames);
  }

  public Map createInputMap(ExecutionContext executionContext) {
    Token token = executionContext.getToken();

    Map inputMap = new HashMap();
    inputMap.put("executionContext", executionContext);
    inputMap.put("token", token);
    inputMap.put("node", executionContext.getNode());
    inputMap.put("task", executionContext.getTask());
    inputMap.put("taskInstance", executionContext.getTaskInstance());

    // if no readable variableInstances are specified,
    ContextInstance contextInstance = executionContext.getContextInstance();
    if (!hasReadableVariable()) {
      // put all variables into the input map
      Map variables = contextInstance.getVariables(token);
      if (variables != null) {
        for (Iterator iter = variables.entrySet().iterator(); iter.hasNext();) {
          Map.Entry entry = (Map.Entry) iter.next();
          String variableName = (String) entry.getKey();
          Object variableValue = entry.getValue();
          inputMap.put(variableName, variableValue);
        }
      }
    }
    else {
      // put only the specified variables into the input map
      for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
        VariableAccess variableAccess = (VariableAccess) iter.next();
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

  public Map eval(Map inputMap, Set outputNames) {
    try {
      // set input variables
      boolean debug = log.isDebugEnabled();
      if (debug) log.debug("script input: " + inputMap);

      Interpreter interpreter = new Interpreter();
      for (Iterator iter = inputMap.entrySet().iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry) iter.next();
        interpreter.set((String) entry.getKey(), entry.getValue());
      }

      // evaluate script
      interpreter.eval(expression);

      // get output variables
      if (outputNames.isEmpty()) return Collections.EMPTY_MAP;
      Map outputMap = new HashMap();
      for (Iterator iter = outputNames.iterator(); iter.hasNext();) {
        String outputName = (String) iter.next();
        Object outputValue = interpreter.get(outputName);
        outputMap.put(outputName, outputValue);
      }
      if (debug) log.debug("script output: " + outputMap);
      return outputMap;
    }
    catch (ParseException e) {
      throw new DelegationException("failed to parse script", e);
    }
    catch (TargetError e) {
      throw new DelegationException("script threw exception", e.getTarget());
    }
    catch (EvalError e) {
      throw new DelegationException("script evaluation halted", e);
    }
  }

  public void addVariableAccess(VariableAccess variableAccess) {
    if (variableAccesses == null) variableAccesses = new HashSet();
    variableAccesses.add(variableAccess);
  }

  private Set getOutputNames() {
    if (variableAccesses == null || variableAccesses.isEmpty()) return Collections.EMPTY_SET;

    Set outputNames = new HashSet();
    for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
      VariableAccess variableAccess = (VariableAccess) iter.next();
      if (variableAccess.isWritable()) outputNames.add(variableAccess.getMappedName());
    }
    return outputNames;
  }

  private boolean hasReadableVariable() {
    if (variableAccesses != null) {
      for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
        VariableAccess variableAccess = (VariableAccess) iter.next();
        if (variableAccess.isReadable()) return true;
      }
    }
    return false;
  }

  private void setVariables(Map outputMap, ExecutionContext executionContext) {
    if (variableAccesses == null) return;

    ContextInstance contextInstance = executionContext.getContextInstance();
    Token token = executionContext.getToken();

    for (Iterator iter = variableAccesses.iterator(); iter.hasNext();) {
      VariableAccess variableAccess = (VariableAccess) iter.next();
      if (variableAccess.isWritable()) {
        Object value = outputMap.get(variableAccess.getMappedName());
        contextInstance.setVariable(variableAccess.getVariableName(), value, token);
      }
    }
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public Set getVariableAccesses() {
    return variableAccesses;
  }

  public void setVariableAccesses(Set variableAccesses) {
    this.variableAccesses = variableAccesses;
  }

  private static final Log log = LogFactory.getLog(Script.class);
}
