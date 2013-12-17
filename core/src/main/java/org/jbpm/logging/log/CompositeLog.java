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

import java.util.ArrayList;
import java.util.List;

public class CompositeLog extends ProcessLog {

  private static final long serialVersionUID = 1L;

  List children;

  public CompositeLog() {
  }

  public List getChildren() {
    return children;
  }

  public void setChildren(List children) {
    this.children = children;
  }

  public String toString() {
    return "composite";
  }

  public void addChild(ProcessLog processLog) {
    if (children == null) children = new ArrayList();
    children.add(processLog);
  }
}
