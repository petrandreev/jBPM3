package org.jbpm.instantiation;

import java.io.Serializable;

import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * <p>
 * Factory to retrieve the Process {@link ClassLoader} which is then used to load delegation classes
 * used by the process.
 * </p>
 * <p>
 * Default is the build in {@link ProcessClassLoader}, which tries to load the classes from the jBPM
 * database first.
 * </p>
 * <p>
 * Can be configured by setting the property <code>jbpm.process.class.loader</code> in the
 * configuration file to the class name of the custom class loader.
 * </p>
 * <p>
 * Implementations should be serializable, as the {@link JbpmConfiguration} that references them is.
 * </p>
 * 
 * @author bernd.ruecker@camunda.com
 */
public interface ProcessClassLoaderFactory extends Serializable {

  public ClassLoader getProcessClassLoader(ProcessDefinition processDefinition);

}