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
package org.jbpm.examples.action;

import org.jbpm.graph.def.*;
import org.jbpm.graph.exe.*;

// MyActionHandler represents a class that could execute 
// some user code during the execution of a jBPM process.
public class MyActionHandler implements ActionHandler {

  private static final long serialVersionUID = 1L;

  // Before each test (in the setUp), the isExecuted member 
  // will be set to false.
  public static boolean isExecuted = false;  

  // The action will set the isExecuted to true so the 
  // unit test will be able to show when the action
  // is being executed.
  public void execute(ExecutionContext executionContext) {
    isExecuted = true;
  }
}
