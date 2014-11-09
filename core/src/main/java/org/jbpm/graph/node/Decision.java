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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.jpdl.xml.JpdlXmlReader;

/**
 * decision node.
 */
@SuppressWarnings({
  "rawtypes"
})
public class Decision extends Node {

  private List decisionConditions;
  private Delegation decisionDelegation;
  private String decisionExpression;

  private static final long serialVersionUID = 1L;

  public Decision() {
  }

  public Decision(String name) {
    super(name);
  }

  public NodeType getNodeType() {
    return NodeType.Decision;
  }

  public void read(Element decisionElement, JpdlXmlReader jpdlReader) {
    String expression = decisionElement.attributeValue("expression");
    Element decisionHandlerElement = decisionElement.element("handler");

    if (expression != null) {
      decisionExpression = expression;
    }
    else if (decisionHandlerElement != null) {
      decisionDelegation = new Delegation();
      decisionDelegation.read(decisionHandlerElement, jpdlReader);
    }
  }

  public Transition addLeavingTransition(Transition leavingTransition) {
    // since the decision node evaluates transition conditions,
    // the condition of the leaving transition will always be met.
    // hence the condition enforcement can be disabled safely
    leavingTransition.setConditionEnforced(false);
    return super.addLeavingTransition(leavingTransition);
  }

  public void execute(ExecutionContext executionContext) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      // set context class loader correctly for delegation class
      // https://jira.jboss.org/jira/browse/JBPM-1448
      ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(executionContext
        .getProcessDefinition());
      Thread.currentThread().setContextClassLoader(processClassLoader);

      Transition transition = null;
      if (decisionDelegation != null) {
        // call decision handler
        transition = handleDecision(executionContext);
      }
      else if (decisionExpression != null) {
        transition = evaluateDecisionExpression(executionContext);
      }
      else if (decisionConditions != null && !decisionConditions.isEmpty()) {
        // backwards compatible mode based on separate DecisionConditions
        for (Iterator iter = decisionConditions.iterator(); iter.hasNext();) {
          DecisionCondition decisionCondition = (DecisionCondition) iter.next();
          // evaluate condition
          String expression = decisionCondition.getExpression();
          Boolean result = (Boolean) JbpmExpressionEvaluator
            .evaluate(expression, executionContext, Boolean.class);
          // if condition was true
          if (Boolean.TRUE.equals(result)) {
            // fetch name associated to condition
            String transitionName = decisionCondition.getTransitionName();
            // select transition by name
            transition = getLeavingTransition(transitionName);
            if (transition != null) break;
          }
        }
      }
      else {
        // new mode based on conditions in the transition itself
        for (Iterator iter = leavingTransitions.iterator(); iter.hasNext();) {
          Transition candidate = (Transition) iter.next();
          // evaluate condition if present
          String condition = candidate.getCondition();
          if (condition != null) {
            Boolean result = (Boolean) JbpmExpressionEvaluator
              .evaluate(condition, executionContext, Boolean.class);
            // if condition was true
            if (Boolean.TRUE.equals(result)) {
              // select transition associated to condition
              transition = candidate;
              break;
            }
          }
        }
      }

      // if no transition was selected, just take the default
      if (transition == null) {
        transition = getDefaultLeavingTransition();
        // if there is no default transition, complain
        if (transition == null) throw new JbpmException(this + " has no default transition");
      }

      if (log.isDebugEnabled()) {
        log.debug(executionContext.getToken() + " leaves " + this + " over " + transition);
      }
      executionContext.leaveNode(transition);
    }
    finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  /**
   * Method added to assist in a refactoring.
   * Implementation had been failing when a Boolean was returned from
   * JbpmExpressionEvaluator.evaluate().  (Boolean couldn't be cast to String)
   */
  private Transition evaluateDecisionExpression(ExecutionContext executionContext) {

    String transitionName = null;
    // evaluate expression
    try {
      transitionName = (String) JbpmExpressionEvaluator.evaluate(decisionExpression, executionContext, String.class);
    } catch (ClassCastException cce) {
      log.warn("Unexpected type found when using ExpressionEvaluator attempting a cast");
      try {
        Boolean booleanTransition = (Boolean) JbpmExpressionEvaluator.evaluate(decisionExpression, executionContext, String.class);
        transitionName = booleanTransition.toString();
      } catch(Exception e) {
        throw new JbpmException("Condition returned an object of unknown type when determining transition.");
      }

    }
    
    Transition transition = getLeavingTransition(transitionName);
    if (transition == null) {
      throw new JbpmException("no such transition: " + transitionName);
    }
    return transition;
  }

  public void setDecisionExpression(String decisionExpression) {
    this.decisionExpression = decisionExpression;
  }
   
  private Transition handleDecision(ExecutionContext executionContext) {
    // invoke handler to obtain transition name
    DecisionHandler decisionHandler = (DecisionHandler) decisionDelegation.getInstance();
    String transitionName;
    try {
      transitionName = decisionHandler.decide(executionContext);
    }
    catch (Exception e) {
      raiseException(e, executionContext);
      return null;
    }

    // resolve transition from name
    Transition transition = getLeavingTransition(transitionName);
    if (transition == null) {
      throw new JbpmException("no such transition: " + transitionName);
    }
    return transition;
  }

  public List getDecisionConditions() {
    return decisionConditions;
  }

  public Delegation getDecisionDelegation() {
    return decisionDelegation;
  }

  public void setDecisionDelegation(Delegation decisionDelegation) {
    this.decisionDelegation = decisionDelegation;
  }

  private static Log log = LogFactory.getLog(Decision.class);
}
