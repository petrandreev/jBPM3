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
package org.jbpm.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.Converter;
import org.jbpm.context.exe.JbpmType;
import org.jbpm.context.exe.JbpmTypeMatcher;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.db.hibernate.Converters;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class JbpmTypeObjectInfo extends AbstractObjectInfo {

  private static final long serialVersionUID = 1L;

  ObjectInfo typeMatcherObjectInfo = null;
  Converter<?, ?> converter = null;
  Class<? extends VariableInstance> variableInstanceClass = null;

  public JbpmTypeObjectInfo(Element jbpmTypeElement, ObjectFactoryParser objectFactoryParser) {
    super(jbpmTypeElement, objectFactoryParser);

    try {
      Element typeMatcherElement = XmlUtil.element(jbpmTypeElement, "matcher");
      if (typeMatcherElement == null) {
        throw new ConfigurationException("matcher element is required in <jbpm-type>: "
            + XmlUtil.toString(jbpmTypeElement));
      }
      Element typeMatcherBeanElement = XmlUtil.element(typeMatcherElement);
      typeMatcherObjectInfo = objectFactoryParser.parse(typeMatcherBeanElement);

      Element converterElement = XmlUtil.element(jbpmTypeElement, "converter");
      if (converterElement != null) {
        if (!converterElement.hasAttribute("class")) {
          throw new ConfigurationException("class attribute is required in <converter>: "
              + XmlUtil.toString(jbpmTypeElement));
        }
        String converterClassName = converterElement.getAttribute("class");
        converter = Converters.getConverterByClassName(converterClassName);
      }

      Element variableInstanceElement = XmlUtil.element(jbpmTypeElement, "variable-instance");
      if (!variableInstanceElement.hasAttribute("class")) {
        throw new ConfigurationException("class attribute is required in <variable-instance>: "
            + XmlUtil.toString(jbpmTypeElement));
      }
      String variableInstanceClassName = variableInstanceElement.getAttribute("class");
      variableInstanceClass = ClassLoaderUtil.classForName(variableInstanceClassName).asSubclass(
          VariableInstance.class);
    }
    catch (ConfigurationException e) {
      throw e;
    }
    catch (RuntimeException e) {
      // Client probably does not need support for this type and omitted the dependency
      // So let us log and ignore
      log.debug("could not instantiate jbpm type '" + XmlUtil.toString(jbpmTypeElement) + "': " + e);
      // make sure this JbpmType is ignored by always returning false in the JbpmTypeMatcher
      typeMatcherObjectInfo = NoObjectInfo.getInstance();
      converter = null;
      variableInstanceClass = null;
    }
  }

  static class NoObjectInfo implements ObjectInfo {

    private static final long serialVersionUID = 1L;

    private static ObjectInfo instance;

    private NoObjectInfo() {
      // hide default constructor to prevent instantiation
    }

    public boolean hasName() {
      return false;
    }

    public String getName() {
      return null;
    }

    public boolean isSingleton() {
      return true;
    }

    public Object createObject(ObjectFactoryImpl objectFactory) {
      return NoTypeMatcher.getInstance();
    }

    static ObjectInfo getInstance() {
      if (instance == null) {
        instance = new NoObjectInfo();
      }
      return instance;
    }
  }

  static class NoTypeMatcher implements JbpmTypeMatcher {

    private static final long serialVersionUID = 1L;

    static final JbpmTypeMatcher instance = new NoTypeMatcher();

    private NoTypeMatcher() {
      // hide default constructor to prevent instantiation
    }

    public boolean matches(Object value) {
      return false;
    }

    static JbpmTypeMatcher getInstance() {
      return instance;
    }
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    JbpmTypeMatcher jbpmTypeMatcher = (JbpmTypeMatcher) objectFactory.createObject(typeMatcherObjectInfo);
    return new JbpmType(jbpmTypeMatcher, converter, variableInstanceClass);
  }

  private static Log log = LogFactory.getLog(JbpmTypeObjectInfo.class);
}
