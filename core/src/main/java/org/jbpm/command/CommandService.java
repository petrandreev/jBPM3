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
package org.jbpm.command;

import org.jbpm.JbpmException;

/**
 * Provides jBPM engine services.
 * 
 * @author Jim Rigsbee, Tom Baeyens
 */
public interface CommandService
{

  /**
   * is the session facade that takes commands and executes them. The CommandService is responsible for creating or
   * obtaining the JbpmContext. The JbpmContext will be passed to the commands as a parameter. In a transactional
   * environment, the execute method demarcates a transaction. The command can be executed remotely and/or
   * asynchronously.
   * 
   * @param command engine command to execute
   * @return an object. The types of objects is determined by the command implementation. See those docs for more
   *         details on the return object.
   * @throws JbpmException
   */
  public Object execute(Command command);

}
