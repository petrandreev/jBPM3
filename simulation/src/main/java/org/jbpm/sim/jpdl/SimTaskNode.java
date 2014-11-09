package org.jbpm.sim.jpdl;

import org.dom4j.Element;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.jpdl.xml.JpdlXmlReader;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class SimTaskNode extends TaskNode {

  private static final long serialVersionUID = 1L;

  public void read(Element element, JpdlXmlReader jpdlReader) {
    super.read(element, jpdlReader);
  }

}
