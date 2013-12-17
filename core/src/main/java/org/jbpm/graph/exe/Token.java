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
package org.jbpm.graph.exe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Identifiable;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.log.SignalLog;
import org.jbpm.graph.log.TokenCreateLog;
import org.jbpm.graph.log.TokenEndLog;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.logging.exe.LoggingInstance;
import org.jbpm.logging.log.CompositeLog;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;
import org.jbpm.util.Clock;

/**
 * represents one path of execution and maintains a pointer to a node in the
 * {@link org.jbpm.graph.def.ProcessDefinition}. Most common way to get a hold of the token
 * objects is with {@link ProcessInstance#getRootToken()} or
 * {@link org.jbpm.graph.exe.ProcessInstance#findToken(String)}.
 */
public class Token implements Identifiable, Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected String name;
  protected Date start;
  protected Date end;
  protected Node node;
  protected Date nodeEnter;
  protected ProcessInstance processInstance;
  protected Token parent;
  protected Map children;
  protected List comments;
  protected ProcessInstance subProcessInstance;
  protected int nextLogIndex;
  private boolean isAbleToReactivateParent = true;
  private boolean isTerminationImplicit;
  private boolean isSuspended;
  private String lock;

  // constructors
  // /////////////////////////////////////////////////////

  public Token() {
  }

  /**
   * creates a root token.
   */
  public Token(ProcessInstance processInstance) {
    this.start = Clock.getCurrentTime();
    this.processInstance = processInstance;
    this.node = processInstance.getProcessDefinition().getStartState();
    this.isTerminationImplicit = processInstance.getProcessDefinition().isTerminationImplicit();

    // assign an id to this token before events get fired
    // skip, process instance is saved shortly after constructing root token
    // Services.assignId(this);
  }

  /**
   * creates a child token.
   */
  public Token(Token parent, String name) {
    this.start = Clock.getCurrentTime();
    this.processInstance = parent.getProcessInstance();
    this.name = name;
    this.node = parent.getNode();
    this.parent = parent;
    parent.addChild(this);
    this.isTerminationImplicit = parent.isTerminationImplicit();
    parent.addLog(new TokenCreateLog(this));

    // assign an id to this token before events get fired
    Services.assignId(this);
  }

  // operations
  // ///////////////////////////////////////////////////////////////////////////

  private void addChild(Token token) {
    if (children == null) children = new HashMap();
    children.put(token.getName(), token);
  }

  /**
   * sends a signal to this token. leaves the current {@link #getNode() node} over the default
   * transition.
   */
  public void signal() {
    if (node == null) {
      throw new JbpmException(this + " is not positioned in a node");
    }

    Transition defaultTransition = node.getDefaultLeavingTransition();
    if (defaultTransition == null) {
      throw new JbpmException(node + " has no default transition");
    }

    signal(defaultTransition, new ExecutionContext(this));
  }

  /**
   * sends a signal to this token. leaves the current {@link #getNode() node} over the
   * transition with the given name.
   */
  public void signal(String transitionName) {
    if (node == null) {
      throw new JbpmException(this + " is not positioned in a node");
    }

    Transition leavingTransition = node.getLeavingTransition(transitionName);
    if (leavingTransition == null) {
      // fall back to target node name
      for (Iterator iter = node.getLeavingTransitions().iterator(); iter.hasNext();) {
        Transition transition = (Transition) iter.next();
        if (transitionName.equals(transition.getTo().getName())) {
          leavingTransition = transition;
          break;
        }
      }
      if (leavingTransition == null) {
        throw new JbpmException(node + " has no leaving transition named " + transitionName);
      }
    }

    signal(leavingTransition, new ExecutionContext(this));
  }

  /**
   * sends a signal to this token. leaves the current {@link #getNode() node} over the given
   * transition.
   */
  public void signal(Transition transition) {
    signal(transition, new ExecutionContext(this));
  }

  void signal(ExecutionContext executionContext) {
    signal(node.getDefaultLeavingTransition(), executionContext);
  }

  private void signal(Transition transition, ExecutionContext executionContext) {
    if (transition == null) {
      throw new JbpmException("transition is null");
    }
    if (executionContext == null) {
      throw new JbpmException("execution context is null");
    }
    if (isSuspended) {
      throw new JbpmException("token is suspended");
    }
    if (isLocked()) {
      throw new JbpmException("token is locked by " + lock);
    }
    if (hasEnded()) {
      throw new JbpmException("token has ended");
    }

    startCompositeLog(new SignalLog(transition));
    try {
      // fire the event before-signal
      Node signalNode = node;
      signalNode.fireEvent(Event.EVENTTYPE_BEFORE_SIGNAL, executionContext);

      // start calculating the next state
      node.leave(executionContext, transition);

      // if required, check if this token is implicitly terminated
      checkImplicitTermination();

      // fire the event after-signal
      signalNode.fireEvent(Event.EVENTTYPE_AFTER_SIGNAL, executionContext);
    }
    finally {
      endCompositeLog();
    }
  }

  /**
   * a set of all the leaving transitions on the current node for which the condition expression
   * resolves to true.
   */
  public Set getAvailableTransitions() {
    if (node == null) return Collections.EMPTY_SET;

    Set availableTransitions = new HashSet();
    addAvailableTransitionsOfNode(node, availableTransitions);
    return availableTransitions;
  }

  /**
   * adds available transitions of that node to the Set and after that calls itself recursively
   * for the SuperSate of the Node if it has a super state
   */
  private void addAvailableTransitionsOfNode(Node currentNode, Set availableTransitions) {
    List leavingTransitions = currentNode.getLeavingTransitions();
    if (leavingTransitions != null) {
      for (Iterator iter = leavingTransitions.iterator(); iter.hasNext();) {
        Transition transition = (Transition) iter.next();
        String conditionExpression = transition.getCondition();
        if (conditionExpression != null) {
          Boolean result = (Boolean) JbpmExpressionEvaluator.evaluate(conditionExpression,
            new ExecutionContext(this), Boolean.class);
          if (Boolean.TRUE.equals(result)) {
            availableTransitions.add(transition);
          }
        }
        else {
          availableTransitions.add(transition);
        }
      }
    }

    if (currentNode.getSuperState() != null) {
      addAvailableTransitionsOfNode(currentNode.getSuperState(), availableTransitions);
    }
  }

  /**
   * ends this token and all of its children (if any). this is the last active (i.e. not-ended)
   * child of a parent token, the parent token will be ended as well and that verification will
   * continue to propagate.
   */
  public void end() {
    end(true);
  }

  /**
   * ends this token with optional parent ending verification.
   * 
   * @param verifyParentTermination specifies if the parent token should be checked for
   * termination. if verifyParentTermination is set to true and this is the last non-ended child
   * of a parent token, the parent token will be ended as well and the verification will
   * continue to propagate.
   */
  public void end(boolean verifyParentTermination) {
    // if already ended, do nothing
    if (end != null) {
      if (parent != null) log.warn(this + " has ended already");
      return;
    }

    // record the end date
    // the end date also indicates that this token has ended
    end = Clock.getCurrentTime();

    // ended tokens cannot reactivate parents
    isAbleToReactivateParent = false;

    // end all this token's children
    if (children != null) {
      for (Iterator iter = children.values().iterator(); iter.hasNext();) {
        Token child = (Token) iter.next();
        if (!child.hasEnded()) {
          child.end();
        }
      }
    }

    // end the subprocess instance, if any
    if (subProcessInstance != null) {
      subProcessInstance.end();
    }

    // only log child-token ends
    // process instance logs replace root token logs
    if (parent != null) {
      parent.addLog(new TokenEndLog(this));
    }

    // if there are tasks associated to this token,
    // remove signaling capabilities
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    if (taskMgmtInstance != null) taskMgmtInstance.removeSignalling(this);

    if (verifyParentTermination) {
      // if this is the last active token of the parent,
      // the parent needs to be ended as well
      notifyParentOfTokenEnd();
    }
  }

  // comments /////////////////////////////////////////////////////////////////

  public void addComment(String message) {
    addComment(new Comment(message));
  }

  public void addComment(Comment comment) {
    if (comments == null) comments = new ArrayList();
    comments.add(comment);
    comment.setToken(this);
  }

  public List getComments() {
    return comments;
  }

  // operations helper methods ////////////////////////////////////////////////

  /**
   * notifies a parent that one of its nodeMap has ended.
   */
  private void notifyParentOfTokenEnd() {
    if (isRoot()) {
      processInstance.end();
    }
    else if (parent != null && !parent.hasActiveChildren()) {
      parent.end();
    }
  }

  /**
   * tells if this token has child tokens that have not yet ended.
   */
  public boolean hasActiveChildren() {
    // try and find at least one child token that is still active (not ended)
    if (children != null) {
      for (Iterator iter = children.values().iterator(); iter.hasNext();) {
        Token child = (Token) iter.next();
        if (!child.hasEnded()) return true;
      }
    }
    return false;
  }

  // log convenience methods //////////////////////////////////////////////////

  /**
   * convenience method for adding a process log.
   */
  public void addLog(ProcessLog processLog) {
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    if (loggingInstance != null) {
      processLog.setToken(this);
      loggingInstance.addLog(processLog);
    }
  }

  /**
   * convenience method for starting a composite log. When you add composite logs, make sure you
   * put the {@link #endCompositeLog()} in a finally block.
   */
  public void startCompositeLog(CompositeLog compositeLog) {
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    if (loggingInstance != null) {
      compositeLog.setToken(this);
      loggingInstance.startCompositeLog(compositeLog);
    }
  }

  /**
   * convenience method for ending a composite log. Make sure you put this in a finally block.
   */
  public void endCompositeLog() {
    LoggingInstance loggingInstance = processInstance.getLoggingInstance();
    if (loggingInstance != null) loggingInstance.endCompositeLog();
  }

  // various information extraction methods ///////////////////////////////////

  public boolean hasEnded() {
    return end != null;
  }

  public boolean isRoot() {
    return processInstance != null && equals(processInstance.getRootToken());
  }

  public boolean hasParent() {
    return parent != null;
  }

  public boolean hasChild(String name) {
    return children != null ? children.containsKey(name) : false;
  }

  public Token getChild(String name) {
    return children != null ? (Token) children.get(name) : null;
  }

  public String getFullName() {
    if (isRoot()) return "/";

    StringBuffer nameBuilder = new StringBuffer();
    for (Token token = this; token.hasParent(); token = token.getParent()) {
      String tokenName = token.getName();
      if (tokenName != null) nameBuilder.insert(0, tokenName);
      nameBuilder.insert(0, '/');
    }
    return nameBuilder.toString();
  }

  public List getChildrenAtNode(Node aNode) {
    List foundChildren = new ArrayList();
    getChildrenAtNode(aNode, foundChildren);
    return foundChildren;
  }

  private void getChildrenAtNode(Node aNode, List foundTokens) {
    if (aNode.equals(node)) {
      foundTokens.add(this);
    }
    else if (children != null) {
      for (Iterator it = children.values().iterator(); it.hasNext();) {
        Token child = (Token) it.next();
        child.getChildrenAtNode(aNode, foundTokens);
      }
    }
  }

  public void collectChildrenRecursively(List tokens) {
    if (children != null) {
      for (Iterator iter = children.values().iterator(); iter.hasNext();) {
        Token child = (Token) iter.next();
        tokens.add(child);
        child.collectChildrenRecursively(tokens);
      }
    }
  }

  public Token findToken(String relativeTokenPath) {
    if (relativeTokenPath == null) return null;

    String path = relativeTokenPath.trim();
    if (path.length() == 0 || ".".equals(path)) return this;
    if ("..".equals(path)) return parent;

    if (path.startsWith("/")) {
      return processInstance.getRootToken().findToken(path.substring(1));
    }
    if (path.startsWith("./")) return findToken(path.substring(2));
    if (path.startsWith("../")) {
      return parent != null ? parent.findToken(path.substring(3)) : null;
    }

    if (children == null) return null;

    int slashIndex = path.indexOf('/');
    if (slashIndex == -1) return (Token) children.get(path);

    Token token = (Token) children.get(path.substring(0, slashIndex));
    return token != null ? token.findToken(path.substring(slashIndex + 1)) : null;
  }

  public Map getActiveChildren() {
    Map activeChildren = new HashMap();
    if (children != null) {
      for (Iterator iter = children.entrySet().iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry) iter.next();
        Token child = (Token) entry.getValue();
        if (!child.hasEnded()) {
          String childName = (String) entry.getKey();
          activeChildren.put(childName, child);
        }
      }
    }
    return activeChildren;
  }

  public void checkImplicitTermination() {
    if (isTerminationImplicit && node.hasNoLeavingTransitions()) {
      end();
      if (processInstance.isTerminatedImplicitly()) processInstance.end();
    }
  }

  public boolean isTerminatedImplicitly() {
    if (end != null) return true;

    Map leavingTransitions = node.getLeavingTransitionsMap();
    if (leavingTransitions != null && !leavingTransitions.isEmpty()) {
      // ok: found a non-terminated token
      return false;
    }

    // loop over all active child tokens
    for (Iterator iter = getActiveChildren().values().iterator(); iter.hasNext();) {
      Token child = (Token) iter.next();
      if (!child.isTerminatedImplicitly()) return false;
    }
    // if none of the above, this token is terminated implicitly
    return true;
  }

  public int nextLogIndex() {
    return nextLogIndex++;
  }

  /**
   * suspends a process execution.
   */
  public void suspend() {
    isSuspended = true;

    suspendJobs();
    suspendTaskInstances();

    // propagate to child tokens
    if (children != null) {
      for (Iterator iter = children.values().iterator(); iter.hasNext();) {
        Token child = (Token) iter.next();
        child.suspend();
      }
    }
  }

  private void suspendJobs() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext != null) {
      JobSession jobSession = jbpmContext.getJobSession();
      if (jobSession != null) jobSession.suspendJobs(this);
    }
  }

  private void suspendTaskInstances() {
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    if (taskMgmtInstance != null) taskMgmtInstance.suspend(this);
  }

  /**
   * resumes a process execution.
   */
  public void resume() {
    isSuspended = false;

    resumeJobs();
    resumeTaskInstances();

    // propagate to child tokens
    if (children != null) {
      for (Iterator iter = children.values().iterator(); iter.hasNext();) {
        Token child = (Token) iter.next();
        child.resume();
      }
    }
  }

  private void resumeJobs() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext != null) {
      JobSession jobSession = jbpmContext.getJobSession();
      if (jobSession != null) jobSession.resumeJobs(this);
    }
  }

  private void resumeTaskInstances() {
    TaskMgmtInstance taskMgmtInstance = processInstance.getTaskMgmtInstance();
    if (taskMgmtInstance != null) taskMgmtInstance.resume(this);
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Token)) return false;

    Token other = (Token) o;
    if (id != 0 && id == other.getId()) return true;

    return (name != null ? name.equals(other.getName()) : other.getName() == null)
      && (parent != null ? parent.equals(other.getParent())
        : processInstance.equals(other.getProcessInstance()));
  }

  public int hashCode() {
    int result = 2080763213 + (name != null ? name.hashCode() : 0);
    result *= 1076685199; 
    // JBPM-3464: processInstance can be null when computing hash code
    if(parent != null) { 
      result += parent.hashCode(); 
    }
    else if( processInstance != null ) { 
      result += processInstance.hashCode();
    }
    return result;
  }

  public String toString() {
    return "Token(" + (id != 0 ? String.valueOf(id) : getFullName()) + ')';
  }

  public ProcessInstance createSubProcessInstance(ProcessDefinition subProcessDefinition) {
    // create the new sub process instance
    subProcessInstance = new ProcessInstance(subProcessDefinition);
    // bind the subprocess to the super-process-token
    setSubProcessInstance(subProcessInstance);
    subProcessInstance.setSuperProcessToken(this);
    // make sure the process gets saved during super process save
    processInstance.addCascadeProcessInstance(subProcessInstance);
    return subProcessInstance;
  }

  /**
   * locks a process instance for further execution. A locked token cannot continue execution.
   * This is a non-persistent operation. This is used to prevent tokens being propagated during
   * the execution of actions.
   * 
   * @see #unlock(String)
   */
  public void lock(String lockOwner) {
    if (lockOwner == null) throw new JbpmException("lock owner is null");

    if (lock == null) {
      lock = lockOwner;
      if (log.isDebugEnabled()) log.debug('\'' + lockOwner + "' locked " + this);
    }
    else if (!lock.equals(lockOwner)) {
      throw new JbpmException('\'' + lockOwner + "' cannot lock " + this + " because '" + lock
        + "' already locked it");
    }
  }

  /**
   * @see #lock(String)
   */
  public void unlock(String lockOwner) {
    if (lock != null) {
      if (!lock.equals(lockOwner)) {
        throw new JbpmException('\'' + lockOwner + "' cannot unlock " + this + " because '"
          + lock + "' locked it");
      }

      lock = null;
      if (log.isDebugEnabled()) log.debug('\'' + lockOwner + "' unlocked " + this);
    }
    else {
      log.warn(this + " was already unlocked");
    }
  }

  /**
   * force unlocking the token, even if the owner is not known. In some use cases (e.g. in the
   * jbpm esb integration) the lock is persistent, so a state can be reached where the client
   * needs a possibility to force unlock of a token without knowing the owner.
   * 
   * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1888">JBPM-1888</a>
   * @deprecated Use {@link #forceUnlock()} instead
   */
  public void foreUnlock() {
    forceUnlock();
  }

  /**
   * force unlocking the token, even if the owner is not known. In some use cases (e.g. in the
   * jbpm esb integration) the lock is persistent, so a state can be reached where the client
   * needs a possibility to force unlock of a token without knowing the owner.
   * 
   * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1888">JBPM-1888</a>
   */
  public void forceUnlock() {
    if (lock != null) {
      lock = null;
      if (log.isDebugEnabled()) log.debug("forcefully unlocked " + this);
    }
    else {
      log.warn(this + " was unlocked already");
    }
  }

  /**
   * return the current lock owner of the token
   * 
   * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1888">JBPM-1888</a>
   */
  public String getLockOwner() {
    return lock;
  }

  public boolean isLocked() {
    return lock != null;
  }

  // getters and setters //////////////////////////////////////////////////////

  public long getId() {
    return id;
  }

  public Date getStart() {
    return start;
  }

  public Date getEnd() {
    return end;
  }

  public String getName() {
    return name;
  }

  public ProcessInstance getProcessInstance() {
    return processInstance;
  }

  public Map getChildren() {
    return children;
  }

  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }

  public Token getParent() {
    return parent;
  }

  public void setParent(Token parent) {
    this.parent = parent;
  }

  public void setProcessInstance(ProcessInstance processInstance) {
    this.processInstance = processInstance;
  }

  public ProcessInstance getSubProcessInstance() {
    return subProcessInstance;
  }

  public Date getNodeEnter() {
    return nodeEnter;
  }

  public void setNodeEnter(Date nodeEnter) {
    this.nodeEnter = nodeEnter;
  }

  public boolean isAbleToReactivateParent() {
    return isAbleToReactivateParent;
  }

  public void setAbleToReactivateParent(boolean isAbleToReactivateParent) {
    this.isAbleToReactivateParent = isAbleToReactivateParent;
  }

  public boolean isTerminationImplicit() {
    return isTerminationImplicit;
  }

  public void setTerminationImplicit(boolean isTerminationImplicit) {
    this.isTerminationImplicit = isTerminationImplicit;
  }

  public boolean isSuspended() {
    return isSuspended;
  }

  public void setSubProcessInstance(ProcessInstance subProcessInstance) {
    this.subProcessInstance = subProcessInstance;
  }

  private static final Log log = LogFactory.getLog(Token.class);
}
