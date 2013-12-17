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

import org.hibernate.SessionFactory;
import org.jbpm.configuration.ConfigurationException;
import org.jbpm.configuration.ObjectFactory;
import org.jbpm.configuration.ObjectFactoryImpl;
import org.jbpm.configuration.ObjectFactoryParser;
import org.jbpm.util.XmlException;

public class JbpmConfigurationTest extends AbstractJbpmTestCase
{

  protected void setUp() throws Exception
  {
    super.setUp();
    JbpmConfiguration.clearJbpmConfigurationStack();
    JbpmConfiguration.defaultObjectFactory = null;
    JbpmConfiguration.instances.clear();
  }

  public void testSingleton()
  {
    JbpmConfiguration.defaultObjectFactory = new ObjectFactoryImpl(null, null);
    JbpmConfiguration instance = JbpmConfiguration.getInstance();
    assertNotNull(instance);
    assertSame(instance, JbpmConfiguration.getInstance());
    assertSame(instance, JbpmConfiguration.getInstance());
    assertSame(instance, JbpmConfiguration.getInstance());
  }

  public void testDefaultContextCreation()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" + 
        "  <jbpm-context name='" + JbpmContext.DEFAULT_JBPM_CONTEXT_NAME + "' />" + 
        "</jbpm-configuration>");
    assertNotNull(jbpmConfiguration);
    assertNotNull(jbpmConfiguration.createJbpmContext());
  }

  public void testNonExistingContext()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration />");
    assertNotNull(jbpmConfiguration);
    try
    {
      jbpmConfiguration.createJbpmContext("non-existing-context");
      fail("expected exception");
    }
    catch (ConfigurationException e)
    {
      // OK
    }
  }

  public void testParseXmlStringConfiguration()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" + 
        "  <jbpm-context name='a' />" + 
        "  <jbpm-context name='b' />" + 
        "</jbpm-configuration>");
    assertNotNull(jbpmConfiguration);
    JbpmContext a = jbpmConfiguration.createJbpmContext("a");
    assertNotNull(a);
    JbpmContext b = jbpmConfiguration.createJbpmContext("b");
    assertNotNull(b);
    assertNotSame(a, b);
  }

  public void testNonSingletonContextCreation()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" + 
        "  <jbpm-context name='a' />" + 
        "</jbpm-configuration>");
    assertNotNull(jbpmConfiguration);
    JbpmContext a = jbpmConfiguration.createJbpmContext("a");
    assertNotNull(a);
    JbpmContext a2 = jbpmConfiguration.createJbpmContext("a");
    assertNotNull(a2);
    assertNotSame(a, a2);
  }

  public void testParseXmlFault()
  {
    try
    {
      JbpmConfiguration.parseXmlString("<  problematic //   <</>  <x>M/L");
      fail("expected exception");
    }
    catch (XmlException e)
    {
      // OK
    }
  }

  public void testDomainModelConfigsWithoutJbpmContext()
  {
    // Without a current JbpmContext, the jbpm domain model objects that
    // need configuration information fetch it from the object factory of
    // the singleton instance. The singleton instance object factory is
    // by default initialized with the jbpm.cfg.xml resource unless, a
    // custom default object factory is specified.
    //
    // So to use jBPM without a JbpmContext, you can only use the resource with name
    // 'jbpm.cfg.xml' or the static method JbpmConfiguration.Configs.setDefaultObjectFactory
    // to specify the configuration information.

    JbpmConfiguration.Configs.setDefaultObjectFactory(ObjectFactoryParser.parseXmlString(
        "<jbpm-configuration>" + 
        "  <string name='myproperty'>myvalue</string>" + 
        "</jbpm-configuration>"));
    assertEquals("myvalue", JbpmConfiguration.Configs.getString("myproperty"));
  }

  public static class CustomObjectFactory implements ObjectFactory
  {
    private static final long serialVersionUID = 1L;

    public Object createObject(String name)
    {
      Object o = null;
      if ("myproperty".equals(name))
      {
        o = "mycustomfactoriedvalue";
      }
      return o;
    }

    public boolean hasObject(String name)
    {
      return "myproperty".equals(name);
    }
  }

  public void testDomainModelConfigsWithCustomObjectFactory()
  {
    JbpmConfiguration.Configs.setDefaultObjectFactory(new CustomObjectFactory());
    assertEquals("mycustomfactoriedvalue", JbpmConfiguration.Configs.getString("myproperty"));
  }

  public void testDomainModelConfigsWithJbpmContext()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" + 
        "  <jbpm-context name='default.jbpm.context' />" + 
        "  <string name='myproperty'>myvalueinacontext</string>" + 
        "</jbpm-configuration>");

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try
    {
      assertEquals("myvalueinacontext", JbpmConfiguration.Configs.getString("myproperty"));
    }
    finally
    {
      jbpmContext.close();
    }
  }

  public void testDomainModelConfigsWithNestedJbpmContext()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" + 
        "  <jbpm-context name='default.jbpm.context' />" + 
        "  <string name='myproperty'>myvalueinacontext</string>" + 
        "</jbpm-configuration>");
    
    JbpmConfiguration nestedJbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" + 
        "  <jbpm-context name='default.jbpm.context' />" + 
        "  <string name='myproperty'>myvalueinanestedcontext</string>" + 
        "</jbpm-configuration>");

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try
    {
      assertEquals("myvalueinacontext", JbpmConfiguration.Configs.getString("myproperty"));

      JbpmContext nestedJbpmContext = nestedJbpmConfiguration.createJbpmContext();
      try
      {
        assertEquals("myvalueinanestedcontext", JbpmConfiguration.Configs.getString("myproperty"));
      }
      finally
      {
        nestedJbpmContext.close();
      }

      assertEquals("myvalueinacontext", JbpmConfiguration.Configs.getString("myproperty"));
    }
    finally
    {
      jbpmContext.close();
    }
  }

  public void testJbpmConfigurationClose()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance();
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    SessionFactory sessionFactory;
    try
    {
      sessionFactory = jbpmContext.getSessionFactory();
    }
    finally
    {
      jbpmContext.close();
    }
    jbpmConfiguration.close();

    assertTrue("expected " + sessionFactory + " to be closed", sessionFactory.isClosed());
  }
}
