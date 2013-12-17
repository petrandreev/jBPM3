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
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.util.ClassLoaderUtil;

/**
 * {@link ProcessClassLoaderFactory} and class loader can be configured via
 * {@link JbpmConfiguration} / jbpm.cfg.xml. Test if that works. Introduced with
 * https://jira.jboss.org/jira/browse/JBPM-1148
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ConfigurableClassLoadersTest extends AbstractJbpmTestCase {

  private ProcessDefinition processDefinition = new ProcessDefinition();

  public void testDefaultProcessClassLoaderFactory() {
    ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(processDefinition);
    assertSame(ProcessClassLoader.class, processClassLoader.getClass());
  }

  public void testCustomProcessClassLoaderFactory() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
        + "  <bean name='process.class.loader.factory' class='"
        + CustomProcessClassLoaderFactory.class.getName()
        + "' singleton='true' />"
        + "</jbpm-configuration>");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();

    ClassLoader processClassLoader = JbpmConfiguration.getProcessClassLoader(processDefinition);
    assertSame(CustomClassLoader.class, processClassLoader.getClass());

    jbpmContext.close();
    jbpmConfiguration.close();
  }

  public static class CustomProcessClassLoaderFactory implements ProcessClassLoaderFactory {

    private static final long serialVersionUID = 1L;

    public ClassLoader getProcessClassLoader(ProcessDefinition processDefinition) {
      return new CustomClassLoader();
    }
  }

  public void testDefaultClassLoader() {
    ClassLoader classLoader = ClassLoaderUtil.getClassLoader();
    assertSame(ClassLoaderUtil.class.getClassLoader(), classLoader);
  }

  public void testJbpmClassLoader() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
        + "  <string name='jbpm.class.loader' value='jbpm' /> "
        + "</jbpm-configuration>");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();

    ClassLoader classLoader = ClassLoaderUtil.getClassLoader();
    assertSame(ClassLoaderUtil.class.getClassLoader(), classLoader);

    jbpmContext.close();
    jbpmConfiguration.close();
  }

  public void testContextClassLoader() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
        + "  <string name='jbpm.class.loader' value='context' /> "
        + "</jbpm-configuration>");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();

    ClassLoader classLoader = ClassLoaderUtil.getClassLoader();
    assertSame(Thread.currentThread().getContextClassLoader(), classLoader);

    jbpmContext.close();
    jbpmConfiguration.close();
  }

  public void testNonExistentClassLoader() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
        + "  <string name='jbpm.class.loader' value='absent' /> "
        + "</jbpm-configuration>");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();

    try {
      ClassLoaderUtil.getClassLoader();
      fail("expected " + JbpmException.class.getName());
    }
    catch (JbpmException ex) {
      // fine, exception was expected
    }

    jbpmContext.close();
    jbpmConfiguration.close();
  }

  public void testCustomClassLoader() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
        + "  <string name='jbpm.class.loader' value='custom.class.loader' />"
        + "  <bean name='custom.class.loader' class='"
        + CustomClassLoader.class.getName()
        + "' /> "
        + "</jbpm-configuration>");
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();

    ClassLoader classLoader = ClassLoaderUtil.getClassLoader();
    assertSame(CustomClassLoader.class, classLoader.getClass());

    jbpmContext.close();
    jbpmConfiguration.close();
  }

  public static class CustomClassLoader extends ClassLoader {

    public CustomClassLoader() {
      super(Thread.currentThread().getContextClassLoader());
    }
  }

}
