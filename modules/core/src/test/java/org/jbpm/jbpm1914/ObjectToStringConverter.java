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
package org.jbpm.jbpm1914;

import java.lang.reflect.Constructor;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextConverter;
import org.jbpm.db.hibernate.Converters;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 */
public class ObjectToStringConverter implements ContextConverter<Object, String> {

  private static final long serialVersionUID = 1L;
  private static final char SEPARATOR = ';';

  public ObjectToStringConverter() {
    Converters.registerConverter("$", this);
  }

  public String convert(Object o) {
    return o.getClass().getName() + SEPARATOR + o;
  }

  public Object revert(String o) {
    return revert(o, null);
  }

  public Object revert(String o, Token token) {
    int separatorIndex = o.indexOf(SEPARATOR);
    try {
      ClassLoader classLoader = null;
      if (token != null) {
        classLoader = JbpmConfiguration.getProcessClassLoader(token.getProcessInstance()
            .getProcessDefinition());
      }
      Class<?> type = Class.forName(o.substring(0, separatorIndex), true, classLoader);
      Constructor<?> constructor = type.getConstructor(String.class);

      return constructor.newInstance(o.substring(separatorIndex + 1));
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new JbpmException("could not revert: " + o, e);
    }
  }

  public boolean supports(Object value) {
    return true;
  }

}
