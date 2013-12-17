package org.jbpm.mail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

class MailTemplates {

  private static final Map templatePropertiesByResource = new HashMap();
  private static final Map templateVariablesByResource = new HashMap();

  protected static Properties getTemplateProperties(String templateName) {
    String resource = Configs.getString("resource.mail.templates");
    synchronized (templatePropertiesByResource) {
      Map templateProperties = (Map) templatePropertiesByResource.get(resource);
      if (templateProperties == null) {
        loadTemplates(resource);
        templateProperties = (Map) templatePropertiesByResource.get(resource);
      }
      return (Properties) templateProperties.get(templateName);
    }
  }

  protected static Map getTemplateVariables() {
    String resource = Configs.getString("resource.mail.templates");
    synchronized (templateVariablesByResource) {
      Map templateVariables = (Map) templateVariablesByResource.get(resource);
      if (templateVariables == null) {
        loadTemplates(resource);
        templateVariables = (Map) templateVariablesByResource.get(resource);
      }
      return templateVariables;
    }
  }

  protected static void loadTemplates(String resource) {
    Element templatesElement = XmlUtil.parseXmlResource(resource, true).getDocumentElement();

    Map templatePropertiesMap = new HashMap();
    for (Iterator iter = XmlUtil.elementIterator(templatesElement, "mail-template"); iter.hasNext();) {
      Element templateElement = (Element) iter.next();

      Properties templateProperties = new Properties();
      addTemplateProperty(templateElement, "to", templateProperties);
      addTemplateProperty(templateElement, "actors", templateProperties);
      addTemplateProperty(templateElement, "subject", templateProperties);
      addTemplateProperty(templateElement, "text", templateProperties);
      addTemplateProperty(templateElement, "cc", templateProperties);
      addTemplateProperty(templateElement, "cc-actors", templateProperties);
      addTemplateProperty(templateElement, "bcc", templateProperties);
      // preserve backwards compatibility with bccActors element
      Element bccActorsElement = XmlUtil.element(templateElement, "bccActors");
      if (bccActorsElement != null) {
        templateProperties.setProperty("bcc-actors", XmlUtil.getContentText(bccActorsElement));
      }
      else {
        addTemplateProperty(templateElement, "bcc-actors", templateProperties);
      }

      templatePropertiesMap.put(templateElement.getAttribute("name"), templateProperties);
    }
    templatePropertiesByResource.put(resource, templatePropertiesMap);

    Map templateVariables = new HashMap();
    for (Iterator iter = XmlUtil.elementIterator(templatesElement, "variable"); iter.hasNext();) {
      Element variableElement = (Element) iter.next();
      templateVariables.put(variableElement.getAttribute("name"), variableElement.getAttribute("value"));
    }
    templateVariablesByResource.put(resource, templateVariables);
  }

  protected static void addTemplateProperty(Element templateElement, String property,
    Properties templateProperties) {
    Element element = XmlUtil.element(templateElement, property);
    if (element != null) {
      templateProperties.setProperty(property, XmlUtil.getContentText(element));
    }
  }
  
}
