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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.configuration.ObjectFactory;
import org.jbpm.configuration.ObjectFactoryImpl;
import org.jbpm.configuration.ObjectFactoryParser;
import org.jbpm.configuration.ObjectInfo;
import org.jbpm.configuration.ValueInfo;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.instantiation.DefaultProcessClassLoaderFactory;
import org.jbpm.instantiation.ProcessClassLoaderFactory;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.util.ClassLoaderUtil;

/**
 * configuration of one jBPM instance.
 * <p>
 * During process execution, jBPM might need to use some services. A JbpmConfiguration contains the
 * knowledge on how to create those services.
 * </p>
 * <p>
 * A JbpmConfiguration is a thread safe object and serves as a factory for
 * {@link org.jbpm.JbpmContext}s, which means one JbpmConfiguration can be used to create
 * {@link org.jbpm.JbpmContext}s for all threads. The single JbpmConfiguration can be maintained in
 * a static member or in the JNDI tree if that is available.
 * </p>
 * <p>
 * A JbpmConfiguration can be obtained in following ways:
 * </p>
 * <ul>
 * <li>from a resource (by default <code>jbpm.cfg.xml</code> is used):
 * 
 * <pre>
 * JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance();
 * </pre>
 * 
 * <p>
 * or
 * </p>
 * 
 * <pre>
 * String myXmlResource = &quot;...&quot;;
 * JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(myXmlResource);
 * </pre>
 * 
 * </li>
 * <li>from an XML string:
 * 
 * <pre>
 * JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
 *   &quot;&lt;jbpm-configuration&gt;&quot; +
 *   ...
 *   &quot;&lt;/jbpm-configuration&gt;&quot;
 * );
 * </pre>
 * 
 * </li>
 * <li>By specifying a custom implementation of an object factory. This can be used to specify a
 * JbpmConfiguration in other bean-style notations such as used by JBoss Microcontainer or Spring.
 * 
 * <pre>
 * ObjectFactory of = new &lt;i&gt;MyCustomObjectFactory&lt;/i&gt;();
 * JbpmConfiguration.Configs.setDefaultObjectFactory(of);
 * JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance();
 * </pre>
 * 
 * </li>
 * </ul>
 * <p>
 * JbpmConfigurations can be configured using a spring-like XML notation (in relax ng compact
 * notation):
 * </p>
 * 
 * <pre>
 * datatypes xs = &quot;http://www.w3.org/2001/XMLSchema-datatypes&quot;
 * 
 * start = element beans { element object* }
 * 
 * object = {
 *   jbpm-context |
 *   bean |
 *   ref |
 *   map |
 *   list |
 *   string |
 *   int |
 *   long |
 *   float |
 *   double |
 *   char |
 *   bool |
 *   true |
 *   false |
 *   null
 * }
 * 
 * jbpm-context = element jbpm-context {
 *   ( attribute name {xsd:string},
 *     service*,
 *     save-operations? 
 *   )
 * }
 * 
 * service = element service {
 *   ( attribute name {xsd:string},
 *     ( attribute factory {xsd:string} ) |
 *     ( factory )
 *   )
 * }
 * 
 * factory = element factory {
 *   ( bean |
 *     ref
 *   )
 * }
 * 
 * save-operations = element save-operations {
 *   ( save-operation* )
 * }
 * 
 * save-operation = element save-operation {
 *   ( ( attribute class {xsd:string} ) |
 *     ( bean |
 *       ref
 *     ) 
 *   )
 * }
 * 
 * bean = element bean {
 *   ( attribute ref-name {xsd:string} ) |
 *   ( attribute name {xsd:string}?,
 *     attribute class {xsd:string}?,
 *     attribute singleton { &quot;true&quot; | &quot;false&quot; }?,
 *     constructor*,
 *     field*,
 *     property*
 *   )
 * }
 * 
 * ref = element ref {
 *   ( attribute bean (xsd:string) )
 * }
 * 
 * constructor = element constructor {
 *   attribute class {xsd:string}?,
 *   ( attribute factory {xsd:string}, 
 *     attribute method {xsd:string}
 *   )?,
 *   parameter*
 * }
 * 
 * parameter = element parameter {
 *   attribute class {xsd:string},
 *   object
 * }
 * 
 * field = element field {
 *   attribute name {xsd:string},
 *   object
 * }
 * 
 * property = element property {
 *   ( attribute name {xsd:string} |
 *     attribute setter {xsd:string}
 *   ),
 *   object
 * }
 * 
 * map = element map {
 *   entry*
 * }
 * 
 * entry = element entry { 
 *   key, 
 *   value 
 * }
 * 
 * key = element key {
 *   object
 * }
 * 
 * value = element value {
 *   object
 * }
 * 
 * list = element list {
 *   object*
 * }
 * 
 * string = element string {xsd:string}
 * int    = element integer {xsd:integer}
 * long   = element long {xsd:long}
 * float  = element float {xsd:string}
 * double = element string {xsd:double}
 * char   = element char {xsd:character}
 * bool   = element bool { &quot;true&quot; | &quot;false&quot; }
 * true   = element true {}
 * false  = element false {}
 * null   = element null {}
 * </pre>
 * 
 * </p>
 * <p>
 * Other configuration properties
 * </p>
 * <table>
 * <tr>
 * <td>jbpm.files.dir</td>
 * <td/>
 * </tr>
 * <tr>
 * <td>jbpm.types</td>
 * <td/>
 * </tr>
 * </table>
 */
public class JbpmConfiguration implements Serializable
{

  private static final long serialVersionUID = 1L;

  static ObjectFactory defaultObjectFactory;
  static final Map<String, JbpmConfiguration> instances = new HashMap<String, JbpmConfiguration>();
  static final ThreadLocal<List<JbpmConfiguration>> jbpmConfigurationStacks = new StackThreadLocal<JbpmConfiguration>();

  private final ObjectFactory objectFactory;
  private final ThreadLocal<List<JbpmContext>> jbpmContextStacks = new StackThreadLocal<JbpmContext>();
  private JobExecutor jobExecutor;
  private boolean isClosed;

  static class StackThreadLocal<E> extends ThreadLocal<List<E>>
  {
    @Override
    protected List<E> initialValue()
    {
      return new ArrayList<E>();
    }
  }

  public JbpmConfiguration(ObjectFactory objectFactory)
  {
    this.objectFactory = objectFactory;
  }

  public static JbpmConfiguration getInstance()
  {
    return getInstance(null);
  }

  public static JbpmConfiguration getInstance(String resource)
  {
    if (resource == null)
    {
      resource = "jbpm.cfg.xml";
    }

    JbpmConfiguration instance;
    synchronized (instances)
    {
      // look for configuration in cache
      instance = instances.get(resource);
      if (instance == null)
      {
        // configuration does not exist or was evicted, construct it
        if (defaultObjectFactory != null)
        {
          log.debug("creating configuration from default object factory: " + defaultObjectFactory);
          instance = new JbpmConfiguration(defaultObjectFactory);
        }
        else
        {
          log.info("using configuration resource: " + resource);
          InputStream jbpmCfgXmlStream = ClassLoaderUtil.getJbpmConfigurationStream(resource);
          /*
           * if a custom resource is specified, but not found in the classpath, log a warning; otherwise, users who want to load custom stuff will not receive any
           * feedback when their resource cannot be found
           */
          if (jbpmCfgXmlStream == null && !"jbpm.cfg.xml".equals(resource))
          {
            log.warn("configuration resource '" + resource + "' could not be found");
          }
          ObjectFactory objectFactory = parseObjectFactory(jbpmCfgXmlStream);
          instance = createJbpmConfiguration(objectFactory);
        }
        // put configuration in cache
        instances.put(resource, instance);
      }
    }
    return instance;
  }

  public static boolean hasInstance(String resource)
  {
    return instances.containsKey(resource != null ? resource : "jbpm.cfg.xml");
  }

  protected static ObjectFactory parseObjectFactory(InputStream inputStream)
  {
    log.debug("loading defaults in jbpm configuration");
    ObjectFactoryParser objectFactoryParser = new ObjectFactoryParser();
    ObjectFactoryImpl objectFactoryImpl = new ObjectFactoryImpl();
    objectFactoryParser.parseElementsFromResource("org/jbpm/default.jbpm.cfg.xml", objectFactoryImpl);

    if (inputStream != null)
    {
      log.debug("loading specific configuration...");
      objectFactoryParser.parseElementsStream(inputStream, objectFactoryImpl);
    }

    return objectFactoryImpl;
  }

  public static JbpmConfiguration parseXmlString(String xml)
  {
    log.debug("creating jbpm configuration from xml string");
    InputStream inputStream = null;
    if (xml != null)
    {
      inputStream = new ByteArrayInputStream(xml.getBytes());
    }
    ObjectFactory objectFactory = parseObjectFactory(inputStream);
    return createJbpmConfiguration(objectFactory);
  }

  protected static JbpmConfiguration createJbpmConfiguration(ObjectFactory objectFactory)
  {
    JbpmConfiguration jbpmConfiguration = new JbpmConfiguration(objectFactory);

    // make the bean jbpm.configuration always available
    if (objectFactory instanceof ObjectFactoryImpl)
    {
      ObjectFactoryImpl objectFactoryImpl = (ObjectFactoryImpl)objectFactory;
      ObjectInfo jbpmConfigurationInfo = new ValueInfo("jbpmConfiguration", jbpmConfiguration);
      objectFactoryImpl.addObjectInfo(jbpmConfigurationInfo);

      if (getHideStaleObjectExceptions(objectFactory))
      {
        StaleObjectLogConfigurer.hideStaleObjectExceptions();
      }
    }

    return jbpmConfiguration;
  }

  private static boolean getHideStaleObjectExceptions(ObjectFactory objectFactory)
  {
    if (!objectFactory.hasObject("jbpm.hide.stale.object.exceptions"))
      return true;

    Object object = objectFactory.createObject("jbpm.hide.stale.object.exceptions");
    return object instanceof Boolean ? (Boolean)object : true;
  }

  public static JbpmConfiguration parseInputStream(InputStream inputStream)
  {
    log.debug("creating jbpm configuration from input stream");
    ObjectFactory objectFactory = parseObjectFactory(inputStream);
    return createJbpmConfiguration(objectFactory);
  }

  public static JbpmConfiguration parseResource(String resource)
  {
    log.debug("creating jbpm configuration from resource: " + resource);
    InputStream inputStream = null;
    if (resource != null)
    {
      inputStream = ClassLoaderUtil.getJbpmConfigurationStream(resource);
    }
    ObjectFactory objectFactory = parseObjectFactory(inputStream);
    return createJbpmConfiguration(objectFactory);
  }

  public JbpmContext createJbpmContext()
  {
    return createJbpmContext(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public JbpmContext createJbpmContext(String name)
  {
    ensureOpen();

    JbpmContext jbpmContext = (JbpmContext)objectFactory.createObject(name);
    jbpmContext.jbpmConfiguration = this;
    jbpmContextCreated(jbpmContext);
    return jbpmContext;
  }

  private void ensureOpen()
  {
    if (isClosed)
      throw new JbpmException("configuration closed");
  }

  public ServiceFactory getServiceFactory(String serviceName)
  {
    return getServiceFactory(serviceName, JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public ServiceFactory getServiceFactory(String serviceName, String jbpmContextName)
  {
    ServiceFactory serviceFactory = null;
    JbpmContext jbpmContext = createJbpmContext(jbpmContextName);
    try
    {
      serviceFactory = jbpmContext.getServices().getServiceFactory(serviceName);
    }
    finally
    {
      jbpmContext.close();
    }
    return serviceFactory;
  }

  public static ClassLoader getProcessClassLoader(ProcessDefinition processDefinition)
  {
    ProcessClassLoaderFactory factory = null;
    if (Configs.hasObject("jbpm.processClassLoader"))
    {
      factory = (ProcessClassLoaderFactory)Configs.getObject("jbpm.processClassLoader");
    }
    else
    {
      factory = new DefaultProcessClassLoaderFactory();
    }
    return factory.getProcessClassLoader(processDefinition);
  }

  /*
   * gives the jbpm domain model access to configuration information via the current JbpmContext.
   */
  public static class Configs
  {

    private Configs()
    {
      // hide default constructor to prevent instantiation
    }

    public static ObjectFactory getObjectFactory()
    {
      ObjectFactory objectFactory = null;
      JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
      if (jbpmContext != null)
      {
        objectFactory = jbpmContext.objectFactory;
      }
      else
      {
        objectFactory = getInstance().objectFactory;
      }
      return objectFactory;
    }

    public static void setDefaultObjectFactory(ObjectFactory objectFactory)
    {
      defaultObjectFactory = objectFactory;
    }

    public static boolean hasObject(String name)
    {
      ObjectFactory objectFactory = getObjectFactory();
      return objectFactory.hasObject(name);
    }

    public static synchronized Object getObject(String name)
    {
      ObjectFactory objectFactory = getObjectFactory();
      return objectFactory.createObject(name);
    }

    public static String getString(String name)
    {
      return (String)getObject(name);
    }

    public static long getLong(String name)
    {
      return ((Long)getObject(name)).longValue();
    }

    public static int getInt(String name)
    {
      return ((Integer)getObject(name)).intValue();
    }

    public static boolean getBoolean(String name)
    {
      return ((Boolean)getObject(name)).booleanValue();
    }
  }

  public void cleanSchema()
  {
    cleanSchema(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void cleanSchema(String jbpmContextName)
  {
    getPersistenceServiceFactory(jbpmContextName).cleanSchema();
  }

  public void createSchema()
  {
    createSchema(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void createSchema(String jbpmContextName)
  {
    getPersistenceServiceFactory(jbpmContextName).createSchema();
  }

  public void dropSchema()
  {
    dropSchema(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void dropSchema(String jbpmContextName)
  {
    getPersistenceServiceFactory(jbpmContextName).dropSchema();
  }

  private DbPersistenceServiceFactory getPersistenceServiceFactory(String jbpmContextName)
  {
    return (DbPersistenceServiceFactory)getServiceFactory(Services.SERVICENAME_PERSISTENCE, jbpmContextName);
  }

  public boolean isClosed()
  {
    return isClosed;
  }

  public void close()
  {
    close(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void close(String jbpmContextName)
  {
    // prevent configuration from being closed more than once
    if (isClosed)
      return;

    // stop job executor
    if (jobExecutor != null)
    {
      jobExecutor.stop();
      jobExecutor = null;
    }

    // close service factories
    JbpmContext jbpmContext = createJbpmContext(jbpmContextName);
    try
    {
      Map<String, ServiceFactory> serviceFactories = jbpmContext.getServices().getServiceFactories();
      if (serviceFactories != null)
      {
        for (ServiceFactory serviceFactory : serviceFactories.values())
        {
          serviceFactory.close();
        }
      }
    }
    finally
    {
      jbpmContext.close();
    }

    // closing service factories requires open configuration
    isClosed = true;

    // release context stack
    jbpmContextStacks.remove();

    // remove from configuration cache
    synchronized (instances)
    {
      for (java.util.Iterator<JbpmConfiguration> i = instances.values().iterator(); i.hasNext();)
      {
        if (this == i.next())
        {
          i.remove();
          break;
        }
      }
    }
  }

  static JbpmConfiguration getCurrentJbpmConfiguration()
  {
    JbpmConfiguration currentJbpmConfiguration = null;
    List<JbpmConfiguration> stack = getJbpmConfigurationStack();
    if (!stack.isEmpty())
    {
      currentJbpmConfiguration = stack.get(stack.size() - 1);
    }
    return currentJbpmConfiguration;
  }

  static List<JbpmConfiguration> getJbpmConfigurationStack()
  {
    return jbpmConfigurationStacks.get();
  }

  static void clearJbpmConfigurationStack()
  {
    List<JbpmConfiguration> configStack = getJbpmConfigurationStack();
    if (configStack != null)
    {
      for (JbpmConfiguration config : new ArrayList<JbpmConfiguration>(configStack))
      {
        List<JbpmContext> contextStack = config.getJbpmContextStack();
        if (contextStack != null)
        {
          for (JbpmContext context : new ArrayList<JbpmContext>(contextStack))
          {
            context.close();
          }
        }
        contextStack.clear();
      }
      configStack.clear();
    }
  }

  synchronized void pushJbpmConfiguration()
  {
    getJbpmConfigurationStack().add(this);
  }

  synchronized void popJbpmConfiguration()
  {
    getJbpmConfigurationStack().remove(this);
  }

  public JbpmContext getCurrentJbpmContext()
  {
    ensureOpen();

    JbpmContext currentJbpmContext = null;
    List<JbpmContext> stack = getJbpmContextStack();
    if (!stack.isEmpty())
    {
      currentJbpmContext = stack.get(stack.size() - 1);
    }
    return currentJbpmContext;
  }

  List<JbpmContext> getJbpmContextStack()
  {
    return jbpmContextStacks.get();
  }

  void pushJbpmContext(JbpmContext jbpmContext)
  {
    getJbpmContextStack().add(jbpmContext);
  }

  void popJbpmContext(JbpmContext jbpmContext)
  {
    List<JbpmContext> stack = getJbpmContextStack();
    int size = stack.size();
    if (size == 0)
    {
      log.warn("closed JbpmContext more than once... " + "check your try-finally clauses around JbpmContext blocks");
    }
    else if (jbpmContext != stack.remove(size - 1))
    {
      stack.remove(jbpmContext); // prevent context from remaining in the stack
      log.warn("closed JbpmContext in some order that differs from creation... " + "check your try-finally clauses around JbpmContext blocks");
    }
  }

  void jbpmContextCreated(JbpmContext jbpmContext)
  {
    pushJbpmConfiguration();
    pushJbpmContext(jbpmContext);
  }

  void jbpmContextClosed(JbpmContext jbpmContext)
  {
    popJbpmContext(jbpmContext);
    popJbpmConfiguration();
  }

  public void startJobExecutor()
  {
    getJobExecutor().start();
  }

  public synchronized JobExecutor getJobExecutor()
  {
    ensureOpen();

    if (jobExecutor == null)
    {
      Object object = objectFactory.createObject("jbpm.job.executor");
      if (object instanceof JobExecutor)
      {
        jobExecutor = (JobExecutor)object;
      }
      else if (object != null)
      {
        throw new JbpmException("configuration object named 'jbpm.job.executor' is not a " + JobExecutor.class.getSimpleName());
      }
    }
    return jobExecutor;
  }

  private static Log log = LogFactory.getLog(JbpmConfiguration.class);
}
