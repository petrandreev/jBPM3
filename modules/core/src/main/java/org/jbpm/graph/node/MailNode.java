package org.jbpm.graph.node;

import org.dom4j.Element;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;
import org.jbpm.jpdl.xml.JpdlXmlReader;

public class MailNode extends Node {

  private static final long serialVersionUID = 1L;

  // xml //////////////////////////////////////////////////////////////////////

  public void read(Element element, JpdlXmlReader jpdlReader) {
    String template = element.attributeValue("template");
    String actors = element.attributeValue("actors");
    String to = element.attributeValue("to");
    String subject = jpdlReader.getProperty("subject", element);
    String text = jpdlReader.getProperty("text", element);
    Delegation delegation = jpdlReader.createMailDelegation(template, actors, to, subject, text);
    this.action = new Action(delegation);
  }

  public void execute(ExecutionContext executionContext) {
    try {
      executeAction(action, executionContext);
    } catch (JbpmException e) {
      throw e;
    } catch (Exception e) {
      throw new JbpmException("couldn't send email", e);
    }
    leave(executionContext);
  }
  
  
}
