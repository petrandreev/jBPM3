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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.graph.action.Script;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.util.CollectionUtil;

public class Join extends Node {

  /**
   * specifies what type of hibernate lock should be acquired.
   */
  String parentLockMode;

  /**
   * specifies if this join is a discriminator. a descriminator reactivates the parent when the
   * first concurrent token enters the join.
   */
  boolean isDiscriminator = false;

  /**
   * a fixed set of concurrent tokens.
   */
  Collection<String> tokenNames = null;

  /**
   * a script that calculates concurrent tokens at runtime.
   */
  Script script = null;

  /**
   * reactivate the parent if the n-th token arrives in the join.
   */
  int nOutOfM = -1;

  private static final long serialVersionUID = 1L;

  public Join() {
  }

  public Join(String name) {
    super(name);
  }

  @Override
  public NodeType getNodeType() {
    return NodeType.Join;
  }

  public void read(Element element, JpdlXmlReader jpdlReader) {
    String lock = element.attributeValue("lock");
    if (lock != null) {
      if (LockMode.parse(lock) != null)
        parentLockMode = lock;
      else if ("pessimistic".equals(lock))
        parentLockMode = LockMode.UPGRADE.toString();
      else
        jpdlReader.addError("invalid parent lock mode '" + lock + "'");
    }
  }

  public void execute(ExecutionContext executionContext) {
    Token token = executionContext.getToken();

    boolean isAbleToReactivateParent = token.isAbleToReactivateParent();

    if (!token.hasEnded()) token.end(false);

    // if this token is not able to reactivate the parent,
    // we don't need to check anything
    if (isAbleToReactivateParent) {
      // the token arrived in the join and can only reactivate the parent once
      token.setAbleToReactivateParent(false);

      Token parentToken = token.getParent();
      if (parentToken != null) {
        JbpmContext jbpmContext = executionContext.getJbpmContext();
        Session session = jbpmContext != null ? jbpmContext.getSession() : null;
        if (session != null) {
          // obtain update lock by default (LockMode.UPGRADE)
          LockMode lockMode = parentLockMode != null ? LockMode.parse(parentLockMode)
              : LockMode.UPGRADE;
          log.debug("acquiring " + lockMode + " lock on " + parentToken);
          // lock updates as appropriate, no need to flush here
          session.lock(parentToken, lockMode);
        }

        boolean reactivateParent = true;

        // if this is a discriminator
        if (isDiscriminator) {
          // reactivate the parent when the first token arrives in the join.
          // this must be the first token arriving, otherwise isAbleToReactivateParent()
          // should have been false above.
          reactivateParent = true;
        }
        // if a fixed set of tokenNames is specified at design time...
        else if (tokenNames != null) {
          // check reactivation on the basis of those tokenNames
          reactivateParent = mustParentBeReactivated(parentToken, tokenNames.iterator());
        }
        // if a script is specified
        else if (script != null) {
          // check if the script returns a collection or a boolean
          Object result = null;
          try {
            result = script.eval(token);
          }
          catch (Exception e) {
            this.raiseException(e, executionContext);
          }
          // if the result is a collection
          if (result instanceof Collection) {
            // it must be a collection of tokenNames
            Collection<String> runtimeTokenNames = CollectionUtil.checkCollection(
                (Collection<?>) result, String.class);
            reactivateParent = mustParentBeReactivated(parentToken, runtimeTokenNames.iterator());
          }
          // if it's a boolean...
          else if (result instanceof Boolean) {
            // the boolean specifies if the parent needs to be reactivated
            reactivateParent = (Boolean) result;
          }
        }
        // if a nOutOfM is specified
        else if (nOutOfM != -1) {
          int n = 0;
          // check how many tokens already arrived in the join
          for (Token concurrentToken : parentToken.getChildren().values()) {
            if (equals(concurrentToken.getNode())) n++;
          }
          if (n < nOutOfM) reactivateParent = false;
        }
        // if no configuration is specified..
        else {
          // the default behaviour is to check all concurrent tokens and reactivate
          // the parent if the last token arrives in the join
          reactivateParent = mustParentBeReactivated(parentToken, parentToken.getChildren()
              .keySet()
              .iterator());
        }

        // if the parent token needs to be reactivated from this join node
        if (reactivateParent) {
          // write to all child tokens that the parent is already reactivated
          for (Token child : parentToken.getChildren().values()) {
            child.setAbleToReactivateParent(false);
          }
          // write to all child tokens that the parent is already reactivated
          ExecutionContext parentContext = new ExecutionContext(parentToken);
          leave(parentContext);
        }
      }
    }
  }

  public boolean mustParentBeReactivated(Token parentToken, Iterator<String> childTokenNames) {
    while (childTokenNames.hasNext()) {
      String concurrentTokenName = childTokenNames.next();
      Token concurrentToken = parentToken.getChild(concurrentTokenName);
      if (concurrentToken.isAbleToReactivateParent()) {
        log.debug("join will not reactivate parent: found concurrent " + concurrentToken);
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

  public Collection<String> getTokenNames() {
    return tokenNames;
  }

  public void setTokenNames(Collection<String> tokenNames) {
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
