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
package org.jbpm.mock;

import java.util.Arrays;
import java.util.List;

public class Invocation {

  String methodName = null;
  Object[] args = null;

  public Invocation(String method) {
    this.methodName = method;
  }

  public Invocation(String method, Object[] args) {
    this.methodName = method;
    this.args = args;
  }

  public String getMethodName() {
    return methodName;
  }

  public Object getArg(int i) {
    return args[i];
  }

  public String toString() {
    if (args != null) {
      return methodName + Arrays.toString(args);
    }
    else {
      return methodName + "[]";
    }
  }

  public static Invocation getInvocation(List<Invocation> invocations, String methodName, int index) {
    int count = 0;
    for (Invocation invocation : invocations) {
      if (methodName.equals(invocation.getMethodName())) {
        if (count == index) {
          return invocation;
        }
        else {
          count++;
        }
      }
    }
    return null;
  }
}
