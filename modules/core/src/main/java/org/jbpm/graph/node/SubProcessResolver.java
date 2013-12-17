package org.jbpm.graph.node;

import java.io.Serializable;

import org.dom4j.Element;
import org.jbpm.graph.def.ProcessDefinition;

public interface SubProcessResolver extends Serializable {

  ProcessDefinition findSubProcess(Element subProcessElement);

}
