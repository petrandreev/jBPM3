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
import java.lang.reflect.Modifier;

import org.jbpm.file.def.FileDefinitionFileSystemConfigTest;
import org.jbpm.util.ClassLoaderUtil;

public class SerializabilityTest extends AbstractJbpmTestCase {

  String testRootDir = FileDefinitionFileSystemConfigTest.class.getProtectionDomain().getCodeSource().getLocation().getFile().toString();
  
  static String[] excusedClasses = {
      "org.jbpm.ant.",
      "org.jbpm.command.service.CommandServiceImpl",
      "org.jbpm.context.exe.JbpmType",
      "org.jbpm.db.hibernate.ConverterEnumType",
      "org.jbpm.db.hibernate.Converters",
      "org.jbpm.db.hibernate.JbpmNamingStrategy",
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
      "org.jbpm.graph.action.ActionTypes",
      "org.jbpm.graph.exe.ExecutionContext",
      "org.jbpm.graph.node.InterleaveStart$DefaultInterleaver",
      "org.jbpm.graph.node.NodeTypes",
      "org.jbpm.graph.node.ProcessFactory", 
      "org.jbpm.instantiation.BeanInstantiator",
      "org.jbpm.instantiation.ConfigurationPropertyInstantiator",
      "org.jbpm.instantiation.ConstructorInstantiator",
      "org.jbpm.instantiation.DefaultInstantiator",
      "org.jbpm.instantiation.FieldInstantiator",
      "org.jbpm.instantiation.ProcessClassLoader",
      "org.jbpm.instantiation.XmlInstantiator",
      "org.jbpm.job.executor.JobExecutorThread",
      "org.jbpm.job.executor.LockMonitorThread",
      "org.jbpm.jpdl.convert.Converter",
      "org.jbpm.jpdl.convert.Converter$1",
      "org.jbpm.jpdl.el.",
      "org.jbpm.jpdl.par.FileArchiveParser",
      "org.jbpm.jpdl.par.JpdlArchiveParser",
      "org.jbpm.jpdl.par.ProcessArchive",
      "org.jbpm.jpdl.xml.JpdlXmlReader",
      "org.jbpm.jpdl.xml.JpdlXmlWriter",
      "org.jbpm.jsf.",
      "org.jbpm.security.authenticator.JBossAuthenticator",
      "org.jbpm.security.authenticator.JbpmDefaultAuthenticator",
      "org.jbpm.security.authenticator.SubjectAuthenticator",
      "org.jbpm.security.Authorization",
      "org.jbpm.security.authorizer.AccessControllerAuthorizer",
      "org.jbpm.security.authorizer.JbpmIdentityAuthorizer",
      "org.jbpm.security.authorizer.RolesAuthorizer",
      "org.jbpm.security.filter.JbpmAuthenticationFilter",
      "org.jbpm.sim.",
      "org.jbpm.util.Clock",
      "org.jbpm.util.CustomLoaderObjectInputStream",
      "org.jbpm.web.JobExecutorLauncher",
      "org.jbpm.web.JbpmConfigurationCloser"
  };

  public void testForNonSerializableClasses() {
    File jbpmRoot = new File(testRootDir+"../classes/");
    scanForClasses(jbpmRoot, "");
  }
  
  private void scanForClasses(File rootClassDir, String packageDir) {
    File packageDirFile = new File(rootClassDir.getPath()+"/"+packageDir);
    File[] files = packageDirFile.listFiles();
    for (int i=0; i<files.length; i++) {
      if (files[i].isDirectory()) {
        String newPackageDir = ( "".equals(packageDir) ? files[i].getName() : packageDir+"/"+files[i].getName() );
        // log.debug("descending into directory "+newPackageDir);
        scanForClasses(rootClassDir, newPackageDir);
        
      } else if ( (files[i].isFile())
                  && (files[i].getName().endsWith(".class"))
                ) {
        // log.debug("found class file "+files[i].getName());
        String classFilePath = packageDir+"/"+files[i].getName();
        String className = classFilePath.replace('/', '.');
        className = className.substring(0, className.length()-6);
        assertSerializabilityOfClass(className);
      }
    }
  }

  private void assertSerializabilityOfClass(String className) {
    Class<?> clazz = ClassLoaderUtil.classForName(className);
    if ( ! ( Serializable.class.isAssignableFrom(clazz)
             || Modifier.isAbstract(clazz.getModifiers())
             || isExcused(className)
             || clazz.getConstructors().length == 0
           )
       ) {
      fail(className+" is NOT Serializable");
    }
  }

  boolean isExcused(String className) {
    for (String excusedClassName : excusedClasses) {
      if (className.startsWith(excusedClassName)) {
        return true;
      }      
    }
    return false;
  }

}
