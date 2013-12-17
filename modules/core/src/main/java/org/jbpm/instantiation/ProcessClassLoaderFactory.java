package org.jbpm.instantiation;

import org.jbpm.graph.def.ProcessDefinition;

/**
 * Factory to retrieve the Process{@link ClassLoader} which is then used
 * to load delegation classes used by the process.
 * 
 * Default is the build in {@link ProcessClassLoader}, which 
 * tries to load the classes from the jbpm database first.
 * 
 * Can be configured by setting the property <b>'jbpm.processClassLoader'</b>
 * in the jbpm.cfg.xml to the class name of the custom class loader.
 * 
 * @author bernd.ruecker@camunda.com
 */
public interface ProcessClassLoaderFactory {

  public ClassLoader getProcessClassLoader(ProcessDefinition processDefinition);
  
}