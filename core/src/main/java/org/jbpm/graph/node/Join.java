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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.hibernate.LockMode;
import org.hibernate.Session;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.action.Script;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.xml.JpdlXmlReader;

public class Join extends Node {

  private static final long serialVersionUID = 1L;

  /**
   * specifies what type of hibernate lock should be acquired. <code>null</code> means no lock
   * is to be acquired.
   */
  private String parentLockMode;

  /**
   * specifies if this join is a discriminator. a descriminator reactivates the parent when the
   * first child token enters the join.
   */
  private boolean isDiscriminator;

  /**
   * a fixed set of child tokens.
   */
  private Collection tokenNames;

  /**
   * a script that calculates child tokens at runtime.
   */
  private Script script;

  /**
   * reactivate the parent if the n-th token arrives in the join.
   */
  private int nOutOfM = -1;

  public Join() {
  }

  public Join(String name) {
    super(name);
  }

  public NodeType getNodeType() {
    return NodeType.Join;
  }

  public void read(Element element, JpdlXmlReader jpdlReader) {
    String lock = element.attributeValue("lock");
    if (lock != null) {
      LockMode lockMode = LockMode.parse(lock);
      if (lockMode != null)
        parentLockMode = lockMode.toString();
      else if ("pessimistic".equals(lock))
        parentLockMode = LockMode.UPGRADE.toString();
      else
        jpdlReader.addError("invalid parent lock mode '" + lock + "'");
    }
  }

  public void enter(ExecutionContext executionContext) {
    Token token = executionContext.getToken();
    token.end(false);
    token.setAbleToReactivateParent(true);
    super.enter(executionContext);
  }

  public void execute(ExecutionContext executionContext) {
    Token arrivingToken = executionContext.getToken();

    // if this token is not able to reactivate the parent,
    // there is no need to check anything
    if (!arrivingToken.isAbleToReactivateParent()) return;
    arrivingToken.setAbleToReactivateParent(false);

    if (parentLockMode != null) {
      JbpmContext jbpmContext = executionContext.getJbpmContext();
      Session session;
      if (jbpmContext != null && (session = jbpmContext.getSession()) != null) {
        // parse lock mode
        LockMode lockMode = LockMode.parse(parentLockMode);
        // call load() instead of lock() to obtain an unversioned lock
        // https://jira.jboss.org/browse/SOA-1476
        ProcessInstance processInstance = (ProcessInstance) session.load(ProcessInstance.class,
          new Long(arrivingToken.getProcessInstance().getId()), lockMode);
        // load() hits the database as required, no need to flush() here
        // session.flush();
        if (log.isDebugEnabled()) {
          log.debug(this + " acquires " + lockMode + " lock on " + processInstance);
        }
      }
    }

    Token parentToken = arrivingToken.getParent();
    boolean reactivateParent;
    // if this is a discriminator
    if (isDiscriminator) {
      // reactivate the parent when the first token arrives in the join
      reactivateParent = true;
    }
    // if a fixed set of tokenNames is specified at design time...
    else if (tokenNames != null) {
      // check reactivation on the basis of those tokenNames
      reactivateParent = !parentToken.hasActiveChildren()
        && mustParentBeReactivated(parentToken, tokenNames);
    }
    // if a script is specified
    else if (script != null) {
      // script evaluation tells whether parent must be reactivated
      reactivateParent = evaluateScript(executionContext);
    }
    // if a nOutOfM is specified
    else if (nOutOfM != -1) {
      int n = 0;
      // check how many tokens arrived already
      for (Iterator iter = parentToken.getChildren().values().iterator(); iter.hasNext();) {
        Token childToken = (Token) iter.next();
        if (equals(childToken.getNode())) n++;
      }
      reactivateParent = n >= nOutOfM;
    }
    // if no configuration is specified
    else {
      // check all child tokens and reactivate the parent
      // when the last token arrives in the join
      Collection tokenNames = parentToken.getChildren().keySet();
      reactivateParent = !parentToken.hasActiveChildren()
        && mustParentBeReactivated(parentToken, tokenNames);
    }

    // if the parent token is to leave this node
    if (reactivateParent) {
      // make sibling tokens unable to reactivate the parent
      for (Iterator iter = parentToken.getChildren().values().iterator(); iter.hasNext();) {
        Token childToken = (Token) iter.next();
        childToken.setAbleToReactivateParent(false);
      }
      // unlock parent token
      parentToken.unlock(parentToken.getNode().toString());
      // leave the join node
      leave(new ExecutionContext(parentToken));
    }
  }

  private boolean evaluateScript(ExecutionContext executionContext) {
    Map outputMap = script.eval(executionContext);
    if (outputMap.size() == 1) {
      // extract single output value
      Object result = outputMap.values().iterator().next();

      // if result is a collection
      if (result instanceof Collection) {
        Token parentToken = executionContext.getToken().getParent();
        return !parentToken.hasActiveChildren()
          && mustParentBeReactivated(parentToken, (Collection) result);
      }
      // if it is a boolean...
      else if (result instanceof Boolean) {
        // the boolean value tells whether the parent must be reactivated
        return ((Boolean) result).booleanValue();
      }
      // any other object
      else {
        // non-null result means the parent must be reactivated
        return result != null;
      }
    }
    throw new JbpmException("expected " + script + " to write one variable, output was: "
      + outputMap);
  }

  private boolean mustParentBeReactivated(Token parentToken, Collection childTokenNames) {
    return mustParentBeReactivated(parentToken, childTokenNames.iterator());
  }

  public boolean mustParentBeReactivated(Token parentToken, Iterator childTokenNames) {
    while (childTokenNames.hasNext()) {
      String childTokenName = (String) childTokenNames.next();
      Token childToken = parentToken.getChild(childTokenName);
      if (childToken.isAbleToReactivateParent()) {
        if (log.isDebugEnabled()) {
          log.debug(parentToken + " does not leave " + this + " as " + childToken
            + " is still active");
        }
        return false;
      }
    }
    return true;
  }

  public String getParentLockMode() {
    return parentLockMode;
  }

  public void setParentLockMode(String parentLockMode) {
    this.parentLockMode = parentLockMode;
  }

  public Script getScript() {
    return script;
  }

  public void setScript(Script script) {
    this.script = script;
  }

  public Collection getTokenNames() {
    return tokenNames;
  }

  public void setTokenNames(Collection tokenNames) {
    this.tokenNames = tokenNames;
  }

  public boolean isDiscriminator() {
    return isDiscriminator;
  }

  public void setDiscriminator(boolean isDiscriminator) {
    this.isDiscriminator = isDiscriminator;
  }

  public int getNOutOfM() {
    return nOutOfM;
  }

  public void setNOutOfM(int nOutOfM) {
    this.nOutOfM = nOutOfM;
  }

  private static final Log log = LogFactory.getLog(Join.class);
}
