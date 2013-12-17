/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jbpm.command;

import org.jbpm.JbpmContext;
import org.jbpm.util.ClassUtil;

/**
 * Delete a proces definition by ID
 * 
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class DeleteProcessDefinitionCommand extends AbstractGetObjectBaseCommand {

  private static final long serialVersionUID = -1908847549444051495L;

  private long id;

  public DeleteProcessDefinitionCommand() {
  }

  public DeleteProcessDefinitionCommand(long id) {
    super();
    this.id = id;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    jbpmContext.getGraphSession().deleteProcessDefinition(id);
    return Boolean.TRUE;
  }

  public String toString() {
    return ClassUtil.getSimpleName(getClass()) + " [processDefinition.id=" + id + "]";
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  // methods for fluent programming

  public DeleteProcessDefinitionCommand id(long id) {
    setId(id);
    return this;
  }

}
