package org.jbpm.graph.action;

import org.dom4j.Element;
import org.jbpm.graph.def.Action;
import org.jbpm.jpdl.xml.JpdlXmlReader;

public class MailAction extends Action {

  private static final long serialVersionUID = 1L;

  public void read(Element element, JpdlXmlReader jpdlReader) {
    String template = element.attributeValue("template");
    String actors = element.attributeValue("actors");
    String to = element.attributeValue("to");
    String subject = jpdlReader.getProperty("subject", element);
    String text = jpdlReader.getProperty("text", element);
    actionDelegation = jpdlReader.createMailDelegation(template, actors, to, subject, text);
  }
}
