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
package org.jbpm.instantiation;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.persistence.db.DbPersistenceService;

public class ProcessClassLoader extends ClassLoader {

  private ProcessDefinition processDefinition;
  private long processDefinitionId;

  private final JbpmConfiguration jbpmConfiguration;

  private final URLStreamHandler urlStreamHandler = new URLStreamHandler() {
    protected URLConnection openConnection(URL url) throws IOException {
      return new ProcessUrlConnection(url);
    }
  };

  private final class ProcessUrlConnection extends URLConnection {

    ProcessUrlConnection(URL url) {
      super(url);
    }

    public void connect() throws IOException {
      if (!connected) connected = true;
    }

    public InputStream getInputStream() throws IOException {
      connect();
      /*
       * ideally, the connection would fetch the process definition and assign it to a class
       * field on connect(); however, that strategy "leaks" a detached process definition
       */
      ProcessDefinition processDefinition = getProcessDefinition();
      if (processDefinition == null) throw new IOException("no active jbpm context");

      // having established a "connection", check whether the file exists
      String fileName = url.getPath();
      FileDefinition fileDefinition = processDefinition.getFileDefinition();
      if (fileDefinition == null || !fileDefinition.hasFile(fileName)) {
        throw new FileNotFoundException(fileName);
      }

      // retrieve file content
      byte[] fileContent = fileDefinition.getBytes(fileName);
      return new ByteArrayInputStream(fileContent);
    }
  }

  public ProcessClassLoader(ClassLoader parent, ProcessDefinition processDefinition) {
    this(parent, processDefinition, null);
  }

  ProcessClassLoader(ClassLoader parent, ProcessDefinition processDefinition,
    JbpmConfiguration jbpmConfiguration) {
    super(parent);
    // check whether the given process definition is transient
    long id = processDefinition.getId();
    if (id != 0) {
      // persistent, keep id only
      processDefinitionId = id;
      // remember jbpm configuration
      if (jbpmConfiguration != null) {
        this.jbpmConfiguration = jbpmConfiguration;
      }
      else {
        JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
        if (jbpmContext == null) throw new JbpmException("no active jbpm context");
        this.jbpmConfiguration = jbpmContext.getJbpmConfiguration();
      }
    }
    else {
      // transient, keep full object
      this.processDefinition = processDefinition;
      this.jbpmConfiguration = null;
    }
  }

  protected ProcessDefinition getProcessDefinition() {
    if (processDefinition != null) return processDefinition;

    if (jbpmConfiguration != null) {
      // is there an active context?
      JbpmContext jbpmContext = jbpmConfiguration.getCurrentJbpmContext();
      if (jbpmContext != null) {
        // check if transaction is still active before loading process definition
        // https://jira.jboss.org/browse/JBPM-2918
        PersistenceService persistenceService = jbpmContext.getServices()
          .getPersistenceService();
        if (persistenceService instanceof DbPersistenceService) {
          DbPersistenceService dbPersistenceService = (DbPersistenceService) persistenceService;
          if (dbPersistenceService.isTransactionActive()) {
            return persistenceService.getGraphSession()
              .loadProcessDefinition(processDefinitionId);
          }
        }
      }
    }

    return null;
  }

  protected URL findResource(String name) {
    ProcessDefinition processDefinition = getProcessDefinition();
    FileDefinition fileDefinition;
    if (processDefinition == null
      || (fileDefinition = processDefinition.getFileDefinition()) == null) return null;

    // skip leading slashes
    int off = 0;
    for (int len = name.length(); off < len && name.charAt(off) == '/'; off++)
      /* just increase offset */;

    // if the resource name is absolute, that is, starts with one or more slashes
    if (off > 0) {
      // then search from the root of the process archive
      name = name.substring(off);
    }
    else {
      // otherwise, if the resource is relative, search from the classes directory
      name = "classes/" + name;
    }

    if (!fileDefinition.hasFile(name)) return null;

    try {
      return new URL("jbpm", null, -1, name, urlStreamHandler);
    }
    catch (MalformedURLException e) {
      throw new JbpmException("could not create url", e);
    }
  }

  /** @deprecated not in use anymore */
  public static class BytesUrlStreamHandler extends URLStreamHandler {

    private final byte[] bytes;

    public BytesUrlStreamHandler(byte[] bytes) {
      this.bytes = bytes;
    }

    protected URLConnection openConnection(URL u) throws IOException {
      return new BytesUrlConnection(bytes, u);
    }
  }

  /** @deprecated not in use anymore */
  public static class BytesUrlConnection extends URLConnection {

    private final byte[] bytes;

    public BytesUrlConnection(byte[] bytes, URL u) {
      super(u);
      this.bytes = bytes;
    }

    public void connect() throws IOException {
    }

    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(bytes);
    }
  }

  protected Class findClass(String className) throws ClassNotFoundException {
    ProcessDefinition processDefinition = getProcessDefinition();
    FileDefinition fileDefinition;
    if (processDefinition == null
      || (fileDefinition = processDefinition.getFileDefinition()) == null) {
      throw new ClassNotFoundException(className);
    }

    // look in the classes directory of the file module definition
    String fileName = "classes/" + className.replace('.', '/') + ".class";
    byte[] classBytes = fileDefinition.getBytes(fileName);
    if (classBytes == null) throw new ClassNotFoundException(className);

    // if the class is in a package
    int packageIndex = className.lastIndexOf('.');
    if (packageIndex != -1) {
      // check whether this class loader (or any ancestor) defined the package already
      String packageName = className.substring(0, packageIndex);
      if (getPackage(packageName) == null) {
        // define the package prior to defining the class
        // see https://jira.jboss.org/jira/browse/JBPM-1404
        definePackage(packageName, null, null, null, processDefinition.getName(),
          Integer.toString(processDefinition.getVersion()), null, null);
      }
    }
    return defineClass(className, classBytes, 0, classBytes.length);
  }
}
