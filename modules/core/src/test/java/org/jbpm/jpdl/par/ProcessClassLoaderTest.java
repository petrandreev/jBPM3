package org.jbpm.jpdl.par;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.ConfigurableClassloadersTest;
import org.jbpm.instantiation.ProcessClassLoader;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Test case for ProcessClassLoader hierarchy and setting the ContextClassLoader correctly.
 * 
 * Relates to {@link ConfigurableClassloadersTest}.
 * 
 * @author Tom Baeyens, bernd.ruecker@camunda.com
 *
 */
public class ProcessClassLoaderTest extends AbstractJbpmTestCase
{
  public static class TestContextClassLoader extends ClassLoader
  {
    public TestContextClassLoader(ClassLoader parent)
    {
      super(parent);
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
      //System.out.println("loadClass: " + name + ",resolve=" + resolve);
      return super.loadClass(name, resolve);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
      //System.out.println("loadClass: " + name);
      return super.loadClass(name);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
      //System.out.println("findClass: " + name);
      return super.findClass(name);
    }
  }

  static int contextLoadedActionInvocations = 0;
  static ClassLoader originalClassLoader = null;

  protected void setUp() throws Exception
  {
    super.setUp();
    contextLoadedActionInvocations = 0;
    originalClassLoader = Thread.currentThread().getContextClassLoader();
  }

  public static class DefaultLoadedAction implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      assertSame(ProcessClassLoader.class, contextClassLoader.getClass());

      // verify that the default uses the jbpm-lib-classloader
      assertSame(ClassLoaderUtil.class.getClassLoader(), contextClassLoader.getParent());

      contextLoadedActionInvocations++;
    }
  }

  /*
   *  DOES NOT configure usage of the context classloader. So this tests the default (backwards compatible) behaviour. so the classloading hierarchy of
   * DefaultLoadedAction should be ProcessClassloader -> jbpm-lib classloader
   */
  public void testDefaultClassLoader()
  {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" 
        + "  <start-state name='start'>" 
        + "    <transition to='state'>"
        + "      <action class='" + DefaultLoadedAction.class.getName() + "' />" 
        + "    </transition>" 
        + "  </start-state>"
        + "  <state name='state'>" 
        + "    <transition to='end'/>" 
        + "  </state>" 
        + "</process-definition>");

    // create the process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    assertEquals(1, contextLoadedActionInvocations);
  }

  public static class ContextLoadedAction implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
      ClassLoader processClassLoader = Thread.currentThread().getContextClassLoader();
      assertSame(ProcessClassLoader.class, processClassLoader.getClass());

      ClassLoader testContextClassLoader = processClassLoader.getParent();
      assertSame(TestContextClassLoader.class, testContextClassLoader.getClass());

      assertSame(originalClassLoader, testContextClassLoader.getParent());

      contextLoadedActionInvocations++;
    }
  }

  /*
   *  configures usage of the context classloader so the classloading hierarchy of ContextLoadedAction should be ProcessClassloader -> TestContextClassLoader ->
   * Thread.currentContextClassLoader
   */
  public void testContextClassLoader()
  {

    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>" 
        + "  <string name='jbpm.classLoader' value='context' />"
        + "</jbpm-configuration>");

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try
    {
      TestContextClassLoader testContextClassLoader = new TestContextClassLoader(originalClassLoader);
      Thread.currentThread().setContextClassLoader(testContextClassLoader);

      ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" 
          + "  <start-state name='start'>"
          + "    <transition to='state'>" 
          + "      <action class='" + ContextLoadedAction.class.getName() + "' />" 
          + "    </transition>"
          + "  </start-state>" 
          + "  <state name='state'>" 
          + "    <transition to='end'/>" 
          + "  </state>" 
          + "</process-definition>");

      // create the process instance
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      assertEquals(1, contextLoadedActionInvocations);
      assertSame(testContextClassLoader, Thread.currentThread().getContextClassLoader());

    }
    finally
    {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
      jbpmContext.close();
    }
  }

  /*
   * a third test should set the testcontextClassLoader in the test and then let the action throw an exception. Then it should be verified that the original classloader
   * is still restored correctly. Easiest is to start from a copy of the testContextClassLoader
   */
  public static class ContextLoadedExceptionAction implements ActionHandler
  {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
      ClassLoader processClassLoader = Thread.currentThread().getContextClassLoader();
      assertSame(ProcessClassLoader.class, processClassLoader.getClass());

      ClassLoader testContextClassLoader = processClassLoader.getParent();
      assertSame(TestContextClassLoader.class, testContextClassLoader.getClass());

      assertSame(originalClassLoader, testContextClassLoader.getParent());

      contextLoadedActionInvocations++;

      throw new Exception("simulate exception");
    }
  }

  public void testContextClassLoaderException()
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>" 
        + "  <string name='jbpm.classLoader' value='context' />"
        + "</jbpm-configuration>");

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    TestContextClassLoader testContextClassLoader = new TestContextClassLoader(originalClassLoader);
    try
    {
      Thread.currentThread().setContextClassLoader(testContextClassLoader);

      ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition>" 
          + "  <start-state name='start'>"
          + "    <transition to='state'>" 
          + "      <action class='" + ContextLoadedExceptionAction.class.getName() + "' />"
          + "    </transition>" 
          + "  </start-state>" 
          + "  <state name='state'>" 
          + "    <transition to='end'/>" 
          + "  </state>" 
          + "</process-definition>");

      // create the process instance
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

    }
    catch (Exception ex)
    {
      assertEquals(1, contextLoadedActionInvocations);
      assertEquals("simulate exception", ex.getMessage());
      assertSame(testContextClassLoader, Thread.currentThread().getContextClassLoader());

    }
    finally
    {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
      jbpmContext.close();
    }
  }
}
