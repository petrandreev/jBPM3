package org.jbpm.command;

import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * This Command returns all process definitions (or only the latest if onlyLatest is true)
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class GetProcessDefinitionsCommand extends AbstractGetObjectBaseCommand {

  private static final long serialVersionUID = -1908847549444051495L;

  private boolean onlyLatest = true;

  public GetProcessDefinitionsCommand() {
  }

  public GetProcessDefinitionsCommand(boolean onlyLatest) {
    this.onlyLatest = onlyLatest;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    setJbpmContext(jbpmContext);
    List result = (onlyLatest ? jbpmContext.getGraphSession().findLatestProcessDefinitions()
        : jbpmContext.getGraphSession().findAllProcessDefinitions());

    /*
     * traverse and access property if it is missing in the default fetchgroup
     */
    Iterator iter = result.iterator();
    while (iter.hasNext()) {
      ProcessDefinition pd = (ProcessDefinition) iter.next();
      retrieveProcessDefinition(pd);
    }

    return result;
  }

  public boolean isOnlyLatest() {
    return onlyLatest;
  }

  public void setOnlyLatest(boolean onlyLatest) {
    this.onlyLatest = onlyLatest;
  }

  public String getAdditionalToStringInformation() {
    return "onlyLatest=" + onlyLatest;
  }

  // methods for fluent programming

  public GetProcessDefinitionsCommand onlyLatest(boolean onlyLatest) {
    setOnlyLatest(onlyLatest);
    return this;
  }
}
