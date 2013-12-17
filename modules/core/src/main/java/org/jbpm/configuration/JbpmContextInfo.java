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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.svc.Services;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class JbpmContextInfo extends AbstractObjectInfo {

  private static final long serialVersionUID = 1L;
  
  Map serviceFactoryObjectInfos = null;
  Map serviceFactories = null;
  List serviceNames = null;
  
  ObjectInfo[] saveOperationObjectInfos = null;
  List saveOperations = null;
  
  public JbpmContextInfo(Element jbpmContextElement, ObjectFactoryParser objectFactoryParser) {
    super(verifyDefaultName(jbpmContextElement), objectFactoryParser);
    if (jbpmContextElement.hasAttribute("singleton")) {
      throw new ConfigurationException("attribute 'singleton' is not allowed in element 'jbpm-context'");
    }
    
    // parse the services
    serviceFactoryObjectInfos = new HashMap();
    List serviceElements = XmlUtil.elements(jbpmContextElement, "service");
    serviceNames = new ArrayList();
    Iterator iter = serviceElements.iterator();
    while (iter.hasNext()) {
      Element serviceElement = (Element) iter.next();
      if (! serviceElement.hasAttribute("name")) {
        throw new ConfigurationException("name is required in service element "+XmlUtil.toString(serviceElement));
      }
      String serviceName = serviceElement.getAttribute("name");
      serviceNames.add(serviceName);
      ObjectInfo serviceFactoryObjectInfo = null;
      Element factoryElement = XmlUtil.element(serviceElement, "factory");
      if (factoryElement!=null) {
        Element factoryBeanElement = XmlUtil.element(factoryElement);
        if (factoryBeanElement==null) {
          throw new ConfigurationException("element 'factory' requires either a bean or ref element");
        }
        serviceFactoryObjectInfo = objectFactoryParser.parse(factoryBeanElement);
        
        if (serviceElement.hasAttribute("factory")) {
          log.warn("duplicate factory specification for service "+serviceName+", using the factory element");
        }
      } else if (serviceElement.hasAttribute("factory")) {
        String factoryClassName = serviceElement.getAttribute("factory");
        BeanInfo beanInfo = new BeanInfo();
        beanInfo.setClassName(factoryClassName);
        serviceFactoryObjectInfo = beanInfo;
      } else {
        throw new ConfigurationException("element 'service' requires either a factory attribute or a factory element");
      }
      
      serviceFactoryObjectInfos.put(serviceName, serviceFactoryObjectInfo);
    }
    
    // parse the save operations
    Element saveOperationsElement = XmlUtil.element(jbpmContextElement, "save-operations");
    if (saveOperationsElement!=null) {
      List saveOperationElements = XmlUtil.elements(saveOperationsElement, "save-operation");
      saveOperationObjectInfos = new ObjectInfo[saveOperationElements.size()];
      for (int i=0; i<saveOperationElements.size(); i++){
        Element saveOperationElement = (Element) saveOperationElements.get(i);
        
        if (saveOperationElement.hasAttribute("class")) {
          String saveOperationClassName = saveOperationElement.getAttribute("class");
          BeanInfo beanInfo = new BeanInfo();
          beanInfo.setClassName(saveOperationClassName);
          saveOperationObjectInfos[i] = beanInfo;
        } else {
          Element saveOperationBeanElement = XmlUtil.element(saveOperationElement);
          if (saveOperationBeanElement==null) {
            throw new ConfigurationException("element 'save-operation' requires either a class attribute or an element of type 'bean' or 'ref'");
          }
          saveOperationObjectInfos[i] = objectFactoryParser.parse(saveOperationBeanElement);
        }
      }
    }
  }

  static Element verifyDefaultName(Element jbpmContextElement) {
    if (!jbpmContextElement.hasAttribute("name")) {
      jbpmContextElement.setAttribute("name", JbpmContext.DEFAULT_JBPM_CONTEXT_NAME);
    }
    return jbpmContextElement;
  }

  public synchronized Object createObject(ObjectFactoryImpl objectFactory) {
    if (serviceFactories==null) {
      serviceFactories = new HashMap();
      Iterator iter = serviceFactoryObjectInfos.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        String serviceName = (String) entry.getKey();
        ObjectInfo serviceFactoryObjectInfo = (ObjectInfo) entry.getValue();
        serviceFactories.put(serviceName, serviceFactoryObjectInfo.createObject(objectFactory));
      }
    }

    if ( (saveOperations==null)
         && (saveOperationObjectInfos!=null)
       ) {
      saveOperations = new ArrayList();
      for (int i=0; i<saveOperationObjectInfos.length; i++) {
        Object saveOperation = saveOperationObjectInfos[i].createObject(objectFactory);
        saveOperations.add(saveOperation);
      }
    }

    Services services = new Services(serviceFactories, serviceNames, saveOperations);
    
    if (log.isDebugEnabled()) log.debug("creating jbpm context with service factories '"+serviceFactories.keySet()+"'");
    return new JbpmContext(services, objectFactory);
  }
  
  private static Log log = LogFactory.getLog(JbpmContextInfo.class);
}
