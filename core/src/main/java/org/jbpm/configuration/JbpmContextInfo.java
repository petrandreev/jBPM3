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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.jbpm.JbpmContext;
import org.jbpm.svc.Services;
import org.jbpm.util.XmlUtil;

public class JbpmContextInfo extends AbstractObjectInfo {

  private static final long serialVersionUID = 1L;

  private final ObjectInfo[] serviceFactoryInfos;
  private Map serviceFactories;

  private final ObjectInfo[] saveOperationInfos;
  private List saveOperations;

  public JbpmContextInfo(Element jbpmContextElement, ObjectFactoryParser objectFactoryParser) {
    super(verifyDefaultName(jbpmContextElement), objectFactoryParser);
    if (jbpmContextElement.hasAttribute("singleton")) {
      throw new ConfigurationException("attribute singleton is not allowed in jbpm-context");
    }

    // parse the services
    List serviceElements = XmlUtil.elements(jbpmContextElement, "service");
    serviceFactoryInfos = new ObjectInfo[serviceElements.size()];

    for (int i = 0; i < serviceFactoryInfos.length; i++) {
      Element serviceElement = (Element) serviceElements.get(i);

      String serviceName = serviceElement.getAttribute("name");
      if (serviceName.length() == 0) {
        throw new ConfigurationException("service has no name");
      }

      ObjectInfo serviceFactoryInfo;
      Element factoryElement = XmlUtil.element(serviceElement, "factory");
      if (factoryElement != null) {
        Element factoryBeanElement = XmlUtil.element(factoryElement);
        if (factoryBeanElement == null) {
          throw new ConfigurationException("element factory requires either a bean or ref subelement");
        }
        factoryBeanElement.setAttribute("name", serviceName);
        serviceFactoryInfo = objectFactoryParser.parse(factoryBeanElement);
      }
      else if (serviceElement.hasAttribute("factory")) {
        BeanInfo beanInfo = new BeanInfo();
        beanInfo.setName(serviceName);
        beanInfo.setClassName(serviceElement.getAttribute("factory"));
        serviceFactoryInfo = beanInfo;
      }
      else {
        throw new ConfigurationException("element service requires either a factory attribute or a factory subelement");
      }

      serviceFactoryInfos[i] = serviceFactoryInfo;
    }

    // parse the save operations
    Element saveOperationsElement = XmlUtil.element(jbpmContextElement, "save-operations");
    if (saveOperationsElement != null) {
      List saveOperationElements = XmlUtil.elements(saveOperationsElement, "save-operation");
      saveOperationInfos = new ObjectInfo[saveOperationElements.size()];

      for (int i = 0; i < saveOperationInfos.length; i++) {
        Element saveOperationElement = (Element) saveOperationElements.get(i);

        if (saveOperationElement.hasAttribute("class")) {
          String saveOperationClassName = saveOperationElement.getAttribute("class");
          BeanInfo beanInfo = new BeanInfo();
          beanInfo.setClassName(saveOperationClassName);
          saveOperationInfos[i] = beanInfo;
        }
        else {
          Element saveOperationBeanElement = XmlUtil.element(saveOperationElement);
          if (saveOperationBeanElement == null) {
            throw new ConfigurationException("element save-operation requires either a class attribute or a bean or ref subelement");
          }
          saveOperationInfos[i] = objectFactoryParser.parse(saveOperationBeanElement);
        }
      }
    }
    else {
      saveOperationInfos = null;
    }
  }

  private static Element verifyDefaultName(Element jbpmContextElement) {
    if (!jbpmContextElement.hasAttribute("name")) {
      jbpmContextElement.setAttribute("name", JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
    }
    return jbpmContextElement;
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    synchronized (this) {
      if (serviceFactories == null) {
        serviceFactories = new LinkedHashMap(serviceFactoryInfos.length);
        for (int i = 0; i < serviceFactoryInfos.length; i++) {
          ObjectInfo serviceFactoryInfo = serviceFactoryInfos[i];
          Object serviceFactory = serviceFactoryInfo.createObject(objectFactory);
          serviceFactories.put(serviceFactoryInfo.getName(), serviceFactory);
        }

        if (saveOperationInfos != null) {
          saveOperations = new ArrayList(saveOperationInfos.length);
          for (int i = 0; i < saveOperationInfos.length; i++) {
            Object saveOperation = saveOperationInfos[i].createObject(objectFactory);
            saveOperations.add(saveOperation);
          }
        }
      }
    }

    Services services = new Services(serviceFactories, saveOperations);
    return new JbpmContext(services, objectFactory);
  }

}
