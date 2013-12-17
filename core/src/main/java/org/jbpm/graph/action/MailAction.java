package org.jbpm.graph.action;

import org.dom4j.Element;
import org.jbpm.graph.def.Action;
import org.jbpm.jpdl.xml.JpdlXmlReader;

public class MailAction extends Action {

  private static final long serialVersionUID = 1L;

  public void read(Element element, JpdlXmlReader jpdlReader) {
    actionDelegation = jpdlReader.readMailDelegation(element);
  }

  public String toString() {
    return "MailAction(" + actionDelegation.getConfiguration() + ')';
  }
}
