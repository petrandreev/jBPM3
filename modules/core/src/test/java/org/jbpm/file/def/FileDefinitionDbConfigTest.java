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
package org.jbpm.file.def;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.util.IoUtil;

public class FileDefinitionDbConfigTest extends AbstractJbpmTestCase {
  
  private FileDefinition fileDefinition = new FileDefinition();
  
  private InputStream getTestInputStream(String text) {
    return new ByteArrayInputStream(text.getBytes());
  }
  
  public void testStoreInputStreamGetInputStream() throws Exception {
    // store the value of hello world as an input stream
    fileDefinition.addFile("a/b/c/hello.txt", getTestInputStream("hello world") );
    // retrieve the input stream for the given file name
    InputStream in = fileDefinition.getInputStream("a/b/c/hello.txt");
    
    // extract the stream
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IoUtil.transfer(in, out);
    String retrievedText = out.toString();

    // check the result
    assertEquals( "hello world", retrievedText );
  }

  public void testStoreInputStreamGetBytes() throws Exception {
    // store the value of hello world as an input stream
    fileDefinition.addFile("a/b/c/hello.txt", getTestInputStream("hello world") );
    // retrieve the input stream for the given file name
    byte[] retrievedBytes = fileDefinition.getBytes("a/b/c/hello.txt");

    // check the result
    assertEquals( "hello world", new String(retrievedBytes) );
  }

  public void testStoreBytesGetInputStream() throws Exception {
    // store the value of 'hello world'
    fileDefinition.addFile("a/b/c/hello.txt", "hello world".getBytes() );

    // retrieve the input stream for the given file name
    InputStream in = fileDefinition.getInputStream("a/b/c/hello.txt");
    
    // extract the stream
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IoUtil.transfer(in, out);
    String retrievedText = out.toString();

    // check the result
    assertEquals( "hello world", retrievedText );
  }

  public void testStoreBytesGetBytes() throws Exception {
    // store the value of 'hello world'
    fileDefinition.addFile("a/b/c/hello.txt", "hello world".getBytes() );
    
    // retrieve the input stream for the given file name
    byte[] retrievedBytes = fileDefinition.getBytes("a/b/c/hello.txt");

    // check the result
    assertEquals( "hello world", new String(retrievedBytes) );
  }
}
