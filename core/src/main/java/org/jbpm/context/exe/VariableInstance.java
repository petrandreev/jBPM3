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
package org.jbpm.context.exe;

import java.io.Serializable;
import java.util.Iterator;

import org.jbpm.JbpmException;
import org.jbpm.context.exe.converter.SerializableToByteArrayConverter;
import org.jbpm.context.exe.variableinstance.NullInstance;
import org.jbpm.context.exe.variableinstance.UnpersistableInstance;
import org.jbpm.context.log.VariableCreateLog;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.util.ClassUtil;

/**
 * base for classes that store variable values in the database.
 */
public abstract class VariableInstance implements Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected String name;
  protected Token token;
  protected TokenVariableMap tokenVariableMap;
  protected ProcessInstance processInstance;
  protected Converter converter;
  protected Object valueCache;
  protected boolean isValueCached;

  // constructors /////////////////////////////////////////////////////////////

  public VariableInstance() {
  }

  public static VariableInstance create(Token token, String name, Object value) {
    VariableInstance variableInstance = value != null ? createVariableInstance(value)
      : new NullInstance();

    variableInstance.name = name;
    if (token != null) {
      variableInstance.token = token;
      variableInstance.processInstance = token.getProcessInstance();
      token.addLog(new VariableCreateLog(variableInstance));
    }

    variableInstance.setValue(value);
    return variableInstance;
  }

  public static VariableInstance createVariableInstance(Object value) {
    for (Iterator iter = JbpmType.getJbpmTypes().iterator(); iter.hasNext();) {
      JbpmType jbpmType = (JbpmType) iter.next();
      if (jbpmType.matches(value)) return jbpmType.newVariableInstance();
    }
    return new UnpersistableInstance();
  }

  // abstract methods /////////////////////////////////////////////////////////

  /**
   * is true if this variable-instance supports the given value, false otherwise.
   */
  public abstract boolean isStorable(Object value);

  /**
   * is the value, stored by this variable instance.
   */
  protected abstract Object getObject();

  /**
   * stores the value in this variable instance.
   */
  protected abstract void setObject(Object value);

  // variable management //////////////////////////////////////////////////////

  public boolean supports(Object value) {
    if (converter != null) {
      return converter.supports(value);
    }
    return isStorable(value);
  }

  public void setValue(Object value) {
    valueCache = value;
    isValueCached = true;

    if (converter != null) {
      if (!converter.supports(value)) {
        throw new JbpmException("converter " + converter.getClass().getName()
          + " does not support value: " + value);
      }
      value = converter.convert(value);
    }
    if (value != null && !isStorable(value)) {
      throw new JbpmException(this + " cannot store values of type "
        + value.getClass().getName());
    }
    setObject(value);
  }

  public Object getValue() {
    if (isValueCached) return valueCache;

    Object value = getObject();
    if (value != null && converter != null) {
      if (converter instanceof SerializableToByteArrayConverter && processInstance != null) {
        SerializableToByteArrayConverter s2bConverter = (SerializableToByteArrayConverter) converter;
        value = s2bConverter.revert(value, processInstance.getProcessDefinition());
      }
      else {
        value = converter.revert(value);
      }
      valueCache = value;
      isValueCached = true;
    }
    return value;
  }

  public void removeReferences() {
    tokenVariableMap = null;
    token = null;
    processInstance = null;
  }

  // utility methods /////////////////////////////////////////////////////////

  public String toString() {
    return ClassUtil.getSimpleName(getClass()) + '(' + name + ')';
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public ProcessInstance getProcessInstance() {
    return processInstance;
  }

  public Token getToken() {
    return token;
  }

  public void setTokenVariableMap(TokenVariableMap tokenVariableMap) {
    this.tokenVariableMap = tokenVariableMap;
  }
}
