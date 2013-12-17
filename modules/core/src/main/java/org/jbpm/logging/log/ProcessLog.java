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
package org.jbpm.logging.log;

import java.io.*;
import java.util.*;
import org.jbpm.graph.exe.*;

public abstract class ProcessLog implements Serializable {

  private static final long serialVersionUID = 1L;

  long id = 0;
  protected int index = -1;
  protected Date date = null;
  protected Token token = null;
  protected CompositeLog parent = null;

  public ProcessLog() {
  }

  /**
   * provides a text description for this update which can be used e.g. in the admin web console.
   */
  public String toString() {
    return super.toString();
  }

  public String getActorId() {
    String actorId = null;
    if (parent != null) {
      // AuthenticationLog overrides the getActorId
      actorId = parent.getActorId();
    }
    return actorId;
  }

  public void setToken(Token token) {
    this.token = token;
    this.index = token.nextLogIndex();
  }

  public void setParent(CompositeLog parent) {
    this.parent = parent;
  }

  public long getId() {
    return id;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public CompositeLog getParent() {
    return parent;
  }

  public Token getToken() {
    return token;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public List<ProcessLog> getChildren() {
    return null;
  }
}
