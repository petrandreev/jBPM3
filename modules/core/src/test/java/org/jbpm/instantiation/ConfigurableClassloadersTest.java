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

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * {@link ProcessClassLoaderFactory} and class loader can be configured 
 * via {@link JbpmConfiguration} / jbpm.cfg.xml. Test if that works.
 * 
 * Introduced with https://jira.jboss.org/jira/browse/JBPM-1148
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ConfigurableClassloadersTest extends AbstractJbpmTestCase {
  
  public void testDefaultBehavior() {
    JbpmConfiguration.getInstance();
    ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(null);
    
    assertNotNull(processClassLoader);
    assertEquals(ProcessClassLoader.class, processClassLoader.getClass());
  }

  public static class MyClassLoader extends ClassLoader {
    public MyClassLoader() {      
    }
    public MyClassLoader(ClassLoader parent) {
      super(parent);
    }    
  }
  
  public static class TestProcessClassLoaderFactory implements ProcessClassLoaderFactory {
    public ClassLoader getProcessClassLoader(ProcessDefinition processDefinition) {
      return new MyClassLoader(Thread.currentThread().getContextClassLoader());
    }   
  }

  public void testOwnProcessFactory() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
            "<jbpm-configuration>" +
            "  <jbpm-context>" +
            "  </jbpm-context>" +
            "  <string name='jbpm.classLoader' value='jbpm' /> " +
            "  <bean name='jbpm.processClassLoader' class='org.jbpm.instantiation.ConfigurableClassloadersTest$TestProcessClassLoaderFactory' singelton='true' />" +
            "</jbpm-configuration>"
    );
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    
    ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(null);    
    assertNotNull(processClassLoader);
    assertEquals(MyClassLoader.class, processClassLoader.getClass());
    
    jbpmContext.close();
    jbpmConfiguration.close();
  }
  
  public void testContextClassloaderFactory() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
            "<jbpm-configuration>" +
            "  <jbpm-context>" +
            "  </jbpm-context>" +
            "  <string name='jbpm.classLoader' value='context' /> " +
            "</jbpm-configuration>"
    );
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();

    assertNotNull(jbpmContext);
    
    jbpmContext.close();
    jbpmConfiguration.close();
  }

  public void testCustomClassloaderFactoryWithoutClassname() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
            "<jbpm-configuration>" +
            "  <jbpm-context>" +
            "  </jbpm-context>" +
            "  <string name='jbpm.classLoader' value='custom' /> " +
            "</jbpm-configuration>"
    );
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(null);    
      fail("we should get an exception because custom class loader class not specified");
    }
    catch (Exception ex) {}
    
    jbpmContext.close();
    jbpmConfiguration.close();
  }
  
  public void testCustomClassloaderFactory() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
            "<jbpm-configuration>" +
            "  <jbpm-context>" +
            "  </jbpm-context>" +
            "  <string name='jbpm.classLoader' value='custom' /> " +
            "  <string name='jbpm.customClassLoader.className' value='org.jbpm.instantiation.ConfigurableClassloadersTest$MyClassLoader' />" +
            "</jbpm-configuration>"
    );
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    
    ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(null);    
    assertNotNull(processClassLoader);
    // not configured, must be default
    assertEquals(ProcessClassLoader.class, processClassLoader.getClass());
    
    jbpmContext.close();
    jbpmConfiguration.close();
  }
  
}
