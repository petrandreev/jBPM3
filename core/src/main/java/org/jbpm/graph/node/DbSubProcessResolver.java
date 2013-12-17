package org.jbpm.graph.node;

import org.dom4j.Element;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;

public class DbSubProcessResolver implements SubProcessResolver {

  private static final long serialVersionUID = 1L;

  public ProcessDefinition findSubProcess(Element subProcessElement) {
    // if subprocess resolution is done within an active context,
    // there is a database connection to look up the subprocess.
    // otherwise, the subprocess will be left null and
    // it is up to client code to set the subprocess as appropriate.
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext != null) {
      // within an active context it is possible to find the sub-process
      String subProcessName = subProcessElement.attributeValue("name");
      if (subProcessName == null) {
        throw new JpdlException("missing sub-process name");
      }

      // if only the name is specified,
      String subProcessVersion = subProcessElement.attributeValue("version");
      if (subProcessVersion == null) {
        // select the latest version of the subprocess definition
        return jbpmContext.getGraphSession().findLatestProcessDefinition(subProcessName);
      }

      // if the name and the version are specified
      try {
        // select the exact version of the subprocess definition
        int version = Integer.parseInt(subProcessVersion);
        return jbpmContext.getGraphSession().findProcessDefinition(subProcessName, version);
      }
      catch (NumberFormatException e) {
        throw new JpdlException("bad sub-process version value: " + subProcessVersion);
      }
    }

    return null;
  }
}
