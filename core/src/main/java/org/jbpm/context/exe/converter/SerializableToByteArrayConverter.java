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
import org.jbpm.context.exe.Converter;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.util.CustomLoaderObjectInputStream;

public class SerializableToByteArrayConverter implements Converter {

  private static final long serialVersionUID = 1L;

  public boolean supports(Object value) {
    return value instanceof Serializable || value == null;
  }

  public Object convert(Object o) {
    if (o == null) return null;

    try {
      ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(memoryStream);
      objectStream.writeObject(o);
      objectStream.flush();
      return new ByteArray(memoryStream.toByteArray());
    }
    catch (IOException e) {
      throw new JbpmException("failed to serialize: " + o, e);
    }
  }

  public Object revert(Object o) {
    if (o == null) return o;

    ByteArray byteArray = (ByteArray) o;
    InputStream memoryStream = new ByteArrayInputStream(byteArray.getBytes());
    try {
      ObjectInputStream objectStream = new ObjectInputStream(memoryStream);
      return objectStream.readObject();
    }
    catch (IOException e) {
      throw new JbpmException("failed to deserialize object", e);
    }
    catch (ClassNotFoundException e) {
      throw new JbpmException("serialized class not found", e);
    }
  }

  public Object revert(Object o, final ProcessDefinition processDefinition) {
    ByteArray byteArray = (ByteArray) o;
    if( byteArray.getBytes().length > 0 ) { 
      InputStream memoryStream = new ByteArrayInputStream(byteArray.getBytes());
      try {
        ObjectInputStream objectStream = new CustomLoaderObjectInputStream(memoryStream,
          JbpmConfiguration.getProcessClassLoader(processDefinition));
        return objectStream.readObject();
      }
      catch (IOException e) {
        throw new JbpmException("failed to deserialize object", e);
      }
      catch (ClassNotFoundException e) {
        throw new JbpmException("serialized class not found", e);
      }
    }
    else { 
      return null;
    }
  }
}
