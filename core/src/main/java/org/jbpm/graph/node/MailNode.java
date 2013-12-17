package org.jbpm.graph.node;

import org.dom4j.Element;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;
import org.jbpm.jpdl.xml.JpdlXmlReader;

public class MailNode extends Node {

  private static final long serialVersionUID = 1L;

  // xml //////////////////////////////////////////////////////////////////////

  public void read(Element element, JpdlXmlReader jpdlReader) {
    Delegation delegation = jpdlReader.readMailDelegation(element);
    this.action = new Action(delegation);
  }

  public void execute(ExecutionContext executionContext) {
    // execute mail action
    executeAction(action, executionContext);
    // leave the node over the default transition
    leave(executionContext);
  }
}
