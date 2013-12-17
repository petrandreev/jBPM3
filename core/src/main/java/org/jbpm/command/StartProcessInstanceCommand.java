package org.jbpm.command;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Graph command to start a new process and signal it immidiatly. The transition named in
 * <code>startTransitionName</code> is used (or the default transition if it is null). The result of
 * this command, if requested, is a {@link Long} value containing the process instance id.
 * 
 * @author Jim Rigsbee, Tom Baeyens, Bernd Ruecker
 */
public class StartProcessInstanceCommand extends NewProcessInstanceCommand {

  private static final long serialVersionUID = -2428234069404269048L;

  /**
   * this transition name is used for signalling (if null, the default transition is used)
   */
  private String startTransitionName = null;

  public Object execute(JbpmContext jbpmContext) throws Exception {
    Object object = super.execute(jbpmContext);
    if (object instanceof ProcessInstance) {
      ProcessInstance processInstance = (ProcessInstance) object;
      if (startTransitionName == null || startTransitionName.length() == 0)
        processInstance.signal();
      else
        processInstance.signal(startTransitionName);
    }
    return object;
  }

  public String getStartTransitionName() {
    return startTransitionName;
  }

  public void setStartTransitionName(String startTransitionName) {
    this.startTransitionName = startTransitionName;
  }

  public String getAdditionalToStringInformation() {
    return super.getAdditionalToStringInformation() + ";startTransitionName=" + startTransitionName;
  }

  // methods for fluent programming

  public StartProcessInstanceCommand startTransitionName(String startTransitionName) {
    setStartTransitionName(startTransitionName);
    return this;
  }

}
