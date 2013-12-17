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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import org.jbpm.JbpmException;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.jpdl.xml.Parsable;

public class Delegation implements Parsable, Serializable {

  private static final long serialVersionUID = 1L;

  protected final static Map instantiatorCache = createInstantiatorCache();

  private static Map createInstantiatorCache() {
    Map instantiators = new HashMap();
    instantiators.put(null, new FieldInstantiator());
    instantiators.put("field", new FieldInstantiator());
    instantiators.put("bean", new BeanInstantiator());
    instantiators.put("constructor", new ConstructorInstantiator());
    instantiators.put("configuration-property", new ConfigurationPropertyInstantiator());
    instantiators.put("xml", new XmlInstantiator());
    return instantiators;
  }

  private long id;
  protected String className;
  protected String configuration;
  protected String configType;
  protected ProcessDefinition processDefinition;
  private transient Object instance;

  public Delegation() {
  }

  public Delegation(Object instance) {
    this.instance = instance;
  }

  public Delegation(String className) {
    this.className = className;
  }

  public void read(Element delegateElement, JpdlXmlReader jpdlReader) {
    processDefinition = jpdlReader.getProcessDefinition();
    className = delegateElement.attributeValue("class");
    if (className == null) {
      jpdlReader.addWarning("no class specified in delegation: " + delegateElement.getPath());
    }

    configType = delegateElement.attributeValue("config-type");
    if (delegateElement.hasContent()) {
      configuration = jpdlReader.writeElementContent(delegateElement);
    }
  }

  public void write(Element element) {
    element.addAttribute("class", className);
    element.addAttribute("config-type", configType);
    String configuration = this.configuration;
    if (configuration != null) {
      try {
        Element actionElement = DocumentHelper.parseText("<action>" + configuration
          + "</action>").getRootElement();
        element.appendContent(actionElement);
      }
      catch (DocumentException e) {
        log.error("could not parse configuration: " + configuration, e);
      }
    }
  }

  public Object getInstance() {
    if (instance == null) {
      instance = instantiate();
    }
    return instance;
  }

  public Object instantiate() {
    /*
     * The context class loader for the current thread should already be a
     * ProcessClassLoader that this delegation can use directly. There is no
     * need to introduce an extra process class loader.
     */
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader instanceof ProcessClassLoader : classLoader.getClass();

    // find the instantiator
    Instantiator instantiator = (Instantiator) instantiatorCache.get(configType);
    if (instantiator == null) {
      try {
        // load the instantiator class
        Class instantiatorClass = Class.forName(configType, false, classLoader);
        try {
          // instantiate the instantiator with the default constructor
          instantiator = (Instantiator) instantiatorClass.newInstance();
          instantiatorCache.put(configType, instantiator);
        }
        catch (InstantiationException e) {
          throw new JbpmException("failed to instantiate " + instantiatorClass, e);
        }
        catch (IllegalAccessException e) {
          throw new JbpmException(getClass() + " has no access to " + instantiatorClass, e);
        }
      }
      catch (ClassNotFoundException e) {
        throw new JbpmException("could not load instantiator class " + configType, e);
      }
    }

    try {
      // load the class that needs to be instantiated
      Class delegationClass = Class.forName(className, false, classLoader);
      // instantiate the object
      return instantiator.instantiate(delegationClass, configuration);
    }
    catch (ClassNotFoundException e) {
      throw new DelegationException("could not load delegation class " + className, e);
    }
  }

  // equals ///////////////////////////////////////////////////////////////////

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Delegation)) return false;

    Delegation other = (Delegation) o;
    if (id != 0 && id == other.getId()) return true;

    return className.equals(other.getClassName())
      && (configuration != null ? configuration.equals(other.getConfiguration())
        : other.getConfiguration() == null);
  }

  public int hashCode() {
    int result = 2131399759 + className.hashCode();
    result = 702058657 * result + configuration != null ? configuration.hashCode() : 0;
    return result;
  }

  public String toString() {
    return "Delegation(" + className + ')';
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  public String getConfigType() {
    return configType;
  }

  public void setConfigType(String instantiatorType) {
    this.configType = instantiatorType;
  }

  public long getId() {
    return id;
  }

  /**
   * This method has no effect.
   * 
   * @deprecated database identifier is not meant to be mutable
   */
  public void setId(long id) {
  }

  public ProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    this.processDefinition = processDefinition;
  }

  private static final Log log = LogFactory.getLog(Delegation.class);
}
