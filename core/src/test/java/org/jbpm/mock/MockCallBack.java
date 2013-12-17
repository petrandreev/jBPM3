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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class MockCallBack implements MethodInterceptor {
  
  Object o = null;
  List invocations = null;
  public MockCallBack(Object o) {
    this.o = o;
    this.invocations = new ArrayList();
  }
  public Object intercept(Object object, Method m, Object[] args, MethodProxy mp) throws Throwable {
    if (m.getDeclaringClass()==Recorded.class) {
      if ("getInvocations".equals(m.getName())) {
        return invocations;
      }
      throw new RuntimeException("invalid Recorded method on mocked object");
    } else {
      invocations.add(new Invocation(m.getName(), args));
      return m.invoke(this.o, args);
    }
  }  
}