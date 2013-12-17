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

import java.util.Map;

import org.jbpm.graph.exe.Token;

/**
 * is a jbpm-internal map of variableInstances related to one {@link Token}.
 * Each token has it's own map of variableInstances, thereby creating hierarchy
 * and scoping of process variableInstances.
 */
public class TokenVariableMap extends VariableContainer {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected Token token;
  protected ContextInstance contextInstance;

  public TokenVariableMap() {
  }

  public TokenVariableMap(Token token, ContextInstance contextInstance) {
    this.token = token;
    this.contextInstance = contextInstance;
  }

  public void addVariableInstance(VariableInstance variableInstance) {
    super.addVariableInstance(variableInstance);
    variableInstance.setTokenVariableMap(this);
  }

  public String toString() {
    return "TokenVariableMap" + (token != null ? '(' + token.getName() + ')'
      : '@' + Integer.toHexString(hashCode()));
  }

  // protected ////////////////////////////////////////////////////////////////

  protected VariableContainer getParentVariableContainer() {
    Token parentToken = token.getParent();
    return parentToken != null ? contextInstance.getTokenVariableMap(parentToken) : null;
  }

  // getters and setters //////////////////////////////////////////////////////

  public ContextInstance getContextInstance() {
    return contextInstance;
  }

  public Token getToken() {
    return token;
  }

  public Map getVariableInstances() {
    return variableInstances;
  }
}
