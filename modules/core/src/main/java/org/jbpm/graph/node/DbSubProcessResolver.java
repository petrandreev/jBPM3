package org.jbpm.graph.node;

import org.dom4j.Element;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;

public class DbSubProcessResolver implements SubProcessResolver {

  private static final long serialVersionUID = 1L;

  public ProcessDefinition findSubProcess(Element subProcessElement) {
    ProcessDefinition subProcessDefinition = null;

    String subProcessName = subProcessElement.attributeValue("name");
    String subProcessVersion = subProcessElement.attributeValue("version");

    // if this parsing is done in the context of a process deployment, there is
    // a database connection to look up the subprocess.
    // when there is no jbpmSession, the definition will be left null... the
    // testcase can set it as appropriate.
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext != null) {
      
      // now, we must be able to find the sub-process
      if (subProcessName != null) {
        
        // if the name and the version are specified
        if (subProcessVersion != null) {
          
          try {
            int version = Integer.parseInt(subProcessVersion);
            // select that exact process definition as the subprocess definition
            subProcessDefinition = jbpmContext.getGraphSession().findProcessDefinition(subProcessName, version);

          } catch (NumberFormatException e) {
            throw new JpdlException("version in process-state was not a number: " + subProcessElement.asXML());
          }
          
        } else { // if only the name is specified
          // select the latest version of that process as the subprocess
          // definition
          subProcessDefinition = jbpmContext.getGraphSession().findLatestProcessDefinition(subProcessName);
        }
      } else {
        throw new JpdlException("no sub-process name specfied in process-state: " + subProcessElement.asXML());
      }
    }

    return subProcessDefinition;
  }
}
