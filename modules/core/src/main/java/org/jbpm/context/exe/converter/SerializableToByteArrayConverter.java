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
package org.jbpm.context.exe.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.bytes.ByteArray;
import org.jbpm.context.exe.ContextConverter;
import org.jbpm.db.hibernate.Converters;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.Token;
import org.jbpm.util.CustomLoaderObjectInputStream;

public class SerializableToByteArrayConverter implements ContextConverter<Serializable, ByteArray> {

  private static final long serialVersionUID = 1L;

  public SerializableToByteArrayConverter() {
    Converters.registerConverter("R", this);
  }

  public boolean supports(Object value) {
    return value instanceof Serializable;
  }

  public ByteArray convert(Serializable o) {
    byte[] bytes = null;
    try {
      ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(memoryStream);
      objectStream.writeObject(o);
      objectStream.flush();
      bytes = memoryStream.toByteArray();
    }
    catch (IOException e) {
      throw new JbpmException("could not serialize: " + o, e);
    }
    return new ByteArray(bytes);
  }

  public Serializable revert(ByteArray o) {
    return revert(o, null);
  }

  public Serializable revert(ByteArray o, Token token) {
    InputStream memoryStream = new ByteArrayInputStream(o.getBytes());
    try {
      ObjectInputStream objectStream;
      if (token != null) {
        ProcessDefinition processDefinition = token.getProcessInstance().getProcessDefinition();
        ClassLoader classLoader = JbpmConfiguration.getProcessClassLoader(processDefinition);
        objectStream = new CustomLoaderObjectInputStream(memoryStream, classLoader);
      }
      else {
        objectStream = new ObjectInputStream(memoryStream);
      }
      return (Serializable) objectStream.readObject();
    }
    catch (IOException e) {
      throw new JbpmException("failed to read object", e);
    }
    catch (ClassNotFoundException e) {
      throw new JbpmException("serialized object class not found", e);
    }
  }
}
