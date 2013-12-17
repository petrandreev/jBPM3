package org.jbpm.graph.node;

import java.io.Serializable;

import org.dom4j.Element;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * An agent capable of resolving sub-process definitions given the information items in the
 * <code>sub-process</code> element taken from a process definition document.
 */
public interface SubProcessResolver extends Serializable {

  /**
   * Resolves a sub-process definition given the information items in the
   * <code>sub-process</code> element.
   */
  ProcessDefinition findSubProcess(Element subProcessElement);

}
