package org.jbpm.jpdl.par;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Element;

public class FileFilter implements ConfigurableParser {

  private static final long serialVersionUID = 1L;
  
  List files = new ArrayList();

  public void configure(Element parserElement) {
    Iterator iter = XmlUtil.elementIterator(parserElement, "file");
    while(iter.hasNext()) {
      Element fileElement = (Element) iter.next();
      files.add(fileElement.getAttribute("name"));
    }
  }

  public ProcessDefinition readFromArchive(ProcessArchive processArchive, ProcessDefinition processDefinition) {
    processArchive.removeEntry("processdefinition.xml");
    return null;
  }
}
