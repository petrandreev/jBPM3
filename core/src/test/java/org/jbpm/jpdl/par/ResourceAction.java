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
package org.jbpm.jpdl.par;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class ResourceAction implements ActionHandler {

  private static final long serialVersionUID = 1L;

  static Log log = LogFactory.getLog(ProcessArchiveClassLoadingDbTest.class);

  public void execute(ExecutionContext executionContext) throws Exception {
    Assert.assertEquals("org.jbpm.jpdl.par", getClass().getPackage().getName());

    URL resource = getClass().getResource("classresource.txt");
    InputStream stream = resource.openStream();
    String classResourceUrl = read(stream);
    Assert.assertEquals("the class resource content", classResourceUrl);

    stream = getClass().getResourceAsStream("classresource.txt");
    String classResourceStream = read(stream);
    Assert.assertEquals("the class resource content", classResourceStream);

    ClassLoader resourceActionClassLoader = ResourceAction.class.getClassLoader();
    log.info("resource action classloader: " + getClass().getClassLoader());
    log.info("parent of resource action classloader: "
        + getClass().getClassLoader().getParent());
    resource = resourceActionClassLoader.getResource("org/jbpm/jpdl/par/classresource.txt");
    stream = resource.openStream();
    String classLoaderResourceUrl = read(stream);
    Assert.assertEquals("the class resource content", classLoaderResourceUrl);

    stream = resourceActionClassLoader.getResourceAsStream("org/jbpm/jpdl/par/classresource.txt");
    String classLoaderResourceStream = read(stream);
    Assert.assertEquals("the class resource content", classLoaderResourceStream);

    resource = getClass().getResource("//archiveresource.txt");
    stream = resource.openStream();
    String archiveResourceUrl = read(stream);
    Assert.assertEquals("the archive resource content", archiveResourceUrl);

    stream = getClass().getResourceAsStream("//archiveresource.txt");
    String archiveResourceStream = read(stream);
    Assert.assertEquals("the archive resource content", archiveResourceStream);

    resource = resourceActionClassLoader.getResource("//archiveresource.txt");
    stream = resource.openStream();
    String archiveClassLoaderResourceUrl = read(stream);
    Assert.assertEquals("the archive resource content", archiveClassLoaderResourceUrl);

    stream = resourceActionClassLoader.getResourceAsStream("//archiveresource.txt");
    String archiveClassLoaderResourceStream = read(stream);
    Assert.assertEquals("the archive resource content", archiveClassLoaderResourceStream);

    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    log.info("resource action context classloader: " + contextClassLoader);
    log.info("parent of resource action context classloader: " + contextClassLoader.getParent());
    resource = contextClassLoader.getResource("//archiveresource.txt");
    stream = resource.openStream();
    String contextClassLoaderResourceUrl = read(stream);
    Assert.assertEquals("the archive resource content", contextClassLoaderResourceUrl);

    stream = contextClassLoader.getResourceAsStream("//archiveresource.txt");
    String contextClassLoaderResourceStream = read(stream);
    Assert.assertEquals("the archive resource content", contextClassLoaderResourceStream);

    try {
      getClass().getResourceAsStream("unexistingresource.txt");
    }
    catch (RuntimeException e) {
      // ok
    }

    try {
      resourceActionClassLoader.getResourceAsStream("org/jbpm/jpdl/par/unexistingresource.txt");
    }
    catch (RuntimeException e) {
      // ok
    }

    try {
      getClass().getResourceAsStream("//unexistingarchiveresource.txt");
    }
    catch (RuntimeException e) {
      // ok
    }
    try {
      resourceActionClassLoader.getResourceAsStream("//unexistingarchiveresource.txt");
    }
    catch (RuntimeException e) {
      // ok
    }
  }

  static String read(InputStream resourceAsStream) throws Exception {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
    StringBuffer buffer = new StringBuffer();
    String l;
    while ((l = bufferedReader.readLine()) != null) {
      buffer.append(l);
    }
    return buffer.toString();
  }
}