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
package org.jbpm;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jbpm.util.ClassLoaderUtil;

public class SerializabilityTest extends AbstractJbpmTestCase {

  static final String[] excusedClasses = {
    "org.jbpm.JbpmConfiguration$",
    "org.jbpm.ant.",
    "org.jbpm.context.exe.JbpmType",
    "org.jbpm.db.AbstractDbTestCase",
    "org.jbpm.db.ContextSession",
    "org.jbpm.db.FileSession",
    "org.jbpm.db.GraphSession",
    "org.jbpm.db.JbpmSession",
    "org.jbpm.db.JbpmSchema",
    "org.jbpm.db.JobSession",
    "org.jbpm.db.LoggingSession",
    "org.jbpm.db.SchedulerSession",
    "org.jbpm.db.TaskMgmtSession",
    "org.jbpm.db.compatibility.JbpmSchemaUpdate",
    "org.jbpm.db.hibernate.JbpmNamingStrategy",
    "org.jbpm.db.hibernate.MySQLDialect",
    "org.jbpm.db.hibernate.SybaseDialect",
    "org.jbpm.db.hibernate.SybaseRowLockDialect",
    "org.jbpm.graph.action.ActionTypes",
    "org.jbpm.graph.exe.ExecutionContext",
    "org.jbpm.graph.node.InterleaveStart$DefaultInterleaver",
    "org.jbpm.graph.node.NodeTypes",
    "org.jbpm.graph.node.ProcessFactory",
    "org.jbpm.instantiation.BeanInstantiator",
    "org.jbpm.instantiation.ConfigurationPropertyInstantiator",
    "org.jbpm.instantiation.ConstructorInstantiator",
    "org.jbpm.instantiation.DefaultInstantiator",
    "org.jbpm.instantiation.Delegation$CompactXmlWriter",
    "org.jbpm.instantiation.FieldInstantiator",
    "org.jbpm.instantiation.ProcessClassLoader",
    "org.jbpm.instantiation.XmlInstantiator",
    "org.jbpm.job.executor.DispatcherThread",
    "org.jbpm.job.executor.JobExecutorThread",
    "org.jbpm.job.executor.JobExecutor$JobRejectionHandler",
    "org.jbpm.job.executor.JobParcel",
    "org.jbpm.job.executor.LockMonitorThread",
    "org.jbpm.jpdl.convert.Converter",
    "org.jbpm.jpdl.el.",
    "org.jbpm.jpdl.par.FileArchiveParser",
    "org.jbpm.jpdl.par.JpdlArchiveParser",
    "org.jbpm.jpdl.xml.JpdlXmlReader",
    "org.jbpm.jpdl.xml.JpdlXmlWriter",
    "org.jbpm.persistence.db.StaleObjectLogConfigurer$LogWrapper",
    "org.jbpm.util.Clock$DefaultDateGenerator",
    "org.jbpm.util.CustomLoaderObjectInputStream",
    "org.jbpm.util.NodeIterator",
    "org.jbpm.util.XmlUtil$ElementPredicate",
    "org.jbpm.web.JobExecutorLauncher",
    "org.jbpm.web.JbpmConfigurationCloser"
  };

  public void testForNonSerializableClasses() throws URISyntaxException {
    URL location = ClassLoaderUtil.class.getProtectionDomain().getCodeSource().getLocation();
    File classDir = new File(new URI(location.toString()));

    File[] files = classDir.listFiles();
    if (files == null) return;

    // scan top level packages
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      if (file.isDirectory()) {
        scanForClasses(file, file.getName());
      }
    }
  }

  private static void scanForClasses(File classDir, String packageName) {
    File[] files = classDir.listFiles();
    if (files == null) return;

    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      String fileName = file.getName();
      if (file.isDirectory()) {
        scanForClasses(file, packageName + '.' + fileName);
      }
      else if (fileName.endsWith(".class")) {
        String className = packageName + '.' + fileName.substring(0, fileName.length() - 6);
        assertClassIsSerializable(className);
      }
    }
  }

  private static void assertClassIsSerializable(String className) {
    try {
      Class clazz = Class.forName(className);

      if (!Serializable.class.isAssignableFrom(clazz)
        && !Modifier.isAbstract(clazz.getModifiers())
        && !isAnonymous(clazz)
        && !isUtility(clazz)
        && !isExcused(className)) {
        fail(className + " is NOT Serializable");
      }
    }
    catch (ClassNotFoundException e) {
      fail("no such class: " + className);
    }
  }

  private static boolean isAnonymous(Class clazz) {
    return clazz.getName().matches(".*\\$\\d+");
  }

  /**
   * Tells whether the given class consists exclusively of static methods and has no public
   * constructors.
   */
  private static boolean isUtility(Class clazz) {
    Method[] methods = clazz.getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (!Modifier.isStatic(method.getModifiers())
        && method.getDeclaringClass() != Object.class) return false;
    }
    return clazz.getConstructors().length == 0;
  }

  private static boolean isExcused(String className) {
    for (int i = 0; i < excusedClasses.length; i++) {
      if (className.startsWith(excusedClasses[i])) return true;
    }
    return false;
  }

}
