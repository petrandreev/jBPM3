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
package org.jbpm.graph.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

import org.jbpm.JbpmException;
import org.jbpm.graph.action.Script;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.xml.JpdlXmlReader;

/**
 * specifies configurable fork behaviour.
 * <p>
 * the fork can behave in two ways:
 * <ul>
 * <li>without configuration, the fork spawns one new child token over each
 * leaving transition.</li>
 * <li>with a script, the fork evaluates the script to obtain the names of
 * leaving transitions to take. the script must have exactly one variable with
 * 'write' access. the script has to assign a {@link Collection} of transition
 * names ({@link String}) to that variable.</li>
 * </ul>
 * </p>
 * <p>
 * if these behaviors do not cover your needs, consider writing a custom
 * {@link ActionHandler}.
 * </p>
 */
public class Fork extends Node {

  private static final long serialVersionUID = 1L;

  /**
   * a script that calculates the transitionNames at runtime.
   */
  Script script;

  public Fork() {
  }

  public Fork(String name) {
    super(name);
  }

  public NodeType getNodeType() {
    return NodeType.Fork;
  }

  public void read(Element forkElement, JpdlXmlReader jpdlReader) {
    // nothing to read
  }

  public void execute(ExecutionContext executionContext) {
    // phase one: determine leaving transitions
    Collection transitionNames;
    if (script == null) {
      // by default, take all leaving transitions
      List transitions = getLeavingTransitions();
      transitionNames = new ArrayList(transitions.size());
      for (Iterator iter = transitions.iterator(); iter.hasNext();) {
        Transition transition = (Transition) iter.next();
        transitionNames.add(transition.getName());
      }
    }
    else {
      // script evaluation selects transitions to take
      transitionNames = evaluateScript(executionContext);
    }

    // lock the arriving token to prevent application code from signaling tokens
    // parked in a fork
    // the corresponding join node unlocks the token after joining
    // https://jira.jboss.com/jira/browse/JBPM-642
    Token token = executionContext.getToken();
    token.lock(toString());

    // phase two: create child token for each selected transition
    Map childTokens = new HashMap();
    for (Iterator iter = transitionNames.iterator(); iter.hasNext();) {
      String transitionName = (String) iter.next();
      Token childToken = createForkedToken(token, transitionName);
      childTokens.put(transitionName, childToken);
    }

    // phase three: branch child tokens from the fork into the transitions
    for (Iterator iter = childTokens.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      String transitionName = (String) entry.getKey();
      Token childToken = (Token) entry.getValue();
      leave(new ExecutionContext(childToken), transitionName);
    }
  }

  /** evaluates script and retrieves the names of leaving transitions. */
  private Collection evaluateScript(ExecutionContext executionContext) {
    Map outputMap = script.eval(executionContext);
    if (outputMap.size() == 1) {
      // interpret single output value as collection
      Object result = outputMap.values().iterator().next();
      if (result instanceof Collection) return (Collection) result;
    }
    throw new JbpmException("expected " + script
      + " to write one collection variable, output was: " + outputMap);
  }

  protected Token createForkedToken(Token parent, String transitionName) {
    // instantiate the child token
    return new Token(parent, getTokenName(parent, transitionName));
  }

  protected String getTokenName(Token parent, String transitionName) {
    if (transitionName != null) {
      // use transition name, if not taken already
      if (!parent.hasChild(transitionName)) return transitionName;

      // append numeric suffix to transition name
      StringBuffer tokenText = new StringBuffer(transitionName);
      String tokenName;

      int baseLength = transitionName.length();
      int suffix = 2;
      do {
        tokenText.append(suffix++);
        tokenName = tokenText.toString();
        tokenText.setLength(baseLength);
      } while (parent.hasChild(tokenName));

      return tokenName;
    }
    // no transition name
    else {
      Map childTokens = parent.getChildren();
      return childTokens != null ? Integer.toString(childTokens.size() + 1) : "1";
    }
  }

  public Script getScript() {
    return script;
  }

  public void setScript(Script script) {
    this.script = script;
  }
}
