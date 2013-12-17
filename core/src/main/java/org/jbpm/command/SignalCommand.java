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
package org.jbpm.command;

import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * Signals a token. After signalling the token is returned
 * 
 * @author Bernd Ruecker
 */
public class SignalCommand extends AbstractBaseCommand {

  private static final long serialVersionUID = 1L;

  private long tokenId;

  private String transitionName;

  /**
   * if given, it is checked if the state is as expected. If not, a exception is thrown Ignored
   * if null
   */
  private String expectedStateName;

  private Token previousToken;

  private ProcessInstance previousProcessInstance;

  private Map variables;

  public SignalCommand() {
  }

  public SignalCommand(long tokenId, String transitionName) {
    this.tokenId = tokenId;
    this.transitionName = transitionName;
  }

  public Object execute(JbpmContext jbpmContext) {
    if (previousProcessInstance != null) {

      if (variables != null && variables.size() > 0)
        previousProcessInstance.getContextInstance().addVariables(variables);

      if (transitionName == null) {
        previousProcessInstance.signal();
      }
      else {
        previousProcessInstance.signal(transitionName);
      }
      return previousProcessInstance.getRootToken();
    }
    else {
      Token token = getToken(jbpmContext);

      if (expectedStateName != null && !expectedStateName.equals(token.getNode().getName()))
        throw new JbpmException("token is not in expected state '" + expectedStateName
          + "' but in '" + token.getNode().getName() + "'");

      if (variables != null && variables.size() > 0)
        token.getProcessInstance().getContextInstance().addVariables(variables);

      if (transitionName == null) {
        token.signal();
      }
      else {
        token.signal(transitionName);
      }
      return token;
    }
  }

  protected Token getToken(JbpmContext jbpmContext) {
    if (previousToken != null) {
      return previousToken;
    }
    return jbpmContext.loadTokenForUpdate(tokenId);
  }

  public long getTokenId() {
    return tokenId;
  }

  public void setTokenId(long tokenId) {
    this.tokenId = tokenId;
  }

  public String getTransitionName() {
    return transitionName;
  }

  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }

  public Map getVariables() {
    return variables;
  }

  public void setVariables(Map variables) {
    this.variables = variables;
  }

  public String getExpectedStateName() {
    return expectedStateName;
  }

  public void setExpectedStateName(String expectedStateName) {
    this.expectedStateName = expectedStateName;
  }

  public String getAdditionalToStringInformation() {
    return "tokenId=" + tokenId + ";transitionName=" + transitionName
      + ";processDefinitionName=" + expectedStateName
      // TODO: not sure how this is
      + ";variables=" + variables;
  }

  // methods for fluent programming

  public SignalCommand tokenId(long tokenId) {
    setTokenId(tokenId);
    return this;
  }

  public SignalCommand transitionName(String transitionName) {
    setTransitionName(transitionName);
    return this;
  }

  public SignalCommand variables(Map variables) {
    setVariables(variables);
    return this;
  }

  public SignalCommand expectedStateName(String expectedStateName) {
    setExpectedStateName(expectedStateName);
    return this;
  }
}
