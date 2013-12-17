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
package org.jbpm.command.impl;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.command.Command;
import org.jbpm.command.CommandService;

/**
 * The command service provides access the jBPM engine and its related services. Access is provided
 * through a set of {@link org.jbpm.command.Command} derived operations.
 * 
 * @author Jim Rigsbee
 * @author Tom Baeyens
 */
public class CommandServiceImpl implements CommandService, Serializable {

  protected final JbpmConfiguration jbpmConfiguration;

  private static final long serialVersionUID = 1L;

  /**
   * Establish an instance of the command service with a particular configuration.
   * 
   * @param jbpmConfiguration jBPM Configuration
   */
  public CommandServiceImpl(JbpmConfiguration jbpmConfiguration) {
    this.jbpmConfiguration = jbpmConfiguration;
  }

  /**
   * Executes command based on its current context. Each command contains the appropriate context
   * information such as token, process instance, etc. to insure that the operation is carried out
   * on the proper graph object.
   * 
   * @param command jBPM engine command to execute
   */
  public Object execute(Command command) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      log.debug("executing " + command);
      return command.execute(jbpmContext);
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      log.error("failed to execute " + command, e);
      throw e;
    }
    catch (Exception e) {
      jbpmContext.setRollbackOnly();
      log.error("failed to execute " + command, e);
      throw new JbpmException("failed to execute " + command, e);
    }
    finally {
      jbpmContext.close();
    }
  }

  private static final Log log = LogFactory.getLog(CommandServiceImpl.class);
}
