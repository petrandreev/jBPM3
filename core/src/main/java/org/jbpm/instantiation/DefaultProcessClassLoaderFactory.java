package org.jbpm.instantiation;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Default implementation of {@link ProcessClassLoaderFactory}.
 * 
 * @author bernd.ruecker@camunda.com
 */
public class DefaultProcessClassLoaderFactory implements ProcessClassLoaderFactory {

  private static final long serialVersionUID = 1L;

  public ClassLoader getProcessClassLoader(ProcessDefinition processDefinition) {
    // default behavior like before https://jira.jboss.org/jira/browse/JBPM-1148    
    return new ProcessClassLoader(ClassLoaderUtil.getClassLoader(), processDefinition);
  }
}
