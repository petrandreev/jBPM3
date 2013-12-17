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
package org.jbpm.jpdl.xml;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

public class Problem implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final int LEVEL_FATAL = 1;
  public static final int LEVEL_ERROR = 2;
  public static final int LEVEL_WARNING = 3;
  public static final int LEVEL_INFO = 4;

  static String getTypeDescription(int level) {
    switch (level) {
    case LEVEL_FATAL:
      return "FATAL";
    case LEVEL_ERROR:
      return "ERROR";
    case LEVEL_WARNING:
      return "WARNING";
    case LEVEL_INFO:
      return "INFO";
    }
    return null;
  }

  protected int level;
  protected String description;
  protected String resource;
  protected String folder;
  protected Integer line;
  protected Throwable exception;

  public Problem(int level, String description) {
    this.level = level;
    this.description = description;
  }

  public Problem(int level, String description, Throwable exception) {
    this.level = level;
    this.description = description;
    this.exception = exception;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append('[').append(getTypeDescription(level)).append(']');
    if (description != null) buffer.append(' ').append(description);
    if (resource != null) {
      buffer.append(" (").append(resource);
      if (line != null) buffer.append(':').append(line);
      buffer.append(')');
    }
    return buffer.toString();
  }

  public static boolean containsProblemsOfLevel(Collection c, int level) {
    for (Iterator iter = c.iterator(); iter.hasNext();) {
      Problem problem = (Problem) iter.next();
      if (problem.level <= level) return true;
    }
    return false;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Throwable getException() {
    return exception;
  }

  public void setException(Throwable exception) {
    this.exception = exception;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public Integer getLine() {
    return line;
  }

  public void setLine(Integer line) {
    this.line = line;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public int getLevel() {
    return level;
  }
}
