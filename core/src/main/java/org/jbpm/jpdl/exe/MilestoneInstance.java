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
package org.jbpm.jpdl.exe;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

public class MilestoneInstance implements Serializable {

  private static final long serialVersionUID = 1L;

  protected long id;
  protected String name;
  protected boolean reached;
  protected Token token;
  protected Collection listeners;

  public MilestoneInstance() {
  }

  public MilestoneInstance(String name) {
    this.name = name;
  }

  public static MilestoneInstance getMilestoneInstance(String milestoneName, Token token) {
    ContextInstance ci = token.getProcessInstance().getContextInstance();
    MilestoneInstance mi = (MilestoneInstance) ci.getVariable(milestoneName, token);
    if (mi == null) {
      mi = new MilestoneInstance(milestoneName);
      mi.setToken(token);
      ci.setVariable(milestoneName, mi);
    }
    return mi;
  }

  public void addListener(Token token) {
    if (listeners == null) listeners = new HashSet();
    listeners.add(token);
  }

  public void notifyListeners() {
    if (listeners != null) {
      // for every token that was waiting for this milestone
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        Token token = (Token) iter.next();
        // leave the milestone node
        Node node = token.getNode();
        ExecutionContext executionContext = new ExecutionContext(token);
        node.leave(executionContext);
      }
    }
  }

  public long getId() {
    return id;
  }

  /**
   * This method has no effect.
   * 
   * @deprecated database identified is not meant to be mutable
   */
  public void setId(long id) {
  }

  public Collection getListeners() {
    return listeners;
  }

  public void setListeners(Collection listeners) {
    this.listeners = listeners;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isReached() {
    return reached;
  }

  public void setReached(boolean reached) {
    this.reached = reached;
  }

  public Token getToken() {
    return token;
  }

  public void setToken(Token token) {
    this.token = token;
  }
}
