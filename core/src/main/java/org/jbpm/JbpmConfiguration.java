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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.configuration.ObjectFactory;
import org.jbpm.configuration.ObjectFactoryImpl;
import org.jbpm.configuration.ObjectFactoryParser;
import org.jbpm.configuration.ValueInfo;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.instantiation.ProcessClassLoaderFactory;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.util.ClassLoaderUtil;

/**
 * configuration of one jBPM instance.
 * <p>
 * During process execution, jBPM might need to use some services. A JbpmConfiguration contains
 * the knowledge on how to create those services.
 * </p>
 * <p>
 * A JbpmConfiguration is a thread safe object and serves as a factory for
 * {@link org.jbpm.JbpmContext}s, which means one JbpmConfiguration can be used to create
 * {@link org.jbpm.JbpmContext}s for all threads. The single JbpmConfiguration can be maintained
 * in a static member or in the JNDI tree if that is available.
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
 * or
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
 *   &quot;&lt;/jbpm-configuration&gt;&quot;);
 * </pre>
 * 
 * </li>
 * <li>By specifying a custom implementation of an object factory. This can be used to specify a
 * JbpmConfiguration in other bean-style notations such as used by JBoss Microcontainer or
 * Spring.
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
 */
public class JbpmConfiguration implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_RESOURCE = "jbpm.cfg.xml";
  static final String OBJECT_NAME = "jbpm.configuration";

  private static ObjectFactory defaultObjectFactory;
  private static final Map instances = new HashMap();
  private static final ThreadLocal threadLocalConfigurationStack = new ThreadLocal();

  private final ObjectFactory objectFactory;
  private final String resourceName;
  private transient final ThreadLocal threadLocalContextStack = new ThreadLocal();
  private JobExecutor jobExecutor;
  private volatile boolean isClosed;

  public JbpmConfiguration(ObjectFactory objectFactory) {
    this(objectFactory, null);
  }

  private JbpmConfiguration(ObjectFactory objectFactory, String resourceName) {
    if (objectFactory == null) throw new IllegalArgumentException("object factory is null");

    this.objectFactory = objectFactory;
    this.resourceName = resourceName;
  }

  ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  String getResourceName() {
    return resourceName;
  }

  public static void setDefaultObjectFactory(ObjectFactory objectFactory) {
    defaultObjectFactory = objectFactory;
  }

  public static JbpmConfiguration getInstance() {
    return getInstance(null);
  }

  public static JbpmConfiguration getInstance(String resource) {
    if (resource == null) resource = DEFAULT_RESOURCE;

    JbpmConfiguration instance;
    synchronized (instances) {
      // look for configuration in cache
      instance = (JbpmConfiguration) instances.get(resource);
      if (instance == null) {
        // configuration does not exist or was evicted, construct it
        if (defaultObjectFactory != null) {
          log.info("configuring from default object factory");
          instance = new JbpmConfiguration(defaultObjectFactory);
        }
        else {
          log.info("configuring from resource: " + resource);
          InputStream jbpmCfgXmlStream = ClassLoaderUtil.getStream(resource, false);
          /*
           * if a custom resource is specified, but not found in the classpath, log a warning;
           * otherwise, users who want to load custom stuff will not receive any feedback when
           * their resource cannot be found
           */
          if (jbpmCfgXmlStream == null && !DEFAULT_RESOURCE.equals(resource)) {
            log.warn("configuration resource not found: " + resource);
          }
          ObjectFactory objectFactory = parseObjectFactory(jbpmCfgXmlStream);
          instance = createJbpmConfiguration(objectFactory, resource);
        }
        // put configuration in cache
        instances.put(resource, instance);
      }
    }
    return instance;
  }

  public static boolean hasInstance(String resource) {
    return instances.containsKey(resource != null ? resource : DEFAULT_RESOURCE);
  }

  protected static ObjectFactory parseObjectFactory(InputStream inputStream) {
    ObjectFactoryParser objectFactoryParser = new ObjectFactoryParser();
    ObjectFactoryImpl objectFactory = new ObjectFactoryImpl();
    objectFactoryParser.parseElementsFromResource("org/jbpm/default.jbpm.cfg.xml", objectFactory);

    if (inputStream != null) {
      objectFactoryParser.parseElementsStream(inputStream, objectFactory);
    }
    return objectFactory;
  }

  private static ObjectFactory loadDefaultObjectFactory() {
    log.info("loading default configuration");
    return ObjectFactoryParser.parseResource("org/jbpm/default.jbpm.cfg.xml");
  }

  public static JbpmConfiguration parseXmlString(String xml) {
    ObjectFactory objectFactory;
    if (xml != null) {
      log.info("configuring from xml string");
      InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
      objectFactory = parseObjectFactory(inputStream);
    }
    else {
      objectFactory = loadDefaultObjectFactory();
    }
    return createJbpmConfiguration(objectFactory);
  }

  protected static JbpmConfiguration createJbpmConfiguration(ObjectFactory objectFactory) {
    return createJbpmConfiguration(objectFactory, null);
  }

  private static JbpmConfiguration createJbpmConfiguration(ObjectFactory objectFactory,
    String resourceName) {
    JbpmConfiguration jbpmConfiguration = new JbpmConfiguration(objectFactory, resourceName);
    // make the configuration available to other objects
    if (objectFactory instanceof ObjectFactoryImpl) {
      ObjectFactoryImpl objectFactoryImpl = (ObjectFactoryImpl) objectFactory;
      objectFactoryImpl.addObjectInfo(new ValueInfo("jbpmConfiguration", jbpmConfiguration));
      objectFactoryImpl.addObjectInfo(new ValueInfo(OBJECT_NAME, jbpmConfiguration));
    }
    return jbpmConfiguration;
  }

  public static JbpmConfiguration parseInputStream(InputStream inputStream) {
    ObjectFactory objectFactory;
    if (inputStream != null) {
      log.info("configuring from " + inputStream);
      objectFactory = parseObjectFactory(inputStream);
    }
    else {
      objectFactory = loadDefaultObjectFactory();
    }
    return createJbpmConfiguration(objectFactory);
  }

  public static JbpmConfiguration parseResource(String resource) {
    ObjectFactory objectFactory;
    if (resource != null) {
      log.info("configuring from resource: " + resource);
      InputStream inputStream = ClassLoaderUtil.getStream(resource, false);
      if (inputStream == null) {
        throw new IllegalArgumentException("resource not found: " + resource);
      }
      objectFactory = parseObjectFactory(inputStream);
      try {
        inputStream.close();
      }
      catch (IOException e) {
        log.warn("failed to close resource: " + resource, e);
      }
    }
    else {
      objectFactory = loadDefaultObjectFactory();
    }
    return createJbpmConfiguration(objectFactory, resource);
  }

  public JbpmContext createJbpmContext() {
    return createJbpmContext(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public JbpmContext createJbpmContext(String name) {
    ensureOpen();

    JbpmContext jbpmContext = (JbpmContext) objectFactory.createObject(name);
    pushJbpmContext(jbpmContext);
    return jbpmContext;
  }

  public ServiceFactory getServiceFactory(String serviceName) {
    return getServiceFactory(serviceName, JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public ServiceFactory getServiceFactory(String serviceName, String jbpmContextName) {
    JbpmContext jbpmContext = createJbpmContext(jbpmContextName);
    try {
      return jbpmContext.getServices().getServiceFactory(serviceName);
    }
    finally {
      jbpmContext.close();
    }
  }

  private DbPersistenceServiceFactory getPersistenceServiceFactory(String jbpmContextName) {
    return (DbPersistenceServiceFactory) getServiceFactory(Services.SERVICENAME_PERSISTENCE, jbpmContextName);
  }

  public static ClassLoader getProcessClassLoader(ProcessDefinition processDefinition) {
    ProcessClassLoaderFactory factory = (ProcessClassLoaderFactory) Configs.getObject("process.class.loader.factory");
    return factory.getProcessClassLoader(processDefinition);
  }

  /**
   * access to configuration information through the current {@link JbpmContext}
   */
  public static class Configs {

    private Configs() {
      // hide default constructor to prevent instantiation
    }

    public static ObjectFactory getObjectFactory() {
      JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
      return jbpmContext != null ? jbpmContext.getObjectFactory()
        : getInstance().getObjectFactory();
    }

    /**
     * @deprecated call {@link JbpmConfiguration#setDefaultObjectFactory(ObjectFactory)} instead
     */
    public static void setDefaultObjectFactory(ObjectFactory objectFactory) {
      JbpmConfiguration.setDefaultObjectFactory(objectFactory);
    }

    public static boolean hasObject(String name) {
      ObjectFactory objectFactory = getObjectFactory();
      return objectFactory.hasObject(name);
    }

    public static synchronized Object getObject(String name) {
      ObjectFactory objectFactory = getObjectFactory();
      return objectFactory.createObject(name);
    }

    public static String getString(String name) {
      return (String) getObject(name);
    }

    public static long getLong(String name) {
      return ((Long) getObject(name)).longValue();
    }

    public static int getInt(String name) {
      return ((Integer) getObject(name)).intValue();
    }

    public static boolean getBoolean(String name) {
      return ((Boolean) getObject(name)).booleanValue();
    }
  }

  public void cleanSchema() {
    cleanSchema(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void cleanSchema(String jbpmContextName) {
    getPersistenceServiceFactory(jbpmContextName).cleanSchema();
  }

  public void createSchema() {
    createSchema(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void createSchema(String jbpmContextName) {
    getPersistenceServiceFactory(jbpmContextName).createSchema();
  }

  public void dropSchema() {
    dropSchema(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void dropSchema(String jbpmContextName) {
    getPersistenceServiceFactory(jbpmContextName).dropSchema();
  }

  private void ensureOpen() {
    if (isClosed) throw new JbpmException(this + " is closed");
  }

  public boolean isClosed() {
    return isClosed;
  }

  public void close() {
    close(JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
  }

  public void close(String jbpmContextName) {
    // prevent configuration from being closed more than once
    if (isClosed) return;

    synchronized (this) {
      // stop job executor
      if (jobExecutor != null) {
        try {
          jobExecutor.stopAndJoin();
        }
        catch (InterruptedException e) {
          // reassert interruption and continue
          Thread.currentThread().interrupt();
        }
        jobExecutor = null;
      }

      // close remaining contexts
      List contextStack = (List) threadLocalContextStack.get();
      if (contextStack != null && !contextStack.isEmpty()) {
        log.warn("closing "
          + contextStack.size()
          + " open contexts;"
          + " make sure to close JbpmContext after use");

        // copy to array because JbpmContext.close() pops the context off the stack
        JbpmContext[] jbpmContexts = (JbpmContext[]) contextStack.toArray(new JbpmContext[contextStack.size()]);
        for (int i = 0; i < jbpmContexts.length; i++) {
          jbpmContexts[i].close();
        }
      }

      // close service factories
      JbpmContext jbpmContext = createJbpmContext(jbpmContextName);
      try {
        Map serviceFactories = jbpmContext.getServices().getServiceFactories();
        if (serviceFactories != null) {
          for (Iterator i = serviceFactories.values().iterator(); i.hasNext();) {
            ServiceFactory serviceFactory = (ServiceFactory) i.next();
            serviceFactory.close();
          }
        }
      }
      finally {
        jbpmContext.close();
      }

      // release thread-local context stack
      threadLocalContextStack.set(null);
    }

    // closing service factories requires open configuration
    isClosed = true;

    // remove from configuration cache
    if (resourceName != null) {
      synchronized (instances) {
        instances.remove(resourceName);
      }
    }
  }

  static JbpmConfiguration getCurrentJbpmConfiguration() {
    List stack = (List) threadLocalConfigurationStack.get();
    return (stack == null || stack.isEmpty()) ? null
      : (JbpmConfiguration) stack.get(stack.size() - 1);
  }

  static void clearInstances() {
    synchronized (instances) {
      instances.clear();
    }
  }

  public JbpmContext getCurrentJbpmContext() {
    ensureOpen();

    List stack = (List) threadLocalContextStack.get();
    return (stack == null || stack.isEmpty()) ? null
      : (JbpmContext) stack.get(stack.size() - 1);
  }

  void pushJbpmContext(JbpmContext jbpmContext) {
    // first push the configuration
    List configStack = (List) threadLocalConfigurationStack.get();
    if (configStack == null) {
      configStack = new ArrayList();
      threadLocalConfigurationStack.set(configStack);
    }
    configStack.add(this);
    // then push the context
    List contextStack = (List) threadLocalContextStack.get();
    if (contextStack == null) {
      contextStack = new ArrayList();
      threadLocalContextStack.set(contextStack);
    }
    contextStack.add(jbpmContext);
  }

  private static void remove(ThreadLocal threadLocal) {
    try {
      // ThreadLocal.remove does not exist in JDK 1.4.2
      // invoke via reflection if available
      Method removeMethod = ThreadLocal.class.getMethod("remove", null);
      try {
        removeMethod.invoke(threadLocal, null);
      }
      catch (IllegalAccessException e) {
        // method should be public, otherwise Class.getMethod would not return it
        throw new JbpmException(JbpmConfiguration.class + " has no access to " + removeMethod);
      }
      catch (InvocationTargetException e) {
        // if remove method threw an exception, rethrow to client
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        throw new JbpmException(removeMethod + " threw exception", cause);
      }
    }
    catch (NoSuchMethodException e) {
      // method unavailable; just set thread local to null
      // this will still leak the ThreadLocal itself but not the value
      threadLocal.set(null);
    }
  }

  void popJbpmContext(JbpmContext jbpmContext) {
    boolean threadSafetyFlag = false;
    boolean creationOrderFlag = false;

    // first pop the context
    List contextStack = (List) threadLocalContextStack.get();
    int contextIndex;
    if (contextStack == null || (contextIndex = contextStack.lastIndexOf(jbpmContext)) == -1) {
      threadSafetyFlag = true;
    }
    else {
      if (contextIndex != contextStack.size() - 1) {
        creationOrderFlag = true;
      }
      // prevent context from remaining in the stack, no matter what
      contextStack.remove(contextIndex);
      // if context stack gets empty, remove thread-local variable
      if (contextStack.isEmpty()) remove(threadLocalContextStack);
    }

    // then pop the configuration
    List configStack = (List) threadLocalConfigurationStack.get();
    int configIndex;
    if (configStack == null || (configIndex = configStack.lastIndexOf(this)) == -1) {
      threadSafetyFlag = true;
    }
    else {
      if (configIndex != configStack.size() - 1) {
        creationOrderFlag = true;
      }
      // prevent configuration from remaining in the stack, no matter what
      configStack.remove(configIndex);
      // if configuration stack gets empty, remove thread-local variable
      if (configStack.isEmpty()) remove(threadLocalConfigurationStack);
    }

    if (threadSafetyFlag) {
      log.warn(jbpmContext
        + " was not closed in the thread that created it;"
        + " JbpmContext is not safe for access from multiple threads!");
    }
    else if (creationOrderFlag) {
      log.warn(jbpmContext
        + " was not closed in a block-structured manner;"
        + " check try-finally clauses around JbpmContext blocks");
    }
  }

  public void startJobExecutor() {
    getJobExecutor().start();
  }

  public JobExecutor getJobExecutor() {
    ensureOpen();

    synchronized (this) {
      if (jobExecutor == null) {
        jobExecutor = (JobExecutor) objectFactory.createObject("jbpm.job.executor");
      }
    }
    return jobExecutor;
  }

  public String toString() {
    return "JbpmConfiguration"
      + (resourceName != null ? '(' + resourceName + ')'
        : '@' + Integer.toHexString(hashCode()));
  }

  private static final Log log = LogFactory.getLog(JbpmConfiguration.class);
}
