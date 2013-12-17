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
import java.util.Date;

import org.jbpm.security.SecurityHelper;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class Comment implements Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  int version;
  protected String actorId;
  protected Date time;
  protected String message;
  protected Token token;
  protected TaskInstance taskInstance;

  public Comment() {
  }

  public Comment(String message) {
    this.actorId = SecurityHelper.getAuthenticatedActorId();
    this.time = new Date();
    this.message = message;
  }

  public Comment(String actorId, String message) {
    this.actorId = actorId;
    this.time = new Date();
    this.message = message;
  }

  public String toString() {
    return "Comment(" + message + ')';
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Comment)) return false;

    Comment other = (Comment) o;
    if (id != 0 && id == other.getId()) return true;

    return message.equals(other.getMessage())
      && (actorId != null ? actorId.equals(other.getActorId())
          : other.getActorId() == null)
      && (taskInstance != null ? taskInstance.equals(other.getTaskInstance())
          : token != null ? token.equals(other.getToken()) : false);
  }

  public int hashCode() {
    int result = 769046417 + message.hashCode();
    result = 1770536419 * result + actorId != null ? actorId.hashCode() : 0;
    if (taskInstance != null) {
      result = 55354751 * result + taskInstance.hashCode();
    }
    else if (token != null) {
      result = 55354751 * result + token.hashCode();
    }
    return result;
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getActorId() {
    return actorId;
  }

  public long getId() {
    return id;
  }

  public String getMessage() {
    return message;
  }

  public Date getTime() {
    return time;
  }

  public TaskInstance getTaskInstance() {
    return taskInstance;
  }

  public Token getToken() {
    return token;
  }

  public void setTaskInstance(TaskInstance taskInstance) {
    this.taskInstance = taskInstance;
  }

  public void setToken(Token token) {
    this.token = token;
  }

  public void setActorId(String actorId) {
    this.actorId = actorId;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setTime(Date time) {
    this.time = time;
  }
}
