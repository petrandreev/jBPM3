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
import org.w3c.dom.Element;

import org.jbpm.context.exe.Converter;
import org.jbpm.context.exe.JbpmType;
import org.jbpm.context.exe.JbpmTypeMatcher;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.db.hibernate.Converters;
import org.jbpm.util.ClassLoaderUtil;
import org.jbpm.util.XmlUtil;

public class JbpmTypeObjectInfo extends AbstractObjectInfo {

  private static final long serialVersionUID = 1L;

  private final ObjectInfo typeMatcherObjectInfo;
  private final Converter converter;
  private final Class variableInstanceClass;

  public JbpmTypeObjectInfo(Element jbpmTypeElement, ObjectFactoryParser objectFactoryParser) {
    super(jbpmTypeElement, objectFactoryParser);

    Element variableInstanceElement = XmlUtil.element(jbpmTypeElement, "variable-instance");
    if (!variableInstanceElement.hasAttribute("class")) {
      throw new ConfigurationException("missing class attribute in variable-instance");
    }

    String variableInstanceClassName = variableInstanceElement.getAttribute("class");
    Class referencedClass;
    try {
      referencedClass = ClassLoaderUtil.classForName(variableInstanceClassName);
    }
    catch (ClassNotFoundException e) {
      // class likely failed to load due to missing dependencies; log and ignore
      if (log.isDebugEnabled()) {
        log.debug("variable instance class not found: " + variableInstanceClassName);
      }
      referencedClass = null;
    }

    if (referencedClass != null) {
      if (!VariableInstance.class.isAssignableFrom(referencedClass)) {
        throw new ConfigurationException(variableInstanceClassName
          + " is not a variable instance");
      }
      variableInstanceClass = referencedClass;

      // type matcher - required
      Element typeMatcherElement = XmlUtil.element(jbpmTypeElement, "matcher");
      if (typeMatcherElement == null) {
        throw new ConfigurationException("missing matcher element in jbpm-type");
      }
      Element typeMatcherBeanElement = XmlUtil.element(typeMatcherElement);
      typeMatcherObjectInfo = objectFactoryParser.parse(typeMatcherBeanElement);

      // converter - optional
      Element converterElement = XmlUtil.element(jbpmTypeElement, "converter");
      if (converterElement != null) {
        if (!converterElement.hasAttribute("class")) {
          throw new ConfigurationException("missing class attribute in converter");
        }
        String converterClassName = converterElement.getAttribute("class");
        converter = Converters.getConverterByClassName(converterClassName);
      }
      else {
        converter = null;
      }
    }
    else {
      // make sure this type is ignored
      variableInstanceClass = null;
      typeMatcherObjectInfo = null;
      converter = null;
    }
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    JbpmTypeMatcher jbpmTypeMatcher = typeMatcherObjectInfo == null ? NoTypeMatcher.INSTANCE
      : (JbpmTypeMatcher) objectFactory.getObject(typeMatcherObjectInfo);
    return new JbpmType(jbpmTypeMatcher, converter, variableInstanceClass);
  }

  private static class NoTypeMatcher implements JbpmTypeMatcher {

    static final JbpmTypeMatcher INSTANCE = new NoTypeMatcher();
    private static final long serialVersionUID = 1L;

    public boolean matches(Object value) {
      return false;
    }
  }

  private static final Log log = LogFactory.getLog(JbpmTypeObjectInfo.class);
}
