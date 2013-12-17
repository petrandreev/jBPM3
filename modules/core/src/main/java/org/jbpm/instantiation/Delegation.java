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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.jpdl.xml.Parsable;
import org.jbpm.util.EqualsUtil;

public class Delegation implements Parsable, Serializable {

  private static final long serialVersionUID = 1L;

  protected static Map<String, Instantiator> instantiatorCache = createInstantiatorCache();

  private static Map<String, Instantiator> createInstantiatorCache() {
    Map<String, Instantiator> instantiators = new HashMap<String, Instantiator>();
    instantiators.put(null, new FieldInstantiator());
    instantiators.put("field", new FieldInstantiator());
    instantiators.put("bean", new BeanInstantiator());
    instantiators.put("constructor", new ConstructorInstantiator());
    instantiators.put("configuration-property", new ConfigurationPropertyInstantiator());
    instantiators.put("xml", new XmlInstantiator());
    return instantiators;
  }

  long id = 0;
  protected String className = null;
  protected String configuration = null;
  protected String configType = null;
  protected ProcessDefinition processDefinition = null;
  transient Object instance = null;

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
      jpdlReader.addWarning("no class specified in " + delegateElement.asXML());
    }

    configType = delegateElement.attributeValue("config-type");
    if (delegateElement.hasContent()) {
      try {
        StringWriter stringWriter = new StringWriter();
        // when parsing, it could be to store the config in the database, so we want to make the
        // configuration compact
        XMLWriter xmlWriter = new XMLWriter(stringWriter, OutputFormat.createCompactFormat());
        for (Object node : delegateElement.content()) {
          xmlWriter.write(node);
        }
        xmlWriter.flush();
        configuration = stringWriter.toString();
      }
      catch (IOException e) {
        jpdlReader.addWarning("io problem while parsing the configuration of "
            + delegateElement.asXML());
      }
    }
  }

  public void write(Element element) {
    element.addAttribute("class", className);
    element.addAttribute("config-type", configType);
    String configuration = this.configuration;
    if (configuration != null) {
      try {
        Element actionElement = DocumentHelper.parseText("<action>" + configuration + "</action>")
            .getRootElement();
        element.appendContent(actionElement);
      }
      catch (DocumentException e) {
        log.error("couldn't create dom-tree for action configuration '" + configuration + "'", e);
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
    // The thread class loader was set before the instantiation correctly
    // to the ProcesClassLoader which can be directly used here
    // If we would construct a JbpmConfiguration.getProcessClassLoder here
    // we would have the hierarchy ProcessClassLoader -> ProcessClassLoader -> Context...
    // this is one ProcessClassLoader too much
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // load the class that needs to be instantiated
    Class<?> delegationClass = null;
    try {
      delegationClass = Class.forName(className, false, classLoader);
    }
    catch (ClassNotFoundException e) {
      throw new DelegationException("could not load delegation class '" + className + "'", e);
    }

    // find the instantiator
    Instantiator instantiator = instantiatorCache.get(configType);
    if (instantiator == null) {
      Class<?> instantiatorClass = null;
      try {
        // load the instantiator class
        instantiatorClass = Class.forName(configType, false, classLoader);
        // create the instantiator with the default constructor
        instantiator = (Instantiator) instantiatorClass.newInstance();
        instantiatorCache.put(configType, instantiator);
      }
      catch (ClassNotFoundException e) {
        throw new JbpmException("could not load instantiator class '" + configType + "'", e);
      }
      catch (InstantiationException e) {
        throw new JbpmException("could not instantiate " + instantiatorClass, e);
      }
      catch (IllegalAccessException e) {
        throw new JbpmException("could not access " + instantiatorClass, e);
      }
    }

    // instantiate the object
    return instantiator.instantiate(delegationClass, configuration);
  }

  // equals ///////////////////////////////////////////////////////////////////
  // hack to support comparing hibernate proxies against the real objects
  // since this always falls back to ==, we don't need to overwrite the hashcode
  public boolean equals(Object o) {
    return EqualsUtil.equals(this, o);
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

  public void setId(long id) {
    this.id = id;
  }

  public ProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  public void setProcessDefinition(ProcessDefinition processDefinition) {
    this.processDefinition = processDefinition;
  }

  private static final Log log = LogFactory.getLog(Delegation.class);
}
