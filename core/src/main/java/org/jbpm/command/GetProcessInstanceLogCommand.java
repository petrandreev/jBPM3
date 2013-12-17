package org.jbpm.command;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.log.ProcessLog;

/**
 * Retrieve the <code>org.jbpm.logging.log.ProcessLog</code> for the process with the given
 * process-id returns a map that maps {@link Token}s to {@link List}s.
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class GetProcessInstanceLogCommand extends AbstractBaseCommand {

  private static final long serialVersionUID = -2812852941518870502L;

  private long processInstanceId;

  public GetProcessInstanceLogCommand() {
  }

  public GetProcessInstanceLogCommand(long processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    Map logMap = jbpmContext.getLoggingSession().findLogsByProcessInstance(processInstanceId);
    return loadLogdFromMap(logMap);
  }

  /**
   * access everything on all ProcessLog objects, which is not in the default fetch group from
   * hibernate, but needs to be accesible from the client overwrite this method, if you need more
   * details in your client
   */
  protected Map loadLogdFromMap(Map logMap) {
    Iterator iter = logMap.keySet().iterator();
    while (iter.hasNext()) {
      Token t = (Token) iter.next();

      List logs = (List) logMap.get(t);
      Iterator iter2 = logs.iterator();
      while (iter2.hasNext()) {
        ProcessLog pl = (ProcessLog) iter2.next();
        // TODO: I am not sure if we need that, write a test for it
        pl.getActorId();
        pl.toString();
      }
    }
    return logMap;
  }

  /**
   * @deprecated use getProcessInstanceId instead
   */
  public long getProcessId() {
    return processInstanceId;
  }

  /**
   * @deprecated use setProcessInstanceId instead
   */
  public void setProcessId(long processId) {
    this.processInstanceId = processId;
  }

  public long getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(long processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getAdditionalToStringInformation() {
    return "processInstanceId=" + processInstanceId;
  }

  // methods for fluent programming

  public GetProcessInstanceLogCommand processInstanceId(long processInstanceId) {
    setProcessInstanceId(processInstanceId);
    return this;
  }
}
