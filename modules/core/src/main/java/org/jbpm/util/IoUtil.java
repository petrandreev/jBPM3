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
package org.jbpm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IoUtil {
  private static final int BUFFERSIZE = 4096;

  private IoUtil() {
    // hide default constructor to prevent instantiation
  }

  public static byte[] readBytes(InputStream inputStream) throws IOException {
    if (inputStream == null) throw new IllegalArgumentException("InputStream cannot be null");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transfer(inputStream, outputStream);
    return outputStream.toByteArray();
  }

  public static int transfer(InputStream in, OutputStream out) throws IOException {
    if (in == null || out == null)
      throw new IllegalArgumentException("In/OutStream cannot be null");

    int total = 0;
    byte[] buffer = new byte[BUFFERSIZE];
    for (int bytesRead; (bytesRead = in.read(buffer)) != -1;) {
      out.write(buffer, 0, bytesRead);
      total += bytesRead;
    }
    return total;
  }
}
