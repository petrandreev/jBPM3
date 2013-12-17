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
package org.jbpm.jpdl;

import java.util.Collections;
import java.util.List;

import org.jbpm.JbpmException;
import org.jbpm.jpdl.xml.Problem;

public class JpdlException extends JbpmException {

  private static final long serialVersionUID = 1L;

  protected List problems;

  public JpdlException(List problems) {
    super(problems.size() + " problems found");
    this.problems = problems;
  }

  public JpdlException(String message) {
    super(message);
    problems = Collections.singletonList(new Problem(Problem.LEVEL_ERROR, message));
  }

  public JpdlException(String message, Throwable cause) {
    super(message, cause);
    problems = Collections.singletonList(new Problem(Problem.LEVEL_ERROR, message, cause));
  }

  public List getProblems() {
    return problems;
  }
}
